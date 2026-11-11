package ro.cpa.advisor.ai;

import java.util.List;
import java.util.function.Consumer;
import ro.cpa.advisor.domain.BuyerProfile;
import ro.cpa.advisor.domain.Message;

/**
 * Frontiera dintre aplicatie si model. Serviciile cunosc doar acest contract (nu Spring AI, nu OpenAI), iar testele din
 * CI il ruleaza cu un model inregistrat, fara cheie. Apelul blocheaza firul curent (sunt fire virtuale) si livreaza
 * textul bucata cu bucata prin {@code onToken}.
 */
public interface AdvisorModel {

    /**
     * @param onToken primeste fiecare bucata de text; daca arunca {@link ro.cpa.advisor.domain.StreamClosedException},
     *     apelul catre model se intrerupe si rezultatul are {@code aborted = true}
     * @throws ro.cpa.advisor.domain.AdvisorUnavailableException daca apelul esueaza
     */
    AdvisorOutcome stream(AdvisorRequest request, Consumer<String> onToken);

    /** Ce vede modelul: profilul si ultimele mesaje (fara cel nou) sunt date, mesajul nou este tura curenta. */
    record AdvisorRequest(BuyerProfile profile, List<Message> history, String userMessage) {}

    /**
     * Rezultatul unui apel. Tokenii sunt cei raportati de furnizor; cand lipsesc (sau fluxul a fost intrerupt) sunt
     * estimati, ca o intrerupere sa nu poata ocoli bugetul, iar {@code estimated} spune care e cazul.
     */
    record AdvisorOutcome(String text, int inputTokens, int outputTokens, boolean estimated, boolean aborted) {}
}
