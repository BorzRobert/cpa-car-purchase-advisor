package ro.cpa.advisor.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortlistItemRepository extends JpaRepository<ShortlistItemEntity, UUID> {

    List<ShortlistItemEntity> findBySessionIdOrderByCreatedAtDesc(UUID sessionId);

    Optional<ShortlistItemEntity> findByIdAndSessionId(UUID id, UUID sessionId);

    boolean existsBySessionIdAndVariantId(UUID sessionId, String variantId);

    long countBySessionId(UUID sessionId);
}
