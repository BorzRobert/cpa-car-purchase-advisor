package ro.cpa.advisor.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Un eveniment de consum de tokeni. `sessionId` devine NULL cand sesiunea e stearsa, dar randul ramane in totalul zilei. */
@Entity
@Table(name = "usage_log")
public class UsageLogEntity {

    @Id
    private UUID id;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(length = 60)
    private String model;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UsageLogEntity() {}

    public UsageLogEntity(UUID id, UUID sessionId, LocalDate usageDate, String model, int inputTokens, int outputTokens, Instant now) {
        this.id = id;
        this.sessionId = sessionId;
        this.usageDate = usageDate;
        this.model = model;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.createdAt = now;
    }
}
