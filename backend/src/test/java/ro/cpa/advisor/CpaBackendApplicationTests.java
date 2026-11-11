package ro.cpa.advisor;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pornirea completa a aplicatiei FARA cheie OpenAI (criteriul "done when" al Fazei 2): contextul, baza H2 cu Flyway,
 * securitatea si springdoc se leaga impreuna, iar consilierul apare ca neconfigurat in loc sa opreasca aplicatia.
 */
@SpringBootTest(properties = "spring.ai.openai.api-key=")
@AutoConfigureMockMvc
class CpaBackendApplicationTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    void bootsWithoutApiKeyAndReportsAdvisorNotConfigured() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.advisorConfigured").value(false));
    }

    @Test
    void actuatorHealthIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void openApiDocumentDescribesTheHealthEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/health")));
    }

    @Test
    void unlistedPathsAreDeniedByDefault() throws Exception {
        mockMvc.perform(get("/api/v1/anything-else")).andExpect(status().isForbidden());
    }
}
