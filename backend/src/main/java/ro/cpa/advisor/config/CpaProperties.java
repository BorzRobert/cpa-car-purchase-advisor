package ro.cpa.advisor.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Toate setarile proprii aplicatiei (prefix {@code cpa}) intr-un singur loc tipizat, validat la pornire:
 * o valoare gresita opreste aplicatia imediat, nu la prima cerere din productie.
 */
@Validated
@ConfigurationProperties("cpa")
public record CpaProperties(
        @NotNull @Valid Llm llm,
        @NotNull @Valid Cors cors,
        @NotNull @Valid Limits limits,
        @NotNull @Valid Stream stream,
        @NotNull @Valid Pricing pricing) {

    /**
     * Preturile energiei din calculatorul TCO (EUR). Cifre ORIENTATIVE, nevalidate, cu data lor: se revizuiesc inainte de
     * productie, ca si catalogul. Cumparatorul le poate inlocui cu ale lui, prin unelte.
     */
    public record Pricing(
            @Positive double petrolEurPerLiter,
            @Positive double dieselEurPerLiter,
            @Positive double homeElectricityEurPerKwh,
            @Positive double publicElectricityEurPerKwh,
            @NotNull LocalDate dataAsOf) {}

    /**
     * Modelul OpenAI folosit de consilier: o singura valoare, fara selector (ADR 0007, modelul din ADR 0010). Preturile
     * (USD per milion de tokeni) servesc doar jurnalului „cost per raspuns”; bugetul se numara in tokeni, nu in bani.
     */
    public record Llm(
            @NotBlank String model,
            @Positive int maxOutputTokens,
            @Positive int historyMessages,
            @Positive int summaryBatchMessages,
            @Positive int maxToolCalls,
            @PositiveOrZero double inputUsdPerMillion,
            @PositiveOrZero double outputUsdPerMillion) {}

    /** Parametrii fluxului SSE; `heartbeatSeconds` tine proxy-urile sa nu inchida o conexiune care tace in timpul modelului. */
    public record Stream(@Positive int heartbeatSeconds) {}

    /**
     * Protectia unui endpoint public: limite de rata (in memorie, per instanta), de volum per sesiune si bugetul zilnic
     * de tokeni. Valorile implicite sunt in `application.yml`; testele le coboara ca sa fie usor de atins.
     */
    public record Limits(
            @Positive int requestsPerMinutePerSession,
            @Positive int requestsPerMinutePerIp,
            @Positive int sessionsPerHourPerIp,
            @Positive int maxConversationsPerSession,
            @Positive int maxMessagesPerConversation,
            @Positive int maxShortlistItems,
            @Positive long dailyTokenBudgetPerSession,
            @Positive long dailyTokenBudgetGlobal,
            @PositiveOrZero int trustedProxyHops) {}

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
