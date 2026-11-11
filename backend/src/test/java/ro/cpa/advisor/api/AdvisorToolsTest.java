package ro.cpa.advisor.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ro.cpa.advisor.api.SseTestSupport.parse;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ro.cpa.advisor.ai.RecordedAdvisorConfig;
import ro.cpa.advisor.ai.RecordedChatModel;
import ro.cpa.advisor.ai.RecordedChatModel.Reply;
import ro.cpa.advisor.api.SseTestSupport.Sse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Uneltele consilierului cap-coada: modelul inregistrat „cere” unelte exact cum ar face OpenAI, bucla `ToolCallingAdvisor`
 * le executa pe baza H2 cu catalogul real, iar UI-ul primeste `tool`, `vehicles` si `profile`. Baza proprie.
 */
@SpringBootTest(
        properties = {
            RecordedAdvisorConfig.TEST_KEY,
            "spring.datasource.url=jdbc:h2:mem:tools;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "cpa.limits.requests-per-minute-per-session=100000",
            "cpa.limits.requests-per-minute-per-ip=100000",
            "cpa.limits.sessions-per-hour-per-ip=100000",
            "cpa.llm.max-tool-calls=6"
        })
@AutoConfigureMockMvc
@Import(RecordedAdvisorConfig.class)
class AdvisorToolsTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    RecordedChatModel model;

    @BeforeEach
    void resetModel() {
        model.reset();
    }

    @Test
    void searchingShowsCardsAndFeedsTheCatalogueDataBackToTheModel() throws Exception {
        model.enqueue(
                Reply.calling("searchVehicles", "{\"bodyType\":\"HATCHBACK\",\"maxPriceEur\":30000,\"limit\":3}"),
                Reply.of("Iata ", "cateva ", "optiuni.").withUsage(400, 25));
        String token = newSession();

        List<Sse> events = parse(send(token, newConversation(token), "Caut un hatchback sub 30000 EUR").andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        assertThat(events).extracting(Sse::name).containsExactly("meta", "tool", "vehicles", "tool", "token", "token", "token", "done");
        assertThat(JsonPath.<String>read(events.get(1).data(), "$.name")).isEqualTo("searchVehicles");
        assertThat(JsonPath.<String>read(events.get(1).data(), "$.status")).isEqualTo("started");
        assertThat(JsonPath.<String>read(events.get(3).data(), "$.status")).isEqualTo("finished");

        String cards = events.get(2).data();
        assertThat(JsonPath.<Integer>read(cards, "$.cards.length()")).isEqualTo(3);
        assertThat(JsonPath.<List<String>>read(cards, "$.cards[*].bodyType")).containsOnly("HATCHBACK");
        assertThat(JsonPath.<List<Integer>>read(cards, "$.cards[*].priceEur")).allMatch(price -> price <= 30000).isSorted();
        assertThat(JsonPath.<String>read(cards, "$.dataAsOf")).isEqualTo("2026-11-11");

        // Modelul a primit rezultatul uneltei (cu data cifrelor), nu a trebuit sa-l „stie” din memorie.
        assertThat(toolResults(model.lastPrompt().getInstructions())).singleElement().satisfies(result -> {
            assertThat(result).contains("2026-11-11").contains("totalMatches").contains("indicative");
            assertThat(result).contains(JsonPath.<String>read(cards, "$.cards[0].variantId"));
        });

        // Consumul este totalul ambelor apeluri ale modelului (ToolCallingAdvisor il aduna): 100 + 400 de intrare, 10 + 25 de iesire.
        assertThat(JsonPath.<Integer>read(events.get(7).data(), "$.usage.input")).isEqualTo(500);
        assertThat(JsonPath.<Integer>read(events.get(7).data(), "$.usage.output")).isEqualTo(35);
    }

    @Test
    void theProfileIsUpdatedWithCleanedValuesAndAnInvalidUpdateIsExplainedToTheModel() throws Exception {
        model.enqueue(
                Reply.calling(
                        "updateBuyerProfile",
                        "{\"budgetAmount\":20000,\"budgetKind\":\"TOTAL\",\"yearlyKm\":15000,\"mustHaves\":[\"  ISOFIX \",\"ISOFIX\\u200b\",\"\"],\"homeCharging\":false}"),
                Reply.calling("updateBuyerProfile", "{\"passengers\":42}"),
                Reply.of("Am notat."));
        String token = newSession();

        List<Sse> events = parse(send(token, newConversation(token), "Am 20000 de euro si 15000 km pe an").andReturn().getResponse().getContentAsString());

        assertThat(events).extracting(Sse::name).containsExactly("meta", "tool", "profile", "tool", "tool", "tool", "token", "done");
        // Valorile din eveniment sunt cele salvate (curatate, fara dubluri), nu textul brut al modelului.
        String patch = events.get(2).data();
        assertThat(JsonPath.<Integer>read(patch, "$.patch.yearlyKm")).isEqualTo(15000);
        assertThat(JsonPath.<List<String>>read(patch, "$.patch.mustHaves")).containsExactly("ISOFIX");
        assertThat(JsonPath.<Boolean>read(patch, "$.patch.homeCharging")).isFalse();

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/profile").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(jsonPath("$.budgetAmount").value(20000))
                .andExpect(jsonPath("$.budgetKind").value("TOTAL"))
                .andExpect(jsonPath("$.passengers").doesNotExist());

        // A doua cerere (passengers = 42) e refuzata de regulile profilului: modelul primeste eroarea, profilul ramane intact.
        List<String> results = toolResults(model.lastPrompt().getInstructions());
        assertThat(results).hasSize(2);
        assertThat(results.get(1)).contains("passengers must be between 1 and 9");
    }

    @Test
    void shortlistToolsActOnTheCallersShortlistOnly() throws Exception {
        model.enqueue(
                Reply.calling("addToShortlist", "{\"variantId\":\"corolla-1-8-hybrid-140\",\"note\":\"prima alegere\"}"),
                Reply.calling("addToShortlist", "{\"variantId\":\"nu-exista\"}"),
                Reply.calling("getShortlist", "{}"),
                Reply.of("Gata."));
        String alice = newSession();
        String bob = newSession();

        List<Sse> events = parse(send(alice, newConversation(alice), "Salveaza Corolla").andReturn().getResponse().getContentAsString());

        assertThat(events).extracting(Sse::name).contains("vehicles");
        Sse vehicles = events.stream().filter(e -> e.name().equals("vehicles")).findFirst().orElseThrow();
        assertThat(JsonPath.<List<String>>read(vehicles.data(), "$.cards[*].variantId")).containsExactly("corolla-1-8-hybrid-140");
        assertThat(toolResults(model.lastPrompt().getInstructions()).get(1)).contains("Vehicle variant not found");

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/shortlist").header(HttpHeaders.AUTHORIZATION, "Bearer " + alice))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].note").value("prima alegere"));
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/shortlist").header(HttpHeaders.AUTHORIZATION, "Bearer " + bob)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void comparingNeedsTwoKnownVariantsAndShowsAScoredComparison() throws Exception {
        model.enqueue(
                Reply.calling("compareVehicles", "{\"variantIds\":[\"golf-1-5-tsi-130\"]}"),
                Reply.calling("compareVehicles", "{\"variantIds\":[\"golf-1-5-tsi-130\",\"corolla-1-8-hybrid-140\",\"nu-exista\"]}"),
                Reply.of("Comparatie."));
        String token = newSession();

        List<Sse> events = parse(send(token, newConversation(token), "Compara Golf cu Corolla").andReturn().getResponse().getContentAsString());

        List<String> results = toolResults(model.lastPrompt().getInstructions());
        assertThat(results.get(0)).contains("between 2 and 4");
        assertThat(results.get(1)).contains("Unknown ids (not compared): [nu-exista]");
        Sse compare = events.stream().filter(e -> e.name().equals("compare")).findFirst().orElseThrow();
        assertThat(JsonPath.<List<String>>read(compare.data(), "$.vehicles[*].variantId")).containsExactlyInAnyOrder("golf-1-5-tsi-130", "corolla-1-8-hybrid-140");
        assertThat(JsonPath.<List<String>>read(compare.data(), "$.criteria[*].id")).containsExactly("price_fit", "running_cost", "space", "safety", "reliability", "usage_fit");
        assertThat(JsonPath.<Integer>read(compare.data(), "$.scores.length()")).isEqualTo(2);
        assertThat(JsonPath.<Integer>read(compare.data(), "$.scores[0].breakdown.length()")).isEqualTo(6);
        assertThat(JsonPath.<String>read(compare.data(), "$.dataAsOf")).isEqualTo("2026-11-11");
        assertThat(JsonPath.<List<String>>read(compare.data(), "$.assumptions[*]")).anyMatch(a -> a.contains("15000 km/year"));
        // Modelul primeste aceleasi cifre ca widgetul: scorul total al primului loc apare in rezultatul uneltei.
        assertThat(results.get(1)).contains("\"total\":" + JsonPath.<Integer>read(compare.data(), "$.scores[0].total"));
    }

    @Test
    void rankingUsesTheProfileTheAgentJustSaved() throws Exception {
        model.enqueue(
                Reply.calling(
                        "updateBuyerProfile",
                        "{\"budgetAmount\":30000,\"budgetKind\":\"TOTAL\",\"yearlyKm\":15000,\"cityPercent\":80,\"passengers\":5,\"homeCharging\":false}"),
                Reply.calling("rankVehicles", "{\"limit\":3}"),
                Reply.of("Iata clasamentul."));
        String token = newSession();

        List<Sse> events = parse(send(token, newConversation(token), "Am 30000 EUR, 15000 km pe an, in oras, fara incarcare acasa, 5 persoane").andReturn().getResponse().getContentAsString());

        Sse compare = events.stream().filter(e -> e.name().equals("compare")).findFirst().orElseThrow();
        List<Integer> totals = JsonPath.read(compare.data(), "$.scores[*].total");
        assertThat(totals).hasSize(3).isSortedAccordingTo(java.util.Comparator.reverseOrder());
        assertThat(JsonPath.<List<String>>read(compare.data(), "$.vehicles[*].variantId")).isEqualTo(JsonPath.<List<String>>read(compare.data(), "$.scores[*].variantId"));
        // Profilul salvat intra in scor: bugetul si numarul de locuri contau, deci niciun criteriu nu e „neaplicabil” pentru preturi.
        assertThat(JsonPath.<Boolean>read(compare.data(), "$.scores[0].breakdown[0].applicable")).isTrue();
        assertThat(JsonPath.<String>read(compare.data(), "$.scores[0].breakdown[0].criterion")).isEqualTo("price_fit");
        // Un clasament de 3 din 21 de variante, cele mai bune primele; fara sa pice nimic din catalog.
        assertThat(toolResults(model.lastPrompt().getInstructions())).hasSize(2);
    }

    @Test
    void tcoAndFinancingNumbersComeFromTheCalculatorsNotFromTheModel() throws Exception {
        model.enqueue(
                Reply.calling("calculateTco", "{\"variantId\":\"golf-1-5-tsi-130\"}"),
                Reply.calling("calculateTco", "{\"variantId\":\"golf-1-5-tsi-130\",\"years\":5,\"kmPerYear\":15000,\"fuelPriceEurPerLiter\":2.0}"),
                Reply.calling("calculateTco", "{\"variantId\":\"golf-1-5-tsi-130\",\"aprPercent\":6}"),
                Reply.calling("calculateFinancing", "{\"priceEur\":20000,\"downPaymentEur\":5000,\"aprPercent\":6,\"months\":48}"),
                Reply.of("Cifre."));
        String token = newSession();
        List<Sse> events = parse(send(token, newConversation(token), "Cat costa un Golf?").andReturn().getResponse().getContentAsString());

        List<String> results = toolResults(model.lastPrompt().getInstructions());
        // Distanta lipseste din profil si din cerere: se presupune 15000 km/an, si modelul este informat.
        assertThat(results.get(0)).contains("\"totalCostEur\":28660.00").contains("15000 km/year assumed");
        assertThat(results.get(1)).contains("\"energyEur\":8400.00");
        assertThat(results.get(2)).contains("aprPercent and loanMonths must be given together");
        assertThat(results.get(3)).contains("\"monthlyPaymentEur\":352.28").contains("\"totalInterestEur\":1909.44");
        assertThat(events.stream().filter(e -> e.name().equals("tool") && e.data().contains("finished")).map(e -> JsonPath.<String>read(e.data(), "$.summary")).toList())
                .contains("Cost total pe 5 ani: 28660.00 €", "Rata lunară: 352.28 €");
    }

    @Test
    void evVsIceUsesTheMileageAndChargingAccessFromTheProfile() throws Exception {
        model.enqueue(
                Reply.calling("updateBuyerProfile", "{\"yearlyKm\":15000,\"homeCharging\":true}"),
                Reply.calling("evVsIceBreakEven", "{\"evVariantId\":\"spring-electric-65\",\"iceVariantId\":\"sandero-tce-90\"}"),
                Reply.calling("evVsIceBreakEven", "{\"evVariantId\":\"sandero-tce-90\",\"iceVariantId\":\"golf-1-5-tsi-130\"}"),
                Reply.of("Se plateste."));
        String token = newSession();
        List<Sse> events = parse(send(token, newConversation(token), "Se plateste un electric pentru mine?").andReturn().getResponse().getContentAsString());

        List<String> results = toolResults(model.lastPrompt().getInstructions());
        assertThat(results.get(1)).contains("\"tcoBreakEvenYears\":1.8").contains("\"simplePaybackYears\":3.1").contains("\"annualRunningSavingEur\":762.25");
        assertThat(results.get(2)).contains("evVariantId must be an electric variant");
        assertThat(events.stream().filter(e -> e.name().equals("tool") && e.data().contains("finished")).map(e -> JsonPath.<String>read(e.data(), "$.summary")).toList())
                .contains("Amortizare simplă: 3.1 ani");
    }

    @Test
    void aRunawayToolLoopIsCutOffAndTheAnswerStillCompletes() throws Exception {
        for (int i = 0; i < 8; i++) {
            model.enqueue(Reply.calling("getShortlist", "{}"));
        }
        model.enqueue(Reply.of("Gata."));
        String token = newSession();

        List<Sse> events = parse(send(token, newConversation(token), "Arata-mi lista").andReturn().getResponse().getContentAsString());

        // Plafonul este 6 apeluri (cpa.llm.max-tool-calls); al saptelea primeste o eroare in loc sa ruleze.
        assertThat(events.stream().filter(e -> e.name().equals("tool") && e.data().contains("started")).count()).isEqualTo(6);
        assertThat(toolResults(model.lastPrompt().getInstructions()).get(6)).contains("Tool call limit reached");
        assertThat(events).extracting(Sse::name).endsWith("token", "done");
    }

    @Test
    void theStoredAnswerRemembersWhichToolsRanAndWhichCardsWereShown() throws Exception {
        model.enqueue(Reply.calling("searchVehicles", "{\"maxPriceEur\":20000,\"limit\":2}"), Reply.of("Doua optiuni."));
        String token = newSession();
        String conversation = newConversation(token);

        List<Sse> events = parse(send(token, conversation, "Ceva sub 20000").andReturn().getResponse().getContentAsString());

        String assistantId = JsonPath.read(events.get(0).data(), "$.messageId");
        String metadata = jdbc.queryForObject("select metadata from message where id = ?", String.class, UUID.fromString(assistantId));
        assertThat(JsonPath.<List<String>>read(metadata, "$.tools")).containsExactly("searchVehicles");
        assertThat(JsonPath.<List<String>>read(metadata, "$.shownVariantIds")).hasSize(2);
    }

    // ---------- ajutoare ----------

    private static List<String> toolResults(List<Message> instructions) {
        return instructions.stream()
                .filter(ToolResponseMessage.class::isInstance)
                .flatMap(m -> ((ToolResponseMessage) m).getResponses().stream())
                .map(ToolResponseMessage.ToolResponse::responseData)
                .toList();
    }

    private ResultActions send(String token, String conversation, String content) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/api/v1/conversations/" + conversation + "/messages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":" + MAPPER.writeValueAsString(content) + "}"));
    }

    private String newSession() throws Exception {
        String body = mvc.perform(MockMvcRequestBuilders.post("/api/v1/sessions")).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String newConversation(String token) throws Exception {
        String body = mvc.perform(MockMvcRequestBuilders.post("/api/v1/conversations").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}
