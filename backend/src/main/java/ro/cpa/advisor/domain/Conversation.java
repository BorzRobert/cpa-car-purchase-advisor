package ro.cpa.advisor.domain;

import java.time.Instant;
import java.util.UUID;

/** O conversatie a unei sesiuni. Titlul poate lipsi pana la primul mesaj, cand se deduce din text. */
public record Conversation(UUID id, String title, Instant createdAt, Instant updatedAt) {

    public static final int MAX_TITLE_CHARS = 120;
}
