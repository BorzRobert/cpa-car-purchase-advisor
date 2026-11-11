package ro.cpa.advisor.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ro.cpa.advisor.domain.Message;
import ro.cpa.advisor.domain.SessionPrincipal;
import ro.cpa.advisor.service.MessageService;

/**
 * Mesajele unei conversatii. In Faza 4 POST salveaza doar mesajul utilizatorului si raspunde cu JSON; in Faza 5 acelasi
 * endpoint trece pe `text/event-stream` si adauga raspunsul consilierului (vezi docs/architecture/communication.md).
 */
@RestController
@RequestMapping(value = "/api/v1/conversations/{conversationId}/messages", produces = "application/json")
@Tag(name = "Messages")
public class MessageController {

    private final MessageService messages;

    public MessageController(MessageService messages) {
        this.messages = messages;
    }

    public record PostMessageRequest(@NotBlank @Size(max = Message.MAX_CHARS) String content) {}

    @GetMapping
    @Operation(summary = "List messages", description = "Oldest first. The number of messages per conversation is capped.")
    public List<Message> list(@AuthenticationPrincipal SessionPrincipal principal, @PathVariable UUID conversationId) {
        return messages.list(principal.sessionId(), conversationId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Post a user message",
            description = "Stores the message (no model call yet). Refused with 429 when the daily token budget is used up.")
    public Message post(
            @AuthenticationPrincipal SessionPrincipal principal, @PathVariable UUID conversationId, @RequestBody @Valid PostMessageRequest request) {
        return messages.addUserMessage(principal.sessionId(), conversationId, request.content());
    }
}
