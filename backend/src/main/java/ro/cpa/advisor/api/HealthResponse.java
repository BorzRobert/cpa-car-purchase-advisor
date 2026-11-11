package ro.cpa.advisor.api;

import io.swagger.v3.oas.annotations.media.Schema;

/** Raspunsul endpoint-ului de status; spune doar DACA e configurat consilierul, niciodata cheia. */
public record HealthResponse(
        @Schema(example = "UP") String status,
        @Schema(example = "cpa-backend") String service,
        @Schema(example = "0.0.1-SNAPSHOT") String version,
        @Schema(description = "False when OPENAI_API_KEY is missing; the advisor endpoints then answer 503.") boolean advisorConfigured) {}
