package ro.cpa.advisor.service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.cpa.advisor.ai.AdvisorAvailability;
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
 * Mesajele unei conversatii. `startTurn` este poarta de intrare a fiecarui schimb cu consilierul: toate verificarile
 * (proprietar, buget, text, limita de mesaje, cheie) se fac aici, INAINTE de orice apel costisitor, si doar apoi se
 * salveaza mesajul utilizatorului. Raspunsul consilierului se salveaza separat, dupa ce a fost generat.
 */
@Service
public class MessageService {

    private static final int TITLE_FROM_MESSAGE_CHARS = 60;

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final UsageService usage;
    private final AdvisorAvailability advisor;
    private final CpaProperties properties;
    private final Clock clock;

    public MessageService(
            ConversationRepository conversations,
            MessageRepository messages,
            UsageService usage,
            AdvisorAvailability advisor,
            CpaProperties properties,
            Clock clock) {
        this.conversations = conversations;
        this.messages = messages;
        this.usage = usage;
        this.advisor = advisor;
        this.properties = properties;
        this.clock = clock;
    }

    /** Mesajul nou al utilizatorului, salvat, plus ultimele mesaje dinaintea lui (contextul conversatiei). */
    public record Turn(Message userMessage, List<Message> history) {}

    @Transactional(readOnly = true)
    public List<Message> list(UUID sessionId, UUID conversationId) {
        findConversation(sessionId, conversationId);
        return messages.findByConversationIdOrderByCreatedAtAscIdAsc(conversationId).stream().map(MessageEntity::toDomain).toList();
    }

    @Transactional
    public Turn startTurn(UUID sessionId, UUID conversationId, String rawContent) {
        ConversationEntity conversation = findConversation(sessionId, conversationId);
        usage.assertWithinBudget(sessionId);

        String content = TextRules.multiLine(rawContent, Message.MAX_CHARS, "content");
        if (content == null) {
            throw new InvalidInputException("content must not be blank.");
        }
        List<Message> existing =
                messages.findByConversationIdOrderByCreatedAtAscIdAsc(conversationId).stream().map(MessageEntity::toDomain).toList();
        if (existing.size() >= properties.limits().maxMessagesPerConversation()) {
            throw new LimitReachedException("This conversation reached its message limit. Start a new conversation.");
        }
        // Ultima verificare: o cheie lipsa nu trebuie sa mascheze o eroare a utilizatorului (400, 404, 409, 429).
        advisor.requireConfigured();

        var now = clock.instant();
        MessageEntity saved = messages.save(new MessageEntity(UUID.randomUUID(), conversationId, MessageRole.USER, content, now));
        if (conversation.getTitle() == null) {
            conversation.rename(titleFrom(content), now);
        } else {
            conversation.touch(now);
        }
        int from = Math.max(0, existing.size() - properties.llm().historyMessages());
        return new Turn(saved.toDomain(), existing.subList(from, existing.size()));
    }

    /**
     * Salveaza raspunsul consilierului. Daca utilizatorul a sters conversatia cat timp se genera raspunsul, nu mai
     * avem unde sa-l punem: il ignoram in loc sa incalcam cheia straina.
     */
    @Transactional
    public void addAssistantMessage(UUID conversationId, UUID messageId, String content, String metadataJson) {
        var conversation = conversations.findById(conversationId);
        if (conversation.isEmpty()) {
            return;
        }
        var now = clock.instant();
        messages.save(new MessageEntity(messageId, conversationId, MessageRole.ASSISTANT, content, metadataJson, now));
        conversation.get().touch(now);
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
