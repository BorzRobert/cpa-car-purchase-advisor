package ro.cpa.advisor.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuyerProfileRepository extends JpaRepository<BuyerProfileEntity, UUID> {}
