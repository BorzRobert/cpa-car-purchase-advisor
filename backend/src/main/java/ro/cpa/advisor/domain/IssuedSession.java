package ro.cpa.advisor.domain;

import java.time.Instant;

/** Raspunsul la crearea sesiunii. Tokenul brut nu mai poate fi recuperat de nicaieri dupa aceasta cerere. */
public record IssuedSession(String token, Instant createdAt) {}
