package ro.cpa.advisor.domain;

/** Calea nu este deschisa nimanui (regula implicita "refuza tot ce nu e listat"), chiar daca tokenul este valid. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException() {
        super("Access to this resource is not allowed.");
    }
}
