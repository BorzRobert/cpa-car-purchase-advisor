package ro.cpa.advisor.domain;

import java.time.Instant;
import java.util.UUID;

/** Un vehicul salvat de vizitator; `vehicle` contine doar varianta salvata, in lista `variants`. */
public record ShortlistItem(UUID id, Vehicle vehicle, String note, Instant addedAt) {

    public static final int MAX_NOTE_CHARS = 500;
}
