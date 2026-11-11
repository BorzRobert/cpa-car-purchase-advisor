package ro.cpa.advisor.ai;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ro.cpa.advisor.ai.RecordedChatModel.Reply;
import ro.cpa.advisor.api.SseTestSupport;
import ro.cpa.advisor.api.SseTestSupport.Sse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Evaluarea consilierului pe scenarii „de aur”: acelasi set de scenarii ruleaza cu modelul INREGISTRAT (in CI, fara cheie,
 * deterministic) sau cu modelul REAL (manual, cu cheia ta). Un scenariu are replici ale cumparatorului si, pentru modul
 * inregistrat, raspunsurile pregatite ale modelului; verificarile (`EvalCheck`) sunt aceleasi in ambele moduri si
 * privesc ce conteaza: ce unelte s-au chemat, ce evenimente au ajuns la UI, ca cifrele spuse se leaga de rezultatele
 * uneltelor si de calculatoare, ca nu apar masini inventate si ca promptul nu se scurge.
 */
final class EvalHarness {

    /** O replica a cumparatorului si, in modul inregistrat, ce raspunde modelul (apeluri de unelte si text). */
    record EvalTurn(String user, List<Reply> recorded) {}

    /** Un scenariu: replicile, verificarile si daca se aplica doar modelului inregistrat. */
    record EvalScenario(String id, String description, List<EvalTurn> turns, List<EvalCheck> checks) {}

    /** O verificare asupra unei rulari; arunca `AssertionError` daca nu se respecta. */
    interface EvalCheck {
        String name();

        void verify(EvalRun run);
    }

    /** Ce s-a intamplat la o replica: evenimentele SSE, textul raspunsului si metadatele salvate cu el. */
    record TurnResult(String user, List<Sse> events, String answer, String metadataJson) {

        List<String> toolsStarted() {
            return events.stream()
                    .filter(e -> e.name().equals("tool") && e.data().contains("\"status\":\"started\""))
                    .map(e -> JsonPath.<String>read(e.data(), "$.name"))
                    .toList();
        }
    }

    /** O rulare completa a unui scenariu, cu acces la starea de la sfarsit (profil, lista scurta, conversatie). */
    static final class EvalRun {
        final String token;
        final String conversationId;
        final boolean recorded;
        final RecordedChatModel model;
        final List<TurnResult> turns = new ArrayList<>();
        private final MockMvc mvc;
        private final JdbcTemplate jdbc;

        EvalRun(String token, String conversationId, MockMvc mvc, JdbcTemplate jdbc, RecordedChatModel model) {
            this.token = token;
            this.conversationId = conversationId;
            this.mvc = mvc;
            this.jdbc = jdbc;
            this.model = model;
            this.recorded = model != null;
        }

        TurnResult last() {
            return turns.get(turns.size() - 1);
        }

        List<String> allTools() {
            return turns.stream().flatMap(t -> t.toolsStarted().stream()).toList();
        }

        String get(String path) {
            try {
                return mvc.perform(MockMvcRequestBuilders.get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        String conversationSummary() {
            return jdbc.queryForObject("select summary from conversation where id = ?", String.class, UUID.fromString(conversationId));
        }
    }

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final MockMvc mvc;
    private final JdbcTemplate jdbc;
    private final RecordedChatModel recordedModel;

    /** @param recordedModel modelul inregistrat, sau `null` pentru modelul real */
    EvalHarness(MockMvc mvc, JdbcTemplate jdbc, RecordedChatModel recordedModel) {
        this.mvc = mvc;
        this.jdbc = jdbc;
        this.recordedModel = recordedModel;
    }

    EvalRun run(EvalScenario scenario) throws Exception {
        String session = mvc.perform(MockMvcRequestBuilders.post("/api/v1/sessions")).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(session, "$.token");
        String conversation = JsonPath.read(
                mvc.perform(MockMvcRequestBuilders.post("/api/v1/conversations").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id");
        EvalRun run = new EvalRun(token, conversation, mvc, jdbc, recordedModel);

        for (EvalTurn turn : scenario.turns()) {
            if (recordedModel != null) {
                recordedModel.reset();
                recordedModel.enqueue(turn.recorded().toArray(new Reply[0]));
            }
            String body = mvc.perform(MockMvcRequestBuilders.post("/api/v1/conversations/" + conversation + "/messages")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":" + MAPPER.writeValueAsString(turn.user()) + "}"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            List<Sse> events = SseTestSupport.parse(body);
            StringBuilder answer = new StringBuilder();
            events.stream().filter(e -> e.name().equals("token")).forEach(e -> answer.append(JsonPath.<String>read(e.data(), "$.delta")));
            String messageId = JsonPath.read(events.get(0).data(), "$.messageId");
            List<String> metadata = jdbc.queryForList("select metadata from message where id = ?", String.class, UUID.fromString(messageId));
            run.turns.add(new TurnResult(turn.user(), events, answer.toString(), metadata.isEmpty() ? null : metadata.get(0)));
        }
        return run;
    }
}
