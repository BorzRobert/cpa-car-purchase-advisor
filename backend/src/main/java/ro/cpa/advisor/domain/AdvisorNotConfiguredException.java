package ro.cpa.advisor.domain;

/**
 * Consilierul nu poate raspunde pentru ca nu exista cheia OpenAI. Este o stare asteptata (aplicatia porneste
 * si fara cheie), nu un bug, deci se traduce intr-un 503 tipat pe care UI-ul il arata prietenos.
 */
public class AdvisorNotConfiguredException extends RuntimeException {

    public AdvisorNotConfiguredException() {
        super("The advisor is not configured on this server.");
    }
}
