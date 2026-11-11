package ro.cpa.advisor.domain;

import java.time.Instant;

/** Ce vede clientul despre sesiunea lui; tokenul brut apare o singura data, la creare (vezi `IssuedSession`). */
public record SessionInfo(Instant createdAt, Instant lastSeenAt) {}
