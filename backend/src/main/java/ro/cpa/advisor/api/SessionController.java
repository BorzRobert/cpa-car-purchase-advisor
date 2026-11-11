package ro.cpa.advisor.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ro.cpa.advisor.domain.IssuedSession;
import ro.cpa.advisor.domain.SessionInfo;
import ro.cpa.advisor.domain.SessionPrincipal;
import ro.cpa.advisor.service.SessionService;

/** Identitatea anonima: un token nou, informatii despre sesiunea curenta si „sterge-mi datele”. */
@RestController
@RequestMapping(value = "/api/v1/sessions", produces = "application/json")
@Tag(name = "Sessions")
public class SessionController {

    private final SessionService sessions;

    public SessionController(SessionService sessions) {
        this.sessions = sessions;
    }

    @PostMapping
    @SecurityRequirements
    @Operation(
            summary = "Create an anonymous session",
            description = "Returns an opaque token, shown only once. Send it as `Authorization: Bearer <token>` on every other call.")
    public ResponseEntity<IssuedSession> create() {
        // `no-store`: raspunsul contine un secret si nu trebuie sa ramana in cache-ul browserului sau al unui proxy.
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(sessions.create());
    }

    @GetMapping("/current")
    @Operation(summary = "Current session", description = "Also serves to check that a stored token is still valid.")
    public SessionInfo current(@AuthenticationPrincipal SessionPrincipal principal) {
        return sessions.info(principal.sessionId());
    }

    @DeleteMapping("/current")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete my data", description = "Deletes the session with its profile, conversations and shortlist. The token stops working.")
    public void delete(@AuthenticationPrincipal SessionPrincipal principal) {
        sessions.delete(principal.sessionId());
    }
}
