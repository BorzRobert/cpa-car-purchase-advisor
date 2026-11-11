package ro.cpa.advisor.domain;

/** Lipseste tokenul de sesiune sau nu este valid; clientul trebuie sa creeze o sesiune noua. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
        super("A valid session token is required. Create one with POST /api/v1/sessions.");
    }
}
