package ro.cpa.advisor.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitorSessionRepository extends JpaRepository<VisitorSessionEntity, UUID> {

    Optional<VisitorSessionEntity> findByTokenHash(String tokenHash);
}
