package ro.cpa.advisor.config;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Un singur fir daemon care trimite comentariile de heartbeat ale tuturor fluxurilor SSE active. Sarcina lui este
 * minuscula (o scriere scurta la 15 s per flux), deci nu are nevoie de mai multe fire.
 */
@Configuration
public class StreamConfig {

    @Bean(destroyMethod = "shutdownNow")
    ScheduledExecutorService sseHeartbeatScheduler() {
        return Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "sse-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
    }
}
