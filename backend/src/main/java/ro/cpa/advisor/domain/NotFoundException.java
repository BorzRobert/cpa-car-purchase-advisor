package ro.cpa.advisor.domain;

/**
 * Resursa nu exista SAU apartine altei sesiuni. Ambele cazuri dau acelasi 404, ca un id ghicit sa nu poata
 * confirma existenta datelor altui vizitator.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
