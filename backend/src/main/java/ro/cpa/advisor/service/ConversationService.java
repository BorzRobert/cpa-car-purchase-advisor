package ro.cpa.advisor.service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.cpa.advisor.config.CpaProperties;
import ro.cpa.advisor.domain.Conversation;
import ro.cpa.advisor.domain.LimitReachedException;
import ro.cpa.advisor.domain.NotFoundException;
import ro.cpa.advisor.domain.TextRules;
import ro.cpa.advisor.repository.ConversationEntity;
import ro.cpa.advisor.repository.ConversationRepository;

/** Conversatiile unei sesiuni. Fiecare operatie include sesiunea in interogare: id-ul altcuiva arata ca unul inexistent. */
@Service
public class ConversationService {

    private final ConversationRepository conversations;
    private final CpaProperties properties;
    private final Clock clock;

    public ConversationService(ConversationRepository conversations, CpaProperties properties, Clock clock) {
        this.conversations = conversations;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Conversation> list(UUID sessionId) {
        return conversations.findBySessionIdOrderByUpdatedAtDesc(sessionId).stream().map(ConversationEntity::toDomain).toList();
    }

    @Transactional
    public Conversation create(UUID sessionId, String title) {
        if (conversations.countBySessionId(sessionId) >= properties.limits().maxConversationsPerSession()) {
            throw new LimitReachedException("The conversation limit for this session was reached. Delete one to start another.");
        }
        String cleanTitle = TextRules.singleLine(title, Conversation.MAX_TITLE_CHARS, "title");
        return conversations.save(new ConversationEntity(UUID.randomUUID(), sessionId, cleanTitle, clock.instant())).toDomain();
    }

    @Transactional(readOnly = true)
    public Conversation get(UUID sessionId, UUID conversationId) {
        return find(sessionId, conversationId).toDomain();
    }

    @Transactional
    public Conversation rename(UUID sessionId, UUID conversationId, String title) {
        ConversationEntity conversation = find(sessionId, conversationId);
        String cleanTitle = TextRules.singleLine(title, Conversation.MAX_TITLE_CHARS, "title");
        conversation.rename(cleanTitle, clock.instant());
        return conversation.toDomain();
    }

    @Transactional
    public void delete(UUID sessionId, UUID conversationId) {
        conversations.delete(find(sessionId, conversationId));
    }

    private ConversationEntity find(UUID sessionId, UUID conversationId) {
        return conversations
                .findByIdAndSessionId(conversationId, sessionId)
                .orElseThrow(() -> new NotFoundException("Conversation not found."));
    }
}
