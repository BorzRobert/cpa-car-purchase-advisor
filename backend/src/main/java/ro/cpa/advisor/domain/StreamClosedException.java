package ro.cpa.advisor.domain;

/**
 * Clientul a inchis conexiunea (butonul „opreste” sau tab inchis) cat timp raspunsul era in curs. Este o oprire
 * ceruta, nu o eroare: o aruncam din receptorul de evenimente ca sa intrerupem apelul catre model si sa nu mai platim
 * pentru tokeni pe care nu-i citeste nimeni.
 */
public class StreamClosedException extends RuntimeException {

    public StreamClosedException(Throwable cause) {
        super("The client closed the stream.", cause);
    }
}
