package ro.cpa.advisor.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import ro.cpa.advisor.domain.SessionToken;
import ro.cpa.advisor.service.UsageService;

/**
 * Bugetele zilnice de tokeni, intr-o clasa separata cu baza ei: bugetul global se imparte intre toate sesiunile din baza,
 * deci epuizarea lui ar strica orice alt test care ar posta mesaje in aceeasi baza.
 */
@SpringBootTest(
        properties = {
            "spring.ai.openai.api-key=",
            "spring.datasource.url=jdbc:h2:mem:budgets;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "cpa.limits.requests-per-minute-per-session=100000",
            "cpa.limits.requests-per-minute-per-ip=100000",
            "cpa.limits.sessions-per-hour-per-ip=100000",
            "cpa.limits.daily-token-budget-per-session=1000",
            "cpa.limits.daily-token-budget-global=2500"
        })
@AutoConfigureMockMvc
class DailyBudgetTest extends LimitsTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    UsageService usage;

    @Test
    void sessionBudgetThenGlobalBudgetStopMessagesBeforeAnyWorkIsDone() throws Exception {
        // 1) Bugetul sesiunii (1000 de tokeni).
        String token = sessionToken("10.5.0.1");
        String messages = "/api/v1/conversations/" + createConversation(token, "10.5.0.1") + "/messages";
        post(token, "10.5.0.1", messages, "{\"content\":\"inainte de buget\"}").andExpect(status().isCreated());

        usage.record(sessionId(token), "test-model", 700, 300);
        post(token, "10.5.0.1", messages, "{\"content\":\"dupa buget\"}")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("budget-exceeded"))
                .andExpect(jsonPath("$.detail").value(containsString("session")))
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
        get(token, "10.5.0.1", "/api/v1/usage").andExpect(jsonPath("$.remainingTokens").value(0));

        // 2) Bugetul global (2500): o sesiune proaspata, fara consum propriu, este oprita de capacitatea zilei.
        usage.record(sessionId(sessionToken("10.5.0.2")), "test-model", 1000, 1000);
        String fresh = sessionToken("10.5.0.3");
        String freshMessages = "/api/v1/conversations/" + createConversation(fresh, "10.5.0.3") + "/messages";
        post(fresh, "10.5.0.3", freshMessages, "{\"content\":\"salut\"}")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("budget-exceeded"))
                .andExpect(jsonPath("$.detail").value(containsString("capacity")));
    }

    private UUID sessionId(String token) {
        return jdbc.queryForObject("select id from visitor_session where token_hash = ?", UUID.class, SessionToken.hash(token));
    }
}
