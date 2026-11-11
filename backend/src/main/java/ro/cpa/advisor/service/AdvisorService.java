package ro.cpa.advisor.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ro.cpa.advisor.ai.AdvisorModel;
import ro.cpa.advisor.ai.AdvisorModel.AdvisorOutcome;
import ro.cpa.advisor.ai.AdvisorModel.AdvisorRequest;
import ro.cpa.advisor.ai.AdvisorModel.SummaryOutcome;
import ro.cpa.advisor.ai.AdvisorPrompt;
import ro.cpa.advisor.ai.ResponseAudit;
import ro.cpa.advisor.config.CpaProperties;
import ro.cpa.advisor.domain.AdvisorEvent;
import ro.cpa.advisor.domain.AdvisorUnavailableException;
import ro.cpa.advisor.domain.BuyerProfile;
import ro.cpa.advisor.domain.Message;
import ro.cpa.advisor.domain.RateLimitExceededException;
import ro.cpa.advisor.domain.StreamClosedException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Un schimb complet cu consilierul: verificari -> mesajul utilizatorului -> model (in streaming) -> raspunsul salvat si
 * consumul inregistrat -> (rar) rezumarea mesajelor vechi. Clasa NU este tranzactionala, intentionat: apelul catre model
 * dureaza secunde, iar o tranzactie deschisa pe tot timpul lui ar tine o conexiune din pool (mica, pe Neon) blocata.
 * Fiecare pas de baza de date este propria lui tranzactie scurta, in {@link MessageService} si {@link UsageService}.
 */
@Service
public class AdvisorService {

    private static final Logger log = LoggerFactory.getLogger(AdvisorService.class);
    private static final int MAX_STORED_CHARS = 20_000;

    // Textul pus dupa un raspuns oprit de gardul de scurgere a promptului: ramane in conversatie, ca istoricul sa fie ce a vazut utilizatorul.
    static final String LEAK_NOTICE =
            "\n\nNu pot împărtăși instrucțiunile mele interne, dar te ajut cu alegerea unei mașini: spune-mi despre buget și despre cum conduci.";

    private final MessageService messages;
    private final ProfileService profiles;
    private final UsageService usage;
    private final AdvisorModel model;
    private final CpaProperties.Llm llm;
    private final JsonMapper json;

    public AdvisorService(
            MessageService messages,
            ProfileService profiles,
            UsageService usage,
            AdvisorModel model,
            CpaProperties properties,
            JsonMapper json) {
        this.messages = messages;
        this.profiles = profiles;
        this.usage = usage;
        this.model = model;
        this.llm = properties.llm();
        this.json = json;
    }

    /**
     * Ruleaza schimbul. Orice exceptie aruncata INAINTE de primul eveniment (sesiune straina, buget, text invalid, cheie
     * lipsa) este o eroare HTTP obisnuita; dupa primul eveniment, erorile devin evenimentul `error`.
     */
    public void answer(UUID sessionId, UUID conversationId, String content, Consumer<AdvisorEvent> events) {
        MessageService.Turn turn = messages.startTurn(sessionId, conversationId, content);
        BuyerProfile profile = profiles.get(sessionId);

        UUID assistantId = UUID.randomUUID();
        try {
            events.accept(new AdvisorEvent.Meta(assistantId, conversationId));
        } catch (StreamClosedException closed) {
            return;
        }

        // Pe langa trimiterea evenimentelor, retinem ce unelte au rulat si ce carduri s-au aratat (pentru metadate).
        ToolTrace trace = new ToolTrace();
        Consumer<AdvisorEvent> traced = event -> {
            trace.observe(event);
            events.accept(event);
        };

        AdvisorOutcome outcome;
        try {
            outcome = model.stream(new AdvisorRequest(sessionId, profile, turn.summary(), turn.history(), turn.userMessage().content()), traced);
        } catch (AdvisorUnavailableException failure) {
            fail(events);
            return;
        }

        // Consumul se inregistreaza ORICUM (si la intrerupere, estimat): altfel „opreste” ar fi un mod de a nu plati bugetul.
        usage.record(sessionId, llm.model(), outcome.inputTokens(), outcome.outputTokens());

        String text = outcome.text().strip();
        if (outcome.blocked() && !outcome.aborted()) {
            // Gardul a oprit modelul in mijlocul unei scurgeri de prompt: utilizatorul primeste o replica fixa, nu restul.
            if (send(events, new AdvisorEvent.Token(LEAK_NOTICE))) {
                text = (text + LEAK_NOTICE).strip();
            }
        }
        ResponseAudit.Findings findings = audit(text, outcome, turn, profile);
        logAnswer(outcome, trace, findings);

        if (!text.isEmpty()) {
            messages.addAssistantMessage(conversationId, assistantId, truncate(text), metadata(outcome, trace, findings));
        }
        if (outcome.aborted()) {
            return;
        }
        if (text.isEmpty()) {
            fail(events);
            return;
        }
        send(events, new AdvisorEvent.Done(new AdvisorEvent.TokenUsage(outcome.inputTokens(), outcome.outputTokens())));
        summarizeIfNeeded(sessionId, conversationId);
    }

