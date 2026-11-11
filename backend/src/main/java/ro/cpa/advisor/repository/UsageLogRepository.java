package ro.cpa.advisor.repository;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsageLogRepository extends JpaRepository<UsageLogEntity, UUID> {

    @Query("select coalesce(sum(u.inputTokens), 0) from UsageLogEntity u where u.sessionId = :sessionId and u.usageDate = :day")
    long sumInputTokens(@Param("sessionId") UUID sessionId, @Param("day") LocalDate day);

    @Query("select coalesce(sum(u.outputTokens), 0) from UsageLogEntity u where u.sessionId = :sessionId and u.usageDate = :day")
    long sumOutputTokens(@Param("sessionId") UUID sessionId, @Param("day") LocalDate day);

    /** Totalul global include si randurile sesiunilor sterse (session_id NULL). */
    @Query("select coalesce(sum(u.inputTokens + u.outputTokens), 0) from UsageLogEntity u where u.usageDate = :day")
    long sumAllTokens(@Param("day") LocalDate day);
}
