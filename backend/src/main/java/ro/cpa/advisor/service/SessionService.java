package ro.cpa.advisor.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.cpa.advisor.domain.IssuedSession;
import ro.cpa.advisor.domain.NotFoundException;
import ro.cpa.advisor.domain.SessionInfo;
import ro.cpa.advisor.domain.SessionPrincipal;
import ro.cpa.advisor.domain.SessionToken;
import ro.cpa.advisor.repository.VisitorSessionEntity;
import ro.cpa.advisor.repository.VisitorSessionRepository;

/** Ciclul de viata al sesiunii anonime: creare, recunoastere dupa token si stergerea tuturor datelor ei. */
@Service
public class SessionService {

    // `last_seen_at` se rescrie cel mult din 5 in 5 minute: fara asta fiecare cerere ar face si o scriere in baza.
    private static final Duration TOUCH_INTERVAL = Duration.ofMinutes(5);

    private final VisitorSessionRepository sessions;
    private final Clock clock;

    public SessionService(VisitorSessionRepository sessions, Clock clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional
    public IssuedSession create() {
        String token = SessionToken.generate();
        Instant now = clock.instant();
        sessions.save(new VisitorSessionEntity(UUID.randomUUID(), SessionToken.hash(token), now));
        return new IssuedSession(token, now);
    }

    /** Un token necunoscut, expirat sau de forma gresita da acelasi rezultat: gol. */
    @Transactional
    public Optional<SessionPrincipal> authenticate(String rawToken) {
        if (!SessionToken.hasValidShape(rawToken)) {
            return Optional.empty();
        }
        Instant now = clock.instant();
        return sessions.findByTokenHash(SessionToken.hash(rawToken)).map(session -> {
            if (Duration.between(session.getLastSeenAt(), now).compareTo(TOUCH_INTERVAL) > 0) {
                session.markSeen(now);
            }
            return new SessionPrincipal(session.getId());
        });
    }

    @Transactional(readOnly = true)
    public SessionInfo info(UUID sessionId) {
        VisitorSessionEntity session = sessions.findById(sessionId).orElseThrow(() -> new NotFoundException("Session not found."));
        return new SessionInfo(session.getCreatedAt(), session.getLastSeenAt());
    }

    /** „Sterge-mi datele”: baza de date sterge in cascada profilul, conversatiile si lista scurta. */
    @Transactional
    public void delete(UUID sessionId) {
        sessions.deleteById(sessionId);
    }
}
