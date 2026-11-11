package ro.cpa.advisor.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import ro.cpa.advisor.domain.Conversation;

@Entity
@Table(name = "conversation")
public class ConversationEntity {

    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(length = 120)
    private String title;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ConversationEntity() {}

    public ConversationEntity(UUID id, UUID sessionId, String title, Instant now) {
        this.id = id;
        this.sessionId = sessionId;
        this.title = title;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getTitle() {
        return title;
    }

    public void rename(String newTitle, Instant now) {
        this.title = newTitle;
        this.updatedAt = now;
    }

    public void touch(Instant now) {
        this.updatedAt = now;
    }

    public Conversation toDomain() {
        return new Conversation(id, title, createdAt, updatedAt);
    }
}
