package ro.cpa.advisor.api;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import ro.cpa.advisor.ai.RecordedAdvisorConfig;

/** Limitele de rata si de volum, coborate ca sa fie usor de atins (baza H2 proprie, ca testele sa nu se influenteze). */
@SpringBootTest(
        properties = {
            RecordedAdvisorConfig.TEST_KEY,
            "spring.datasource.url=jdbc:h2:mem:rate-limits;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "cpa.limits.requests-per-minute-per-session=5",
            "cpa.limits.requests-per-minute-per-ip=1000",
            "cpa.limits.sessions-per-hour-per-ip=2",
            "cpa.limits.max-conversations-per-session=2",
            "cpa.limits.max-messages-per-conversation=3",
            "cpa.limits.max-shortlist-items=1"
        })
@AutoConfigureMockMvc
@Import(RecordedAdvisorConfig.class)
class RateLimitTest extends LimitsTestSupport {

    @Test
    void creatingSessionsIsLimitedPerIp() throws Exception {
        createSession("10.1.0.1").andExpect(status().isCreated());
        createSession("10.1.0.1").andExpect(status().isCreated());
        createSession("10.1.0.1")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/problem+json"))
                .andExpect(jsonPath("$.code").value("rate-limited"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
        // Alt IP, alta galeata.
        createSession("10.1.0.2").andExpect(status().isCreated());
    }

    @Test
    void requestsAreRateLimitedPerSession() throws Exception {
        String token = sessionToken("10.2.0.1");
        for (int i = 0; i < 5; i++) {
            get(token, "10.2.0.1", "/api/v1/profile").andExpect(status().isOk());
        }
        get(token, "10.2.0.1", "/api/v1/profile")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.code").value("rate-limited"));

        // O alta sesiune (de pe alt IP, ca sa nu atingem limita de creare) nu e afectata.
        String other = sessionToken("10.2.0.2");
        get(other, "10.2.0.2", "/api/v1/profile").andExpect(status().isOk());
    }

    @Test
    void conversationAndMessageCountsAreCapped() throws Exception {
        String token = sessionToken("10.3.0.1");
        post(token, "10.3.0.1", "/api/v1/conversations", "{\"title\":\"a\"}").andExpect(status().isCreated());
        post(token, "10.3.0.1", "/api/v1/conversations", "{\"title\":\"b\"}").andExpect(status().isCreated());
        post(token, "10.3.0.1", "/api/v1/conversations", "{\"title\":\"c\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("limit-reached"));

        // Alta sesiune, ca limita de rata (5/minut) sa nu intervina.
        // Fiecare schimb adauga doua mesaje (utilizator + raspuns): limita de 3 permite doua schimburi, nu trei.
        String token2 = sessionToken("10.3.0.2");
        String messages = "/api/v1/conversations/" + createConversation(token2, "10.3.0.2") + "/messages";
        post(token2, "10.3.0.2", messages, "{\"content\":\"1\"}").andExpect(status().isOk());
        post(token2, "10.3.0.2", messages, "{\"content\":\"2\"}").andExpect(status().isOk());
        post(token2, "10.3.0.2", messages, "{\"content\":\"3\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("limit-reached"));
    }

    @Test
    void shortlistIsCapped() throws Exception {
        String token = sessionToken("10.4.0.1");
        post(token, "10.4.0.1", "/api/v1/shortlist", "{\"variantId\":\"golf-1-5-tsi-130\"}").andExpect(status().isCreated());
        post(token, "10.4.0.1", "/api/v1/shortlist", "{\"variantId\":\"yaris-1-5-hybrid-116\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("limit-reached"));
    }
}
