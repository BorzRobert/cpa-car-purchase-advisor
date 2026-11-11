package ro.cpa.advisor.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {

    List<ConversationEntity> findBySessionIdOrderByUpdatedAtDesc(UUID sessionId);

    /** Interogarea include mereu sesiunea: asa un id al altcuiva arata exact ca unul inexistent. */
    Optional<ConversationEntity> findByIdAndSessionId(UUID id, UUID sessionId);

    long countBySessionId(UUID sessionId);
}
