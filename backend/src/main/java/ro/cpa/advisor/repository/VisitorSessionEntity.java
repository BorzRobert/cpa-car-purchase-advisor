package ro.cpa.advisor.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Randul unei sesiuni anonime. Pastreaza doar hash-ul tokenului, niciodata tokenul. */
@Entity
@Table(name = "visitor_session")
public class VisitorSessionEntity {

    @Id
    private UUID id;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    protected VisitorSessionEntity() {}

    public VisitorSessionEntity(UUID id, String tokenHash, Instant now) {
        this.id = id;
        this.tokenHash = tokenHash;
        this.createdAt = now;
        this.lastSeenAt = now;
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void markSeen(Instant now) {
        this.lastSeenAt = now;
    }
}
