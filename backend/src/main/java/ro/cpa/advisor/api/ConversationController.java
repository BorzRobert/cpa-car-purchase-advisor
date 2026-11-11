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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ro.cpa.advisor.domain.Conversation;
import ro.cpa.advisor.domain.SessionPrincipal;
import ro.cpa.advisor.service.ConversationService;

@RestController
@RequestMapping(value = "/api/v1/conversations", produces = "application/json")
@Tag(name = "Conversations")
public class ConversationController {

    private final ConversationService conversations;

    public ConversationController(ConversationService conversations) {
        this.conversations = conversations;
    }

    public record CreateConversationRequest(@Size(max = Conversation.MAX_TITLE_CHARS) String title) {}

    public record RenameConversationRequest(@NotBlank @Size(max = Conversation.MAX_TITLE_CHARS) String title) {}

    @GetMapping
    @Operation(summary = "List conversations", description = "Most recently updated first. The number of conversations per session is capped.")
    public List<Conversation> list(@AuthenticationPrincipal SessionPrincipal principal) {
        return conversations.list(principal.sessionId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a conversation", description = "The title is optional; if empty it is derived from the first message.")
    public Conversation create(@AuthenticationPrincipal SessionPrincipal principal, @RequestBody(required = false) @Valid CreateConversationRequest request) {
        return conversations.create(principal.sessionId(), request == null ? null : request.title());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a conversation")
    public Conversation get(@AuthenticationPrincipal SessionPrincipal principal, @PathVariable UUID id) {
        return conversations.get(principal.sessionId(), id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Rename a conversation")
    public Conversation rename(
            @AuthenticationPrincipal SessionPrincipal principal, @PathVariable UUID id, @RequestBody @Valid RenameConversationRequest request) {
        return conversations.rename(principal.sessionId(), id, request.title());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a conversation with its messages")
    public void delete(@AuthenticationPrincipal SessionPrincipal principal, @PathVariable UUID id) {
        conversations.delete(principal.sessionId(), id);
    }
}
