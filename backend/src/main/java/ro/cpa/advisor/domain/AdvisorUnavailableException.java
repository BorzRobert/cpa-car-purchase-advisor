package ro.cpa.advisor.domain;

/**
 * Apelul catre model a esuat (retea, cheie respinsa, limita OpenAI, raspuns gol). Mesajul este generic si fara detalii
 * de la furnizor: ce a raspuns OpenAI poate contine fragmente de cheie, deci ramane doar in jurnal (fara mesajul brut).
 */
public class AdvisorUnavailableException extends RuntimeException {

    public AdvisorUnavailableException(Throwable cause) {
        super("The advisor could not complete the answer.", cause);
    }

    public AdvisorUnavailableException() {
        super("The advisor could not complete the answer.");
    }
}
