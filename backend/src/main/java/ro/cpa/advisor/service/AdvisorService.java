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
import ro.cpa.advisor.ai.AdvisorPrompt;
import ro.cpa.advisor.config.CpaProperties;
import ro.cpa.advisor.domain.AdvisorEvent;
import ro.cpa.advisor.domain.AdvisorUnavailableException;
import ro.cpa.advisor.domain.BuyerProfile;
import ro.cpa.advisor.domain.StreamClosedException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Un schimb complet cu consilierul: verificari -> mesajul utilizatorului -> model (in streaming) -> raspunsul salvat si
 * consumul inregistrat. Clasa NU este tranzactionala, intentionat: apelul catre model dureaza secunde, iar o tranzactie
 * deschisa pe tot timpul lui ar tine o conexiune din pool (mica, pe Neon) blocata. Fiecare pas de baza de date este
 * propria lui tranzactie scurta, in {@link MessageService} si {@link UsageService}.
 */
@Service
public class AdvisorService {

    private static final Logger log = LoggerFactory.getLogger(AdvisorService.class);
    private static final int MAX_STORED_CHARS = 20_000;

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
            outcome = model.stream(new AdvisorRequest(sessionId, profile, turn.history(), turn.userMessage().content()), traced);
        } catch (AdvisorUnavailableException failure) {
            fail(events);
            return;
        }

        // Consumul se inregistreaza ORICUM (si la intrerupere, estimat): altfel „opreste” ar fi un mod de a nu plati bugetul.
        usage.record(sessionId, llm.model(), outcome.inputTokens(), outcome.outputTokens());
        logCost(outcome, trace);

        String text = outcome.text().strip();
        if (!text.isEmpty()) {
            messages.addAssistantMessage(conversationId, assistantId, truncate(text), metadata(outcome, trace));
        }
        if (outcome.aborted()) {
            return;
        }
        if (text.isEmpty()) {
            fail(events);
            return;
        }
        try {
            events.accept(new AdvisorEvent.Done(new AdvisorEvent.TokenUsage(outcome.inputTokens(), outcome.outputTokens())));
        } catch (StreamClosedException closed) {
            // Raspunsul si consumul sunt deja salvate; nu mai e nimeni care sa primeasca `done`.
        }
    }

    private void fail(Consumer<AdvisorEvent> events) {
        try {
            events.accept(new AdvisorEvent.Failed("advisor-unavailable", "The advisor could not complete the answer."));
        } catch (StreamClosedException closed) {
            // Clientul a plecat deja.
        }
    }

    private void logCost(AdvisorOutcome outcome, ToolTrace trace) {
        // Fara id-uri si fara text: jurnalul retine doar cifre, ca sa se poata urmari costul pe raspuns.
        log.info(
                "Advisor answer: model={} promptVersion={} inputTokens={} outputTokens={} estimated={} aborted={} toolCalls={} costUsd={}",
                llm.model(),
                AdvisorPrompt.VERSION,
                outcome.inputTokens(),
                outcome.outputTokens(),
                outcome.estimated(),
                outcome.aborted(),
                trace.tools().size(),
                String.format(Locale.ROOT, "%.5f", costUsd(outcome)));
    }

    private double costUsd(AdvisorOutcome outcome) {
        return (outcome.inputTokens() * llm.inputUsdPerMillion() + outcome.outputTokens() * llm.outputUsdPerMillion()) / 1_000_000.0;
    }

    private String metadata(AdvisorOutcome outcome, ToolTrace trace) {
        return json.writeValueAsString(new AnswerMetadata(
                llm.model(),
                AdvisorPrompt.VERSION,
                outcome.inputTokens(),
                outcome.outputTokens(),
                outcome.estimated(),
                outcome.aborted(),
                costUsd(outcome),
                trace.tools(),
                trace.variantIds()));
    }

    private static String truncate(String text) {
        return text.length() <= MAX_STORED_CHARS ? text : text.substring(0, MAX_STORED_CHARS);
    }

    /** Ce se retine despre un raspuns, in `message.metadata`: de unde a venit, cat a costat, ce unelte au rulat, ce carduri s-au aratat. */
    record AnswerMetadata(
            String model,
            int promptVersion,
            int inputTokens,
            int outputTokens,
            boolean estimated,
            boolean aborted,
            double costUsd,
            List<String> tools,
            List<String> shownVariantIds) {}

    /** Urma uneltelor dintr-un raspuns, strans din evenimentele care trec spre client. */
    private static final class ToolTrace {
        private final List<String> tools = new ArrayList<>();
        private final Set<String> variantIds = new LinkedHashSet<>();

        synchronized void observe(AdvisorEvent event) {
            if (event instanceof AdvisorEvent.Tool tool && AdvisorEvent.Tool.STARTED.equals(tool.status())) {
                tools.add(tool.toolName());
            } else if (event instanceof AdvisorEvent.Vehicles vehicles) {
                vehicles.cards().forEach(card -> variantIds.add(card.variantId()));
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
