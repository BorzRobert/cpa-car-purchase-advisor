package ro.cpa.advisor.api;

import java.util.ArrayList;
import java.util.List;

/** Citeste un corp `text/event-stream` in evenimente (comentariile de heartbeat se ignora). */
public final class SseTestSupport {

    public record Sse(String name, String data) {}

    private SseTestSupport() {}

    public static List<Sse> parse(String body) {
        List<Sse> events = new ArrayList<>();
        for (String block : body.split("\n\n")) {
            String name = null;
            String data = null;
            for (String line : block.split("\n")) {
                if (line.startsWith("event: ")) {
                    name = line.substring(7);
                } else if (line.startsWith("data: ")) {
                    data = line.substring(6);
                }
            }
            if (name != null) {
                events.add(new Sse(name, data));
            }
        }
        return events;
    }
}