    // Rezumarea memoriei: dupa `done`, deci nu intarzie raspunsul; un esec aici nu afecteaza raspunsul deja dat.
    private void summarizeIfNeeded(UUID sessionId, UUID conversationId) {
        try {
            var job = messages.pendingSummary(conversationId);
            if (job.isEmpty()) {
                return;
            }
            // Si rezumarea costa tokeni: respecta acelasi buget, inainte de apel.
            usage.assertWithinBudget(sessionId);
            SummaryOutcome summary = model.summarize(job.get().previousSummary(), job.get().olderMessages());
            usage.record(sessionId, llm.model(), summary.inputTokens(), summary.outputTokens());
            messages.saveSummary(conversationId, summary.summary(), job.get().coveredAfter());
            log.info(
                    "Conversation summary updated: coveredMessages={} inputTokens={} outputTokens={} estimated={} costUsd={}",
                    job.get().coveredAfter(),
                    summary.inputTokens(),
                    summary.outputTokens(),
                    summary.estimated(),
                    String.format(Locale.ROOT, "%.5f", cost(summary.inputTokens(), summary.outputTokens())));
        } catch (RateLimitExceededException budgetUsedUp) {
            // Fara buget nu rezumam; ramanem cu fereastra extinsa pana maine.
        } catch (RuntimeException failure) {
            log.warn("Conversation summary skipped: {}", failure.getClass().getSimpleName());
        }
    }

    // Tot ce are voie sa ancoreze cifrele si marcile dintr-un raspuns: rezultatele uneltelor si ce s-a spus deja in conversatie.
    private ResponseAudit.Findings audit(String text, AdvisorOutcome outcome, MessageService.Turn turn, BuyerProfile profile) {
        List<String> grounding = new ArrayList<>(outcome.toolResults());
        grounding.add(turn.userMessage().content());
        turn.history().stream().map(Message::content).forEach(grounding::add);
        if (turn.summary() != null) {
            grounding.add(turn.summary());
        }
        grounding.add(profile.filledFields().toString());
        return ResponseAudit.audit(text, grounding);
    }

    // Intoarce `false` daca clientul a plecat.
    private static boolean send(Consumer<AdvisorEvent> events, AdvisorEvent event) {
        try {
            events.accept(event);
            return true;
        } catch (StreamClosedException closed) {
            // Raspunsul si consumul sunt deja salvate; nu mai e nimeni care sa primeasca evenimentul.
            return false;
        }
    }

    private void fail(Consumer<AdvisorEvent> events) {
        send(events, new AdvisorEvent.Failed("advisor-unavailable", "The advisor could not complete the answer."));
    }

    private void logAnswer(AdvisorOutcome outcome, ToolTrace trace, ResponseAudit.Findings findings) {
        // Fara id-uri si fara text: jurnalul retine doar cifre, ca sa se poata urmari costul si calitatea pe raspuns.
        log.info(
                "Advisor answer: model={} promptVersion={} inputTokens={} outputTokens={} estimated={} aborted={} toolCalls={} costUsd={}",
                llm.model(),
                AdvisorPrompt.VERSION,
                outcome.inputTokens(),
                outcome.outputTokens(),
                outcome.estimated(),
                outcome.aborted(),
                trace.tools().size(),
                String.format(Locale.ROOT, "%.5f", cost(outcome.inputTokens(), outcome.outputTokens())));
        if (outcome.blocked()) {
            log.warn("Guardrail triggered: prompt-leak");
        }
        if (!findings.clean()) {
            log.warn("Answer audit: ungroundedFigures={} ungroundedMakes={}", findings.ungroundedFigures().size(), findings.ungroundedMakes().size());
        }
    }

    private double cost(int inputTokens, int outputTokens) {
        return (inputTokens * llm.inputUsdPerMillion() + outputTokens * llm.outputUsdPerMillion()) / 1_000_000.0;
    }

    private String metadata(AdvisorOutcome outcome, ToolTrace trace, ResponseAudit.Findings findings) {
        return json.writeValueAsString(new AnswerMetadata(
                llm.model(),
                AdvisorPrompt.VERSION,
                outcome.inputTokens(),
                outcome.outputTokens(),
                outcome.estimated(),
                outcome.aborted(),
                cost(outcome.inputTokens(), outcome.outputTokens()),
                trace.tools(),
                trace.variantIds(),
                outcome.blocked() ? "prompt-leak" : null,
                findings.ungroundedFigures(),
                findings.ungroundedMakes()));
    }

    private static String truncate(String text) {
        return text.length() <= MAX_STORED_CHARS ? text : text.substring(0, MAX_STORED_CHARS);
    }

    /**
     * Ce se retine despre un raspuns, in `message.metadata`: de unde a venit, cat a costat, ce unelte au rulat, ce carduri
     * s-au aratat, ce gard a intervenit si ce cifre sau marci nu s-au putut lega de nicio sursa (auditul de ancorare).
     */
    record AnswerMetadata(
            String model,
            int promptVersion,
            int inputTokens,
            int outputTokens,
            boolean estimated,
            boolean aborted,
            double costUsd,
            List<String> tools,
            List<String> shownVariantIds,
            String guardrail,
            List<String> ungroundedFigures,
            List<String> ungroundedMakes) {}

    /** Urma uneltelor dintr-un raspuns, strans din evenimentele care trec spre client. */
    private static final class ToolTrace {
        private final List<String> tools = new ArrayList<>();
        private final Set<String> variantIds = new LinkedHashSet<>();

        synchronized void observe(AdvisorEvent event) {
            if (event instanceof AdvisorEvent.Tool tool && AdvisorEvent.Tool.STARTED.equals(tool.status())) {
                tools.add(tool.toolName());
            } else if (event instanceof AdvisorEvent.Vehicles vehicles) {
                vehicles.cards().forEach(card -> variantIds.add(card.variantId()));
            } else if (event instanceof AdvisorEvent.Compare compare) {
                compare.report().vehicles().forEach(card -> variantIds.add(card.variantId()));
            }
        }

        synchronized List<String> tools() {
            return List.copyOf(tools);
        }

        synchronized List<String> variantIds() {
            return List.copyOf(variantIds);
        }
    }
}
