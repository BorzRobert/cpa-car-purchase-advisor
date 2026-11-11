package ro.cpa.advisor.domain;

/** Cererea e valida, dar starea curenta o refuza (de exemplu, vehiculul e deja in lista scurta). */
public class ConflictException extends RuntimeException {

    private final String code;

    public ConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** Cod stabil, in kebab-case; UI-ul il traduce in text pentru utilizator. */
    public String code() {
        return code;
    }
}
