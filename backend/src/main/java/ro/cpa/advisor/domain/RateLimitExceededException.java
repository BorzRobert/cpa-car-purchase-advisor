package ro.cpa.advisor.domain;

/** Prea multe cereri (limita de rata) sau bugetul zilnic de tokeni epuizat; clientul primeste 429 si `Retry-After`. */
public class RateLimitExceededException extends RuntimeException {

    private final String code;
    private final long retryAfterSeconds;

    public RateLimitExceededException(String code, String message, long retryAfterSeconds) {
        super(message);
        this.code = code;
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    /** `rate-limited` sau `budget-exceeded`. */
    public String code() {
        return code;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
