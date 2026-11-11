package ro.cpa.advisor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentul OpenAPI este contractul din care frontend-ul isi genereaza tipurile TypeScript (Faza 6);
 * aici stau doar metadatele lui, endpoint-urile se descopera singure din controllere.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    OpenAPI cpaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CPA Backend API")
                        .description("REST API of the Car Purchase Advisor. All data comes from deterministic services; the advisor only explains it.")
                        .version("v1"))
                // Schema Bearer globala: butonul „Authorize” din Swagger UI trimite tokenul de sesiune pe fiecare cerere.
                // Endpoint-urile publice o scot cu @SecurityRequirements.
                .components(new Components().addSecuritySchemes(
                        BEARER_SCHEME, new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").description("Token from POST /api/v1/sessions")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
