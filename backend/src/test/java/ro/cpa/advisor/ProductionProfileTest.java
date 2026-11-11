package ro.cpa.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Profilul `prod` (Render + Neon) pornit pe H2, ca sa nu avem nevoie de o baza reala: verifica ce difera in productie —
 * unelte de dezvoltare oprite, health check care nu atinge baza — si ca aplicatia inca porneste cu setarile de pool.
 * Datasource-ul si originile sunt date explicit, fiindca profilul le cere din mediu fara valoare implicita.
 */
@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:cpa-prod;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "cpa.cors.allowed-origins=https://cpa.example.com",
            "spring.ai.openai.api-key="
        })
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class ProductionProfileTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ApplicationContext context;

    @Test
    void swaggerAndOpenApiAreNotExposed() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
    }

    @Test
    void healthCheckIsUpAndDoesNotQueryTheDatabase() throws Exception {
        // Un indicator de baza de date ar interoga Neon la fiecare health check si nu l-ar mai lasa sa adoarma.
        assertThat(context.containsBean("dbHealthIndicator")).isFalse();
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void statusEndpointStillWorks() throws Exception {
        mockMvc.perform(get("/api/v1/health")).andExpect(status().isOk());
    }
}
