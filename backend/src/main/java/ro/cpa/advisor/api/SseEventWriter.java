package ro.cpa.advisor.api;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.MDC;
import ro.cpa.advisor.config.RequestIdFilter;
import ro.cpa.advisor.domain.AdvisorEvent;
import ro.cpa.advisor.domain.StreamClosedException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Scrie evenimentele consilierului ca Server-Sent Events direct in raspunsul HTTP. Nu folosim `SseEmitter`: cererea
 * ruleaza pe un fir virtual, deci blocarea este ieftina, iar fara dispatch asincron nu mai trece cererea a doua oara
 * prin lantul de securitate dupa ce raspunsul a inceput.
 *
 * <p>Antetele se pun la primul eveniment, nu mai devreme: pana atunci o eroare ramane un raspuns `problem+json`
 * obisnuit (401, 404, 429...). Din acel moment statusul 200 este fix, iar erorile devin evenimentul `error`.
 */
final class SseEventWriter implements Consumer<AdvisorEvent>, AutoCloseable {

    private static final byte[] HEARTBEAT = ": keep-alive\n\n".getBytes(StandardCharsets.UTF_8);

    private final HttpServletResponse response;
    private final JsonMapper json;
    private final ScheduledExecutorService scheduler;
    private final int heartbeatSeconds;

    private OutputStream out;
    private ScheduledFuture<?> heartbeat;
    private boolean closed;

    SseEventWriter(HttpServletResponse response, JsonMapper json, ScheduledExecutorService scheduler, int heartbeatSeconds) {
        this.response = response;
        this.json = json;
        this.scheduler = scheduler;
        this.heartbeatSeconds = heartbeatSeconds;
    }

    @Override
    public synchronized void accept(AdvisorEvent event) {
        if (closed) {
            throw new StreamClosedException(null);
        }
        try {
            open();
            writeBytes(("event: " + event.name() + "\ndata: " + json.writeValueAsString(payload(event)) + "\n\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            closed = true;
            throw new StreamClosedException(e);
        }
    }

    // `ping`-ul porneste doar dupa primul eveniment; trimis de pe alt fir, deci toate scrierile sunt sincronizate.
    private void open() throws IOException {
        if (out != null) {
            return;
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        // Spune proxy-urilor nginx (si celor care il imita) sa nu buferizeze fluxul.
        response.setHeader("X-Accel-Buffering", "no");
        out = response.getOutputStream();
        heartbeat = scheduler.scheduleAtFixedRate(this::beat, heartbeatSeconds, heartbeatSeconds, TimeUnit.SECONDS);
    }

    private synchronized void beat() {
        if (closed || out == null) {
            return;
        }
        try {
            writeBytes(HEARTBEAT);
        } catch (IOException e) {
            closed = true;
        }
    }

    private void writeBytes(byte[] bytes) throws IOException {
        out.write(bytes);
        out.flush();
    }

    // Evenimentul `error` are aceeasi forma ca orice eroare a API-ului (RFC 9457), ca UI-ul sa o citeasca la fel.
    private static Object payload(AdvisorEvent event) {
        if (event instanceof AdvisorEvent.Failed failed) {
            Map<String, Object> problem = new LinkedHashMap<>();
            problem.put("type", URI.create("urn:cpa:problem:" + failed.code()).toString());
            problem.put("title", "Advisor unavailable");
            problem.put("status", 502);
            problem.put("detail", failed.detail());
            problem.put("code", failed.code());
            String requestId = MDC.get(RequestIdFilter.MDC_KEY);
            if (requestId != null) {
                problem.put("requestId", requestId);
            }
            return problem;
        }
        return event;
    }

    @Override
    public synchronized void close() {
        closed = true;
        if (heartbeat != null) {
            heartbeat.cancel(false);
        }
    }
}
