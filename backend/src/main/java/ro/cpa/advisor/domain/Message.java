package ro.cpa.advisor.domain;

import java.time.Instant;
import java.util.UUID;

/** Un mesaj dintr-o conversatie. Textul utilizatorului este date, nu instructiuni, in orice prompt in care ajunge. */
public record Message(UUID id, UUID conversationId, MessageRole role, String content, Instant createdAt) {

    public static final int MAX_CHARS = 4000;
}
