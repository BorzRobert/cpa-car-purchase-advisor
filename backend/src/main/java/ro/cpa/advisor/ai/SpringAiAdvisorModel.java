package ro.cpa.advisor.ai;

import java.util.ArrayList;
import java.util.List;
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
    private final CpaProperties.Llm llm;

    public SpringAiAdvisorModel(ChatClient.Builder chatClientBuilder, CpaProperties properties) {
        this.chat = chatClientBuilder.build();
        this.llm = properties.llm();
    }

    @Override
    public AdvisorOutcome stream(AdvisorRequest request, Consumer<String> onToken) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(AdvisorPrompt.system(request.profile())));
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
        Usage usage = null;
        boolean aborted = false;
        try (Stream<ChatResponse> responses = chat.prompt(new Prompt(messages, options)).stream().chatResponse().toStream()) {
            for (var it = responses.iterator(); it.hasNext(); ) {
                ChatResponse response = it.next();
                Usage reported = response.getMetadata() == null ? null : response.getMetadata().getUsage();
                // Spring AI cumuleaza consumul peste apelurile succesive ale aceluiasi raspuns; ultimul raport este totalul.
                if (reported != null && reported.getTotalTokens() != null && reported.getTotalTokens() > 0) {
                    usage = reported;
                }
                String delta = response.getResult() == null || response.getResult().getOutput() == null
                        ? null
                        : response.getResult().getOutput().getText();
                if (delta != null && !delta.isEmpty()) {
                    text.append(delta);
                    onToken.accept(delta);
                }
            }
        } catch (StreamClosedException closed) {
            aborted = true;
        } catch (RuntimeException failure) {
            // Mesajul exceptiei OpenAI poate contine fragmente de cheie: in jurnal intra doar tipul.
            log.warn("Model call failed: {}", failure.getClass().getSimpleName());
            throw new AdvisorUnavailableException(failure);
        }

        if (usage != null && !aborted) {
            return new AdvisorOutcome(text.toString(), usage.getPromptTokens(), usage.getCompletionTokens(), false, false);
        }
        int promptChars = messages.stream().mapToInt(m -> m.getText() == null ? 0 : m.getText().length()).sum();
        return new AdvisorOutcome(text.toString(), TokenEstimate.of(promptChars), TokenEstimate.of(text.length()), true, aborted);
    }
}
