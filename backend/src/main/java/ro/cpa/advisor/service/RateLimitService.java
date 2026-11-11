package ro.cpa.advisor.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.Duration;
import java.util.UUID;
import org.springframework.stereotype.Service;
import ro.cpa.advisor.config.CpaProperties;
import ro.cpa.advisor.domain.RateLimitExceededException;

/**
 * Limite de rata in memorie (Bucket4j), cu galeti separate pe sesiune, pe IP si pe crearea de sesiuni noi per IP.
 * Sunt per instanta: aplicatia ruleaza intr-un singur container pe planul Free, deci nu e nevoie de o stare comuna.
 * Cache-ul cu expirare impiedica harta de galeti sa creasca la nesfarsit (cheile sunt date controlate de client).
 */
@Service
public class RateLimitService {

    private final Cache<String, Bucket> sessionBuckets = newCache();
    private final Cache<String, Bucket> ipBuckets = newCache();
    private final Cache<String, Bucket> sessionCreationBuckets = newCache();
    private final CpaProperties.Limits limits;

    public RateLimitService(CpaProperties properties) {
        this.limits = properties.limits();
    }

    /** O cerere API: consuma din galeata IP-ului si, daca vizitatorul e autentificat, din cea a sesiunii. */
    public void checkRequest(String clientIp, UUID sessionId) {
        consume(ipBuckets, clientIp, limits.requestsPerMinutePerIp(), Duration.ofMinutes(1));
        if (sessionId != null) {
            consume(sessionBuckets, sessionId.toString(), limits.requestsPerMinutePerSession(), Duration.ofMinutes(1));
        }
    }

    /** Creare de sesiune noua: strans per IP, ca un script sa nu-si reseteze bugetul zilnic cu sesiuni proaspete. */
    public void checkSessionCreation(String clientIp) {
        consume(sessionCreationBuckets, clientIp, limits.sessionsPerHourPerIp(), Duration.ofHours(1));
    }

    private static void consume(Cache<String, Bucket> cache, String key, int capacity, Duration period) {
        Bucket bucket = cache.get(key, k -> Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(capacity).refillGreedy(capacity, period).build())
                .build());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long seconds = (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L;
            throw new RateLimitExceededException("rate-limited", "Too many requests. Slow down and retry shortly.", seconds);
        }
    }

    private static Cache<String, Bucket> newCache() {
        return Caffeine.newBuilder().maximumSize(10_000).expireAfterAccess(Duration.ofHours(2)).build();
    }
}
