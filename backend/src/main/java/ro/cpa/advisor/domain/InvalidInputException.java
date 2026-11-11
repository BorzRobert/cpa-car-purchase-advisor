package ro.cpa.advisor.domain;

/** Date primite de la browser care nu trec validarea de domeniu (dupa cea de format, facuta in `api`). */
public class InvalidInputException extends RuntimeException {

    public InvalidInputException(String message) {
        super(message);
    }
}
