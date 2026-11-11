package ro.cpa.advisor.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shortlist_item")
public class ShortlistItemEntity {

    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "variant_id", nullable = false, length = 80)
    private String variantId;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ShortlistItemEntity() {}

    public ShortlistItemEntity(UUID id, UUID sessionId, String variantId, String note, Instant now) {
        this.id = id;
        this.sessionId = sessionId;
        this.variantId = variantId;
        this.note = note;
        this.createdAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getVariantId() {
        return variantId;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
