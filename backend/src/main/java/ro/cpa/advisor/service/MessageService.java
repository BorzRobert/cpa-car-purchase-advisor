package ro.cpa.advisor.service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.cpa.advisor.config.CpaProperties;
import ro.cpa.advisor.domain.InvalidInputException;
import ro.cpa.advisor.domain.LimitReachedException;
import ro.cpa.advisor.domain.Message;
import ro.cpa.advisor.domain.MessageRole;
import ro.cpa.advisor.domain.NotFoundException;
import ro.cpa.advisor.domain.TextRules;
import ro.cpa.advisor.repository.ConversationEntity;
import ro.cpa.advisor.repository.ConversationRepository;
import ro.cpa.advisor.repository.MessageEntity;
import ro.cpa.advisor.repository.MessageRepository;

/**
 * Mesajele unei conversatii. In Faza 4 se salveaza doar mesajul utilizatorului (fara model); Faza 5 adauga raspunsul
 * consilierului in acelasi flux, pastrand verificarile de aici (proprietar, limita de mesaje, buget).
 */
@Service
public class MessageService {

    private static final int TITLE_FROM_MESSAGE_CHARS = 60;

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final UsageService usage;
    private final CpaProperties properties;
    private final Clock clock;

    public MessageService(
            ConversationRepository conversations,
            MessageRepository messages,
            UsageService usage,
            CpaProperties properties,
            Clock clock) {
        this.conversations = conversations;
        this.messages = messages;
        this.usage = usage;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Message> list(UUID sessionId, UUID conversationId) {
        findConversation(sessionId, conversationId);
        return messages.findByConversationIdOrderByCreatedAtAscIdAsc(conversationId).stream().map(MessageEntity::toDomain).toList();
    }

    @Transactional
    public Message addUserMessage(UUID sessionId, UUID conversationId, String rawContent) {
        ConversationEntity conversation = findConversation(sessionId, conversationId);
        usage.assertWithinBudget(sessionId);

        String content = TextRules.multiLine(rawContent, Message.MAX_CHARS, "content");
        if (content == null) {
            throw new InvalidInputException("content must not be blank.");
        }
        if (messages.countByConversationId(conversationId) >= properties.limits().maxMessagesPerConversation()) {
            throw new LimitReachedException("This conversation reached its message limit. Start a new conversation.");
        }

        var now = clock.instant();
        MessageEntity saved = messages.save(new MessageEntity(UUID.randomUUID(), conversationId, MessageRole.USER, content, now));
        if (conversation.getTitle() == null) {
            conversation.rename(titleFrom(content), now);
        } else {
            conversation.touch(now);
        }
        return saved.toDomain();
    }

    // Titlul implicit vine din primul mesaj, ca lista de conversatii sa nu arate „fara titlu” peste tot.
    private static String titleFrom(String content) {
        String oneLine = content.replaceAll("\\s+", " ").trim();
        if (oneLine.codePointCount(0, oneLine.length()) <= TITLE_FROM_MESSAGE_CHARS) {
            return oneLine;
        }
        return oneLine.substring(0, oneLine.offsetByCodePoints(0, TITLE_FROM_MESSAGE_CHARS - 1)) + "…";
    }

    private ConversationEntity findConversation(UUID sessionId, UUID conversationId) {
        return conversations
                .findByIdAndSessionId(conversationId, sessionId)
                .orElseThrow(() -> new NotFoundException("Conversation not found."));
    }
}
