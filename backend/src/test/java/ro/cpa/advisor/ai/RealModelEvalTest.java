package ro.cpa.advisor.ai;

import java.util.stream.Stream;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import ro.cpa.advisor.ai.EvalHarness.EvalCheck;
import ro.cpa.advisor.ai.EvalHarness.EvalRun;
import ro.cpa.advisor.ai.EvalHarness.EvalScenario;

/**
 * Aceleasi scenarii, cu modelul REAL si cheia din `OPENAI_API_KEY` (mediu sau `backend/.env`). NU ruleaza in CI: este
 * oprit cat timp `CPA_EVAL_REAL` nu este `true`, ca sa nu coste bani fara sa vrei. Comanda:
 * {@code CPA_EVAL_REAL=true ./mvnw test -Dtest=RealModelEvalTest}. Un model real nu e deterministic: un esec izolat se
 * rerula inainte sa fie tratat ca regresie.
 */
@EnabledIfEnvironmentVariable(named = "CPA_EVAL_REAL", matches = "true")
@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:eval-real;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "cpa.limits.requests-per-minute-per-session=100000",
            "cpa.limits.requests-per-minute-per-ip=100000",
            "cpa.limits.sessions-per-hour-per-ip=100000"
        })
@AutoConfigureMockMvc
class RealModelEvalTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    static Stream<EvalScenario> scenarios() {
        return EvalScenarios.realModel().stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void goldenScenario(EvalScenario scenario) throws Exception {
        EvalRun run = new EvalHarness(mvc, jdbc, null).run(scenario);
        for (EvalCheck check : scenario.checks()) {
            try {
                check.verify(run);
            } catch (AssertionError failure) {
                throw new AssertionError("Scenario '" + scenario.id() + "' failed check '" + check.name() + "': " + failure.getMessage(), failure);
            }
        }
    }
}
