package ro.cpa.advisor.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {

    // `id` ca al doilea criteriu face ordinea stabila si cand doua mesaje au acelasi moment.
    List<MessageEntity> findByConversationIdOrderByCreatedAtAscIdAsc(UUID conversationId);

    long countByConversationId(UUID conversationId);
}
