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

    // Rezumatul rulant al mesajelor vechi (memoria conversatiei); `null` pana cand conversatia depaseste fereastra.
    @Column(length = 4000)
    private String summary;

    // Cate mesaje de la inceputul conversatiei sunt acoperite de `summary`.
    @Column(name = "summary_message_count", nullable = false)
    private int summaryMessageCount;

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

    public String getSummary() {
        return summary;
    }

    public int getSummaryMessageCount() {
        return summaryMessageCount;
    }

    public void updateSummary(String newSummary, int coveredMessages) {
        this.summary = newSummary;
        this.summaryMessageCount = coveredMessages;
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
