package ro.cpa.advisor.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import ro.cpa.advisor.domain.AdvisorEvent;
import ro.cpa.advisor.domain.StreamClosedException;
import tools.jackson.databind.json.JsonMapper;

class SseEventWriterTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Test
    void writesNamedEventsWithOneLineOfJson() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        try (SseEventWriter writer = new SseEventWriter(response, json, scheduler, 60)) {
            writer.accept(new AdvisorEvent.Token("linia 1\nlinia 2 \"citat\""));
        }

        // Un `\n` din text nu poate rupe cadrul SSE: JSON-ul il scapa.
        assertThat(response.getContentAsString()).isEqualTo("event: token\ndata: {\"delta\":\"linia 1\\nlinia 2 \\\"citat\\\"\"}\n\n");
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void aBrokenConnectionBecomesAStreamClosedStopSignal() {
        MockHttpServletResponse response = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return new ServletOutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        throw new IOException("Broken pipe");
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setWriteListener(WriteListener listener) {}
                };
            }
        };
        SseEventWriter writer = new SseEventWriter(response, json, scheduler, 60);

        assertThatThrownBy(() -> writer.accept(new AdvisorEvent.Meta(UUID.randomUUID(), UUID.randomUUID()))).isInstanceOf(StreamClosedException.class);
        // Dupa prima esuare nu se mai incearca nimic: urmatorul eveniment opreste imediat producatorul.
        assertThatThrownBy(() -> writer.accept(new AdvisorEvent.Token("x"))).isInstanceOf(StreamClosedException.class);
        writer.close();
    }
}
