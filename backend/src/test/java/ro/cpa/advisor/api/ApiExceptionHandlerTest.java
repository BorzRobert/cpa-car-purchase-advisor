package ro.cpa.advisor.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.cpa.advisor.config.RequestIdFilter;
import ro.cpa.advisor.domain.AdvisorNotConfiguredException;

/** Format unic de eroare: fiecare tip de exceptie trebuie sa iasa ca ProblemDetail, fara detalii interne. */
class ApiExceptionHandlerTest {

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new ApiExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void advisorNotConfiguredIsATyped503WithRequestId() throws Exception {
        mockMvc.perform(get("/not-configured"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:cpa:problem:advisor-not-configured"))
                .andExpect(jsonPath("$.code").value("advisor-not-configured"))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void unexpectedExceptionDoesNotLeakItsMessage() throws Exception {
        mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Unexpected server error."))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void standardSpringErrorsAreAlsoProblemDetails() throws Exception {
        mockMvc.perform(get("/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("not-found"));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/not-configured")
        String notConfigured() {
            throw new AdvisorNotConfiguredException();
        }

        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("jdbc:postgresql://secret-host/db password=hunter2");
        }
    }
}
