package ro.cpa.advisor.ai;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Pune modelul inregistrat in locul celui OpenAI. Testele care il folosesc seteaza si `spring.ai.openai.api-key` la o
 * valoare-substituent, ca `AdvisorAvailability` sa vada consilierul ca „configurat”: cheia nu este folosita, nu exista
 * nicio cheie reala in teste.
 */
@TestConfiguration(proxyBeanMethods = false)
public class RecordedAdvisorConfig {

    /** Valoarea pentru `spring.ai.openai.api-key` in testele cu model inregistrat. */
    public static final String TEST_KEY = "spring.ai.openai.api-key=test-key-not-real";

    @Bean
    @Primary
    RecordedChatModel recordedChatModel() {
        return new RecordedChatModel();
    }
}
