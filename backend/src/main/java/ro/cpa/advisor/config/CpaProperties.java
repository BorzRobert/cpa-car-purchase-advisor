package ro.cpa.advisor.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Toate setarile proprii aplicatiei (prefix {@code cpa}) intr-un singur loc tipizat, validat la pornire:
 * o valoare gresita opreste aplicatia imediat, nu la prima cerere din productie.
 */
@Validated
@ConfigurationProperties("cpa")
public record CpaProperties(@NotNull @Valid Llm llm, @NotNull @Valid Cors cors) {

    /** Modelul OpenAI folosit de consilier: o singura valoare, fara selector (ADR 0007). */
    public record Llm(@NotBlank String model) {}

    /** Originile browserului care au voie sa apeleze API-ul (frontend-ul pe Vercel, localhost in dezvoltare). */
    public record Cors(@NotEmpty List<String> allowedOrigins) {

        public Cors {
            // `*` ar deschide API-ul oricarui site; cerem enumerare explicita a originilor.
            if (allowedOrigins != null && allowedOrigins.stream().anyMatch(o -> o.isBlank() || o.contains("*"))) {
                throw new IllegalArgumentException("cpa.cors.allowed-origins must list explicit origins, no wildcards or blanks");
            }
        }
    }
}
