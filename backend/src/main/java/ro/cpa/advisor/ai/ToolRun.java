package ro.cpa.advisor.ai;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import ro.cpa.advisor.domain.AdvisorEvent;

/**
 * Starea unei singure cereri, pasata uneltelor prin `ToolContext`: pentru cine lucreaza (sesiunea), unde raporteaza
 * (evenimentele SSE) si cate apeluri de unealta mai are voie modelul. Plafonul opreste o bucla de unelte care ar
 * consuma tokeni la nesfarsit: bugetul zilnic se verifica doar INAINTEA cererii, nu in mijlocul ei.
 */
final class ToolRun {

    static final String CONTEXT_KEY = "cpa.toolRun";

    private final UUID sessionId;
    private final Consumer<AdvisorEvent> events;
    private final int maxCalls;
    private final AtomicInteger calls = new AtomicInteger();

    ToolRun(UUID sessionId, Consumer<AdvisorEvent> events, int maxCalls) {
        this.sessionId = sessionId;
        this.events = events;
        this.maxCalls = maxCalls;
    }

    UUID sessionId() {
        return sessionId;
    }

    void emit(AdvisorEvent event) {
        events.accept(event);
    }

    /** `false` cand plafonul de apeluri a fost depasit. */
    boolean allowCall() {
        return calls.incrementAndGet() <= maxCalls;
    }
}
