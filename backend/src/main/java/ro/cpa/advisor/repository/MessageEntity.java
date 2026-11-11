package ro.cpa.advisor.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import ro.cpa.advisor.domain.Message;
import ro.cpa.advisor.domain.MessageRole;

@Entity
@Table(name = "message")
public class MessageEntity {

    @Id
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_role", nullable = false, length = 10)
    private MessageRole role;

    @Column(nullable = false, length = 20000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MessageEntity() {}

    public MessageEntity(UUID id, UUID conversationId, MessageRole role, String content, Instant now) {
        this.id = id;
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.createdAt = now;
    }

    public Message toDomain() {
        return new Message(id, conversationId, role, content, createdAt);
    }
}
