package ro.cpa.advisor.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.cpa.advisor.config.CpaProperties;
import ro.cpa.advisor.domain.Message;
import ro.cpa.advisor.domain.SessionPrincipal;
import ro.cpa.advisor.service.AdvisorService;
import ro.cpa.advisor.service.MessageService;
import tools.jackson.databind.json.JsonMapper;

/**
 * Mesajele unei conversatii. GET le listeaza ca JSON; POST trimite mesajul utilizatorului si intoarce raspunsul
 * consilierului ca `text/event-stream` (contractul SSE din docs/architecture/communication.md).
 */
@RestController
@RequestMapping(value = "/api/v1/conversations/{conversationId}/messages", produces = "application/json")
@Tag(name = "Messages")
public class MessageController {

    private final MessageService messages;
    private final AdvisorService advisor;
    private final JsonMapper json;
    private final ScheduledExecutorService heartbeats;
    private final CpaProperties properties;

    public MessageController(
            MessageService messages,
            AdvisorService advisor,
            JsonMapper json,
            @Qualifier("sseHeartbeatScheduler") ScheduledExecutorService heartbeats,
            CpaProperties properties) {
        this.messages = messages;
        this.advisor = advisor;
        this.json = json;
        this.heartbeats = heartbeats;
        this.properties = properties;
    }

    public record PostMessageRequest(@NotBlank @Size(max = Message.MAX_CHARS) String content) {}

    @GetMapping
    @Operation(summary = "List messages", description = "Oldest first. The number of messages per conversation is capped.")
    public List<Message> list(@AuthenticationPrincipal SessionPrincipal principal, @PathVariable UUID conversationId) {
        return messages.list(principal.sessionId(), conversationId);
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Send a message and stream the advisor's answer",
            description = """
                    Stores the user message, then streams the answer as Server-Sent Events (`text/event-stream`):
                    `meta` {messageId, conversationId}, then `token` {delta} repeatedly, then `done` {usage: {input, output}};
                    `error` (same shape as a problem response) replaces `done` when the answer fails after the stream began.
                    A comment line is sent every ~15 s as a heartbeat. Everything that can be checked up front (401, 404,
                    400, 409, 429, 503) is a regular `application/problem+json` response before the stream starts.
                    Call it with `fetch` and read the body as a stream; `EventSource` cannot send POST or headers.
                    """,
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "The event stream.",
                    content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE, schema = @Schema(type = "string"))))
    public void post(
            @AuthenticationPrincipal SessionPrincipal principal,
            @PathVariable UUID conversationId,
            @RequestBody @Valid PostMessageRequest request,
            HttpServletResponse response) {
        try (SseEventWriter writer = new SseEventWriter(response, json, heartbeats, properties.stream().heartbeatSeconds())) {
            advisor.answer(principal.sessionId(), conversationId, request.content(), writer);
        }
    }
}
