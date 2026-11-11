package ro.cpa.advisor.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import ro.cpa.advisor.ai.EvalHarness.EvalCheck;
import ro.cpa.advisor.ai.EvalHarness.EvalRun;
import ro.cpa.advisor.ai.EvalHarness.EvalScenario;

/**
 * Evaluarea pe scenarii „de aur” cu modelul INREGISTRAT: ruleaza in CI, fara cheie si fara retea, deterministic. Aceleasi
 * scenarii se pot rula cu modelul real prin {@link RealModelEvalTest} (manual). Fereastra mica de istoric face ca scenariul
 * de memorie sa declanseze rezumarea dupa cateva replici.
 */
@SpringBootTest(
        properties = {
            RecordedAdvisorConfig.TEST_KEY,
            "spring.datasource.url=jdbc:h2:mem:eval-recorded;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "cpa.limits.requests-per-minute-per-session=100000",
            "cpa.limits.requests-per-minute-per-ip=100000",
            "cpa.limits.sessions-per-hour-per-ip=100000",
            "cpa.llm.history-messages=4",
            "cpa.llm.summary-batch-messages=2"
        })
@AutoConfigureMockMvc
@Import(RecordedAdvisorConfig.class)
class RecordedEvalTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    RecordedChatModel model;

    @BeforeEach
    void reset() {
        model.reset();
    }

    static Stream<EvalScenario> scenarios() {
        return EvalScenarios.all().stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void goldenScenario(EvalScenario scenario) throws Exception {
        EvalRun run = new EvalHarness(mvc, jdbc, model).run(scenario);

        for (EvalCheck check : scenario.checks()) {
            try {
                check.verify(run);
            } catch (AssertionError failure) {
                throw new AssertionError("Scenario '" + scenario.id() + "' failed check '" + check.name() + "': " + failure.getMessage(), failure);
            }
        }
        assertThat(run.turns).hasSameSizeAs(scenario.turns());
    }
}
