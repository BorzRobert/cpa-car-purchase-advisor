package ro.cpa.advisor.ai;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import ro.cpa.advisor.domain.AdvisorEvent;
import ro.cpa.advisor.domain.BuyerProfile;
import ro.cpa.advisor.domain.Message;

/**
 * Frontiera dintre aplicatie si model. Serviciile cunosc doar acest contract (nu Spring AI, nu OpenAI), iar testele din
 * CI il ruleaza cu un model inregistrat, fara cheie. Apelul blocheaza firul curent (sunt fire virtuale) si livreaza
 * textul bucata cu bucata prin evenimente.
 */
public interface AdvisorModel {

    /**
     * Textul ajunge ca evenimente {@link AdvisorEvent.Token}; uneltele adauga `tool`, `vehicles`, `profile`, `compare` in
     * acelasi receptor. Primul eveniment pe care il arunca receptorul ca {@link ro.cpa.advisor.domain.StreamClosedException}
     * (clientul a plecat) opreste apelul catre model, iar rezultatul are {@code aborted = true}.
     *
     * @throws ro.cpa.advisor.domain.AdvisorUnavailableException daca apelul esueaza
     */
    AdvisorOutcome stream(AdvisorRequest request, Consumer<AdvisorEvent> events);

    /**
     * Pune la zi rezumatul rulant al conversatiei cu mesajele care au iesit din fereastra. Un apel obisnuit, fara unelte,
     * al carui consum se inregistreaza ca oricare altul.
     *
     * @throws ro.cpa.advisor.domain.AdvisorUnavailableException daca apelul esueaza
     */
    SummaryOutcome summarize(String previousSummary, List<Message> olderMessages);

    /**
     * Ce vede modelul: profilul, rezumatul si ultimele mesaje (fara cel nou) sunt date, mesajul nou este tura curenta.
     * `sessionId` nu intra in prompt: ajunge doar la unelte, ca sa lucreze pe datele sesiunii potrivite.
     */
    record AdvisorRequest(UUID sessionId, BuyerProfile profile, String summary, List<Message> history, String userMessage) {}

    /**
     * Rezultatul unui apel. Tokenii sunt cei raportati de furnizor; cand lipsesc (sau fluxul a fost intrerupt) sunt
     * estimati, ca o intrerupere sa nu poata ocoli bugetul, iar {@code estimated} spune care e cazul. `blocked` inseamna
     * ca un gard (scurgere de prompt) a oprit raspunsul; `text` este doar ce a ajuns la utilizator. `toolResults` sunt
     * rezultatele uneltelor din acest raspuns (JSON), materialul cu care se verifica ancorarea cifrelor.
     */
    record AdvisorOutcome(String text, int inputTokens, int outputTokens, boolean estimated, boolean aborted, boolean blocked, List<String> toolResults) {}

    record SummaryOutcome(String summary, int inputTokens, int outputTokens, boolean estimated) {}
}
