package ro.cpa.advisor.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.cpa.advisor.config.CpaProperties;
import ro.cpa.advisor.domain.RateLimitExceededException;
import ro.cpa.advisor.domain.UsageSummary;
import ro.cpa.advisor.repository.UsageLogEntity;
import ro.cpa.advisor.repository.UsageLogRepository;

/**
 * Contabilitatea tokenilor si bugetul zilnic (ziua = ziua UTC). Faza 5 apeleaza `record` dupa fiecare raspuns al
 * modelului; `assertWithinBudget` se apeleaza INAINTE de orice munca costisitoare, ca un buget epuizat sa nu mai coste nimic.
 */
@Service
public class UsageService {

    private final UsageLogRepository usageLog;
    private final CpaProperties properties;
    private final Clock clock;

    public UsageService(UsageLogRepository usageLog, CpaProperties properties, Clock clock) {
        this.usageLog = usageLog;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public void record(UUID sessionId, String model, int inputTokens, int outputTokens) {
        Instant now = clock.instant();
        usageLog.save(new UsageLogEntity(UUID.randomUUID(), sessionId, today(), model, inputTokens, outputTokens, now));
    }

    @Transactional(readOnly = true)
    public UsageSummary summary(UUID sessionId) {
        LocalDate day = today();
        long input = usageLog.sumInputTokens(sessionId, day);
        long output = usageLog.sumOutputTokens(sessionId, day);
        long budget = properties.limits().dailyTokenBudgetPerSession();
        return new UsageSummary(day, input, output, budget, Math.max(0, budget - input - output), nextReset());
    }

    @Transactional(readOnly = true)
    public void assertWithinBudget(UUID sessionId) {
        LocalDate day = today();
        long sessionTokens = usageLog.sumInputTokens(sessionId, day) + usageLog.sumOutputTokens(sessionId, day);
        if (sessionTokens >= properties.limits().dailyTokenBudgetPerSession()) {
            throw new RateLimitExceededException("budget-exceeded", "The daily token budget of this session is used up.", secondsUntilReset());
        }
        if (usageLog.sumAllTokens(day) >= properties.limits().dailyTokenBudgetGlobal()) {
            throw new RateLimitExceededException("budget-exceeded", "The advisor reached its daily capacity. Try again tomorrow.", secondsUntilReset());
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(ZoneOffset.UTC));
    }

    private Instant nextReset() {
        return today().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private long secondsUntilReset() {
        return Duration.between(clock.instant(), nextReset()).toSeconds();
    }
}
