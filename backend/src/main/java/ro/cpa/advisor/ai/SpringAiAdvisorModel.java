package ro.cpa.advisor.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;
import ro.cpa.advisor.config.CpaProperties;
import ro.cpa.advisor.domain.AdvisorEvent;
import ro.cpa.advisor.domain.AdvisorUnavailableException;
import ro.cpa.advisor.domain.MessageRole;
import ro.cpa.advisor.domain.StreamClosedException;

/**
 * Singurul loc care vorbeste cu modelul prin Spring AI (ChatClient). Mesajele se construiesc ca obiecte
 * `Message`, nu ca sabloane de text: `ChatClient.user(String)` interpreteaza `{...}` ca parametri de sablon, deci un
 * mesaj al utilizatorului cu acolade ar putea strica sau manipula promptul.
 */
@Component
public class SpringAiAdvisorModel implements AdvisorModel {

    private static final Logger log = LoggerFactory.getLogger(SpringAiAdvisorModel.class);

    private final ChatClient chat;
    private final AdvisorTools tools;
    private final CpaProperties.Llm llm;

    public SpringAiAdvisorModel(ChatClient.Builder chatClientBuilder, AdvisorTools tools, CpaProperties properties) {
        this.chat = chatClientBuilder.build();
        this.tools = tools;
        this.llm = properties.llm();
    }

    @Override
    public AdvisorOutcome stream(AdvisorRequest request, Consumer<AdvisorEvent> events) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(AdvisorPrompt.system(request.profile(), request.summary())));
        for (ro.cpa.advisor.domain.Message turn : request.history()) {
            messages.add(turn.role() == MessageRole.USER
                    ? new UserMessage(AdvisorPrompt.userTurn(turn.content()))
                    : new AssistantMessage(turn.content()));
        }
        messages.add(new UserMessage(AdvisorPrompt.userTurn(request.userMessage())));

        // Fara `streamUsage` OpenAI nu raporteaza tokenii in flux, iar bugetul ar ramane nemasurat.
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(llm.model())
                .maxCompletionTokens(llm.maxOutputTokens())
                .streamUsage(true)
                .build();

        StringBuilder text = new StringBuilder();
        PromptLeakGuard guard = new PromptLeakGuard(AdvisorPrompt.protectedTexts());
        Usage usage = null;
        boolean aborted = false;
        ToolRun run = new ToolRun(request.sessionId(), events, llm.maxToolCalls());
        var call = chat.prompt(new Prompt(messages, options)).tools(tools).toolContext(Map.of(ToolRun.CONTEXT_KEY, run));
        try (Stream<ChatResponse> responses = call.stream().chatResponse().toStream()) {
            for (var it = responses.iterator(); it.hasNext() && !guard.leaked(); ) {
                ChatResponse response = it.next();
                Usage reported = response.getMetadata() == null ? null : response.getMetadata().getUsage();
                // ToolCallingAdvisor aduna consumul peste apelurile succesive ale aceluiasi raspuns (`UsageAccumulator`):
                // ultimul raport este totalul, nu doar ultimul apel.
                if (reported != null && reported.getTotalTokens() != null && reported.getTotalTokens() > 0) {
                    usage = reported;
                }
                String delta = response.getResult() == null || response.getResult().getOutput() == null
                        ? null
                        : response.getResult().getOutput().getText();
                if (delta != null && !delta.isEmpty()) {
                    emit(guard.accept(delta), text, events);
                }
            }
            if (!guard.leaked()) {
                emit(guard.flush(), text, events);
            }
        } catch (StreamClosedException closed) {
            aborted = true;
        } catch (RuntimeException failure) {
            // Mesajul exceptiei OpenAI poate contine fragmente de cheie: in jurnal intra doar tipul.
            log.warn("Model call failed: {}", failure.getClass().getSimpleName());
            throw new AdvisorUnavailableException(failure);
        }

        boolean blocked = guard.leaked();
        List<String> toolResults = run.results();
        if (usage != null && !aborted && !blocked) {
            return new AdvisorOutcome(text.toString(), usage.getPromptTokens(), usage.getCompletionTokens(), false, false, false, toolResults);
        }
        int promptChars = messages.stream().mapToInt(m -> m.getText() == null ? 0 : m.getText().length()).sum();
        return new AdvisorOutcome(text.toString(), TokenEstimate.of(promptChars), TokenEstimate.of(text.length()), true, aborted, blocked, toolResults);
    }

    private static void emit(String released, StringBuilder text, Consumer<AdvisorEvent> events) {
        if (!released.isEmpty()) {
            text.append(released);
            events.accept(new AdvisorEvent.Token(released));
        }
    }

    @Override
    public SummaryOutcome summarize(String previousSummary, List<ro.cpa.advisor.domain.Message> olderMessages) {
        List<String> lines = olderMessages.stream()
                .map(m -> (m.role() == MessageRole.USER ? "[buyer] " : "[advisor] ") + m.content())
                .toList();
        List<Message> messages = List.of(
                new SystemMessage(AdvisorPrompt.summarizerSystem()), new UserMessage(AdvisorPrompt.summaryRequest(previousSummary, lines)));
        OpenAiChatOptions options = OpenAiChatOptions.builder().model(llm.model()).maxCompletionTokens(llm.maxOutputTokens()).build();
        try {
            ChatResponse response = chat.prompt(new Prompt(messages, options)).call().chatResponse();
            String summary = response == null || response.getResult() == null ? null : response.getResult().getOutput().getText();
            if (summary == null || summary.isBlank()) {
                throw new AdvisorUnavailableException();
            }
            String capped = summary.strip();
            capped = capped.length() <= AdvisorPrompt.MAX_SUMMARY_CHARS ? capped : capped.substring(0, AdvisorPrompt.MAX_SUMMARY_CHARS);
            Usage usage = response.getMetadata() == null ? null : response.getMetadata().getUsage();
            if (usage != null && usage.getTotalTokens() != null && usage.getTotalTokens() > 0) {
                return new SummaryOutcome(capped, usage.getPromptTokens(), usage.getCompletionTokens(), false);
            }
            int promptChars = messages.stream().mapToInt(m -> m.getText() == null ? 0 : m.getText().length()).sum();
            return new SummaryOutcome(capped, TokenEstimate.of(promptChars), TokenEstimate.of(capped.length()), true);
        } catch (AdvisorUnavailableException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            log.warn("Summary call failed: {}", failure.getClass().getSimpleName());
            throw new AdvisorUnavailableException(failure);
        }
    }
}
