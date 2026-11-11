package ro.cpa.advisor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentul OpenAPI este contractul din care frontend-ul isi genereaza tipurile TypeScript (Faza 6);
 * aici stau doar metadatele lui, endpoint-urile se descopera singure din controllere.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI cpaOpenApi() {
        return new OpenAPI().info(new Info()
                .title("CPA Backend API")
                .description("REST API of the Car Purchase Advisor. All data comes from deterministic services; the advisor only explains it.")
                .version("v1"));
    }
}
