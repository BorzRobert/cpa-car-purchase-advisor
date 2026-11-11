package ro.cpa.advisor.domain;

/** O limita de volum per sesiune (conversatii, mesaje, lista scurta) a fost atinsa; sesiunea nu mai poate adauga. */
public class LimitReachedException extends RuntimeException {

    public LimitReachedException(String message) {
        super(message);
    }
}
