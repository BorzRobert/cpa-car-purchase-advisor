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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ro.cpa.advisor.domain.SessionPrincipal;
import ro.cpa.advisor.domain.ShortlistItem;
import ro.cpa.advisor.service.ShortlistService;

@RestController
@RequestMapping(value = "/api/v1/shortlist", produces = "application/json")
@Tag(name = "Shortlist")
public class ShortlistController {

    private final ShortlistService shortlist;

    public ShortlistController(ShortlistService shortlist) {
        this.shortlist = shortlist;
    }

    public record AddShortlistItemRequest(
            @NotBlank @Size(max = 80) String variantId, @Size(max = ShortlistItem.MAX_NOTE_CHARS) String note) {}

    @GetMapping
    @Operation(summary = "List the shortlist", description = "Newest first. Each item carries its vehicle with only the saved variant.")
    public List<ShortlistItem> list(@AuthenticationPrincipal SessionPrincipal principal) {
        return shortlist.list(principal.sessionId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a vehicle variant", description = "404 for an unknown variant, 409 if already saved or the shortlist is full.")
    public ShortlistItem add(@AuthenticationPrincipal SessionPrincipal principal, @RequestBody @Valid AddShortlistItemRequest request) {
        return shortlist.add(principal.sessionId(), request.variantId(), request.note());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a shortlist item")
    public void remove(@AuthenticationPrincipal SessionPrincipal principal, @PathVariable UUID id) {
        shortlist.remove(principal.sessionId(), id);
    }
}
