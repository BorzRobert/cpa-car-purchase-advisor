package ro.cpa.advisor.domain;

import java.time.Instant;
import java.time.LocalDate;

/** Consumul de tokeni al sesiunii in ziua curenta (UTC) fata de bugetul zilnic; UI-ul il arata ca stare „limita atinsa”. */
public record UsageSummary(
        LocalDate date, long inputTokens, long outputTokens, long dailyTokenBudget, long remainingTokens, Instant resetsAt) {}
