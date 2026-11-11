package ro.cpa.advisor.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ro.cpa.advisor.domain.SessionToken;
import ro.cpa.advisor.service.UsageService;

/**
 * Contractul REST cap-coada (securitate + controllere + servicii + H2 cu migrarile reale). Limitele de rata sunt ridicate
 * aici ca sa nu intervina; ele au testul lor (`RateLimitAndBudgetTest`).
 */
@SpringBootTest(
        properties = {
            "spring.ai.openai.api-key=",
            "cpa.limits.requests-per-minute-per-session=100000",
            "cpa.limits.requests-per-minute-per-ip=100000",
            "cpa.limits.sessions-per-hour-per-ip=100000"
        })
@AutoConfigureMockMvc
class ApiContractTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    UsageService usage;

    // ---------- sesiune si autentificare ----------

    @Test
    void protectedEndpointsRequireAValidBearerToken() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.code").value("unauthorized"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/profile").header(HttpHeaders.AUTHORIZATION, "Bearer cpa_" + "x".repeat(43)))
                .andExpect(status().isUnauthorized());
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/profile").header(HttpHeaders.AUTHORIZATION, "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sessionTokenIsIssuedOnceAndStoredOnlyAsHash() throws Exception {
        String body = mvc.perform(MockMvcRequestBuilders.post("/api/v1/sessions"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.token", matchesPattern("^cpa_[A-Za-z0-9_-]{43}$")))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");

        Integer rawStored = jdbc.queryForObject("select count(*) from visitor_session where token_hash = ?", Integer.class, token);
        Integer hashStored = jdbc.queryForObject(
                "select count(*) from visitor_session where token_hash = ?", Integer.class, ro.cpa.advisor.domain.SessionToken.hash(token));
        assertThat(rawStored).isZero();
        assertThat(hashStored).isEqualTo(1);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/sessions/current").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void deletingMyDataRemovesEverythingAndInvalidatesTheToken() throws Exception {
        String token = newSession();
        String conversationId = createConversation(token, "Salut");
        post(token, "/api/v1/conversations/" + conversationId + "/messages", "{\"content\":\"buna\"}").andExpect(status().isCreated());
        post(token, "/api/v1/shortlist", "{\"variantId\":\"golf-1-5-tsi-130\"}").andExpect(status().isCreated());
        put(token, "/api/v1/profile", "{\"passengers\":4}").andExpect(status().isOk());

        mvc.perform(delete("/api/v1/sessions/current").header(HttpHeaders.AUTHORIZATION, bearer(token))).andExpect(status().isNoContent());

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/profile").header(HttpHeaders.AUTHORIZATION, bearer(token))).andExpect(status().isUnauthorized());
        Integer leftovers = jdbc.queryForObject(
                "select count(*) from conversation where id = ?", Integer.class, UUID.fromString(conversationId));
        assertThat(leftovers).isZero();
    }

    // ---------- profil ----------

    @Test
    void profileStartsEmptyIsReplacedAndNormalized() throws Exception {
        String token = newSession();
        get(token, "/api/v1/profile")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetAmount").doesNotExist())
                .andExpect(jsonPath("$.mustHaves", hasSize(0)))
                .andExpect(jsonPath("$.updatedAt").doesNotExist());

        String body = """
                {"budgetAmount": 20000, "budgetKind": "TOTAL", "financingMode": "CASH", "yearlyKm": 15000,
                 "cityPercent": 80, "passengers": 4, "bodyTypes": ["HATCHBACK", "HATCHBACK", "SUV"],
                 "powertrains": ["HYBRID"], "homeCharging": false, "condition": "ANY",
                 "mustHaves": ["  ISOFIX ", "isofix\\u200b", "ISOFIX", ""], "dealBreakers": ["cutie   automata"]}
                """;
        put(token, "/api/v1/profile", body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetAmount").value(20000))
                .andExpect(jsonPath("$.bodyTypes", hasSize(2)))
                .andExpect(jsonPath("$.mustHaves", hasSize(2)))
                .andExpect(jsonPath("$.mustHaves[0]").value("ISOFIX"))
                .andExpect(jsonPath("$.dealBreakers[0]").value("cutie automata"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        // PUT inlocuieste tot: ce lipseste dispare.
        put(token, "/api/v1/profile", "{\"passengers\":2}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetAmount").doesNotExist())
                .andExpect(jsonPath("$.passengers").value(2));

        mvc.perform(delete("/api/v1/profile").header(HttpHeaders.AUTHORIZATION, bearer(token))).andExpect(status().isNoContent());
        get(token, "/api/v1/profile").andExpect(jsonPath("$.passengers").doesNotExist());
    }

    @Test
    void invalidProfilesAreRejectedAsProblemDetails() throws Exception {
        String token = newSession();
        put(token, "/api/v1/profile", "{\"passengers\": 0}")
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/problem+json"))
                .andExpect(jsonPath("$.code").value("validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("passengers"));
        put(token, "/api/v1/profile", "{\"budgetAmount\": 5000}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid-input"));
        put(token, "/api/v1/profile", "{\"bodyTypes\": [\"SPACESHIP\"]}").andExpect(status().isBadRequest());
        put(token, "/api/v1/profile", "{not json").andExpect(status().isBadRequest());
    }

    // ---------- conversatii si mesaje ----------

    @Test
    void conversationLifecycleWithMessages() throws Exception {
        String token = newSession();
        String id = createConversation(token, null);

        post(token, "/api/v1/conversations/" + id + "/messages", "{\"content\":\"  Caut o masina\\n pentru oras  \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.content").value("Caut o masina\n pentru oras"))
                .andExpect(jsonPath("$.conversationId").value(id));
        post(token, "/api/v1/conversations/" + id + "/messages", "{\"content\":\"al doilea\"}").andExpect(status().isCreated());

        get(token, "/api/v1/conversations/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Caut o masina pentru oras"));
        get(token, "/api/v1/conversations/" + id + "/messages")
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].content").value("Caut o masina\n pentru oras"))
                .andExpect(jsonPath("$[1].content").value("al doilea"));

        patch(token, "/api/v1/conversations/" + id, "{\"title\":\"Masina de oras\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Masina de oras"));
        patch(token, "/api/v1/conversations/" + id, "{\"title\":\"  \"}").andExpect(status().isBadRequest());
        get(token, "/api/v1/conversations").andExpect(jsonPath("$[*].id", hasItem(id)));

        mvc.perform(delete("/api/v1/conversations/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token))).andExpect(status().isNoContent());
        get(token, "/api/v1/conversations/" + id).andExpect(status().isNotFound());
        get(token, "/api/v1/conversations/" + id + "/messages").andExpect(status().isNotFound());
    }

    @Test
    void messagesAreValidated() throws Exception {
        String token = newSession();
        String id = createConversation(token, "t");
        String path = "/api/v1/conversations/" + id + "/messages";
        post(token, path, "{\"content\":\"   \"}").andExpect(status().isBadRequest());
        post(token, path, "{\"content\":\"\\u200b\\u200b\"}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("invalid-input"));
        post(token, path, "{\"content\":\"" + "a".repeat(4001) + "\"}").andExpect(status().isBadRequest());
        post(token, path, "{}").andExpect(status().isBadRequest());
        post(token, "/api/v1/conversations/" + UUID.randomUUID() + "/messages", "{\"content\":\"x\"}").andExpect(status().isNotFound());
    }

    @Test
    void sessionsCannotSeeEachOthersData() throws Exception {
        String alice = newSession();
        String bob = newSession();
        String aliceConversation = createConversation(alice, "privat");
        String shortlistItem = JsonPath.read(
                post(alice, "/api/v1/shortlist", "{\"variantId\":\"yaris-1-5-hybrid-116\"}").andReturn().getResponse().getContentAsString(), "$.id");

        // Id-ul altcuiva arata exact ca unul inexistent: 404, nu 403.
        get(bob, "/api/v1/conversations/" + aliceConversation).andExpect(status().isNotFound());
        get(bob, "/api/v1/conversations/" + aliceConversation + "/messages").andExpect(status().isNotFound());
        post(bob, "/api/v1/conversations/" + aliceConversation + "/messages", "{\"content\":\"intrus\"}").andExpect(status().isNotFound());
        patch(bob, "/api/v1/conversations/" + aliceConversation, "{\"title\":\"furat\"}").andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/conversations/" + aliceConversation).header(HttpHeaders.AUTHORIZATION, bearer(bob)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/shortlist/" + shortlistItem).header(HttpHeaders.AUTHORIZATION, bearer(bob))).andExpect(status().isNotFound());
        get(bob, "/api/v1/conversations").andExpect(jsonPath("$", hasSize(0)));
        get(bob, "/api/v1/shortlist").andExpect(jsonPath("$", hasSize(0)));
        get(alice, "/api/v1/conversations/" + aliceConversation).andExpect(status().isOk());
    }

    // ---------- lista scurta ----------

    @Test
    void shortlistAddListRemoveAndConflicts() throws Exception {
        String token = newSession();
        post(token, "/api/v1/shortlist", "{\"variantId\":\"corolla-1-8-hybrid-140\",\"note\":\" prima   alegere \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.note").value("prima alegere"))
                .andExpect(jsonPath("$.vehicle.make").value("Toyota"))
                .andExpect(jsonPath("$.vehicle.variants", hasSize(1)))
                .andExpect(jsonPath("$.vehicle.variants[0].id").value("corolla-1-8-hybrid-140"));

        post(token, "/api/v1/shortlist", "{\"variantId\":\"corolla-1-8-hybrid-140\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("already-in-shortlist"));
        post(token, "/api/v1/shortlist", "{\"variantId\":\"does-not-exist\"}").andExpect(status().isNotFound());
        post(token, "/api/v1/shortlist", "{\"variantId\":\"\"}").andExpect(status().isBadRequest());

        String listBody = get(token, "/api/v1/shortlist").andExpect(jsonPath("$", hasSize(1))).andReturn().getResponse().getContentAsString();
        String itemId = JsonPath.read(listBody, "$[0].id");
        mvc.perform(delete("/api/v1/shortlist/" + itemId).header(HttpHeaders.AUTHORIZATION, bearer(token))).andExpect(status().isNoContent());
        get(token, "/api/v1/shortlist").andExpect(jsonPath("$", hasSize(0)));
    }

    // ---------- catalog ----------

    @Test
    void vehicleCatalogueSearchAndLookup() throws Exception {
        String token = newSession();
        get(token, "/api/v1/vehicles")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataAsOf").value("2026-11-11"))
                .andExpect(jsonPath("$.vehicles", hasSize(14)))
                .andExpect(jsonPath("$.vehicles[0].make").value("Dacia"));

        get(token, "/api/v1/vehicles?powertrain=ELECTRIC&maxPriceEur=40000")
                .andExpect(jsonPath("$.vehicles[*].variants[*].powertrain", not(hasItem("PETROL"))))
                .andExpect(jsonPath("$.vehicles[*].variants[*].priceEur", not(hasItem(41900))));
        get(token, "/api/v1/vehicles?bodyType=ESTATE").andExpect(jsonPath("$.vehicles", hasSize(1)));
        get(token, "/api/v1/vehicles?minSeats=9").andExpect(jsonPath("$.vehicles", hasSize(0)));
        get(token, "/api/v1/vehicles?bodyType=SPACESHIP")
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/problem+json"))
                .andExpect(jsonPath("$.code").value("bad-request"));
        get(token, "/api/v1/vehicles?maxPriceEur=-5").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("invalid-input"));

        get(token, "/api/v1/vehicles/vw-golf")
                .andExpect(jsonPath("$.variants", hasSize(3)))
                .andExpect(jsonPath("$.variants[0].priceEur").value(29900));
        get(token, "/api/v1/vehicles/nope").andExpect(status().isNotFound());
    }

    // ---------- consum ----------

    @Test
    void usageReflectsRecordedTokensAgainstTheDailyBudget() throws Exception {
        String token = newSession();
        get(token, "/api/v1/usage")
                .andExpect(jsonPath("$.inputTokens").value(0))
                .andExpect(jsonPath("$.dailyTokenBudget").value(60000))
                .andExpect(jsonPath("$.remainingTokens").value(60000));

        UUID sessionId = jdbc.queryForObject(
                "select id from visitor_session where token_hash = ?", UUID.class, ro.cpa.advisor.domain.SessionToken.hash(token));
        usage.record(sessionId, "test-model", 1000, 500);

        get(token, "/api/v1/usage")
                .andExpect(jsonPath("$.inputTokens").value(1000))
                .andExpect(jsonPath("$.outputTokens").value(500))
                .andExpect(jsonPath("$.remainingTokens").value(58500));
    }

    // ---------- ajutoare ----------

    private String newSession() throws Exception {
        String body = mvc.perform(MockMvcRequestBuilders.post("/api/v1/sessions")).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String createConversation(String token, String title) throws Exception {
        String json = title == null ? null : "{\"title\":\"" + title + "\"}";
        String body = (json == null
                        ? mvc.perform(MockMvcRequestBuilders.post("/api/v1/conversations").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                        : post(token, "/api/v1/conversations", json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private ResultActions get(String token, String path) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(path).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private ResultActions post(String token, String path, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(path).header(HttpHeaders.AUTHORIZATION, bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions put(String token, String path, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.put(path).header(HttpHeaders.AUTHORIZATION, bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions patch(String token, String path, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.patch(path).header(HttpHeaders.AUTHORIZATION, bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
