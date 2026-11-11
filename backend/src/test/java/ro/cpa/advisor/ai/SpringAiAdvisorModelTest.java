package ro.cpa.advisor.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import ro.cpa.advisor.ai.AdvisorModel.AdvisorOutcome;
import ro.cpa.advisor.ai.AdvisorModel.AdvisorRequest;
import ro.cpa.advisor.ai.RecordedChatModel.Reply;
import ro.cpa.advisor.config.CpaProperties;
import ro.cpa.advisor.domain.AdvisorUnavailableException;
import ro.cpa.advisor.domain.BuyerProfile;
import ro.cpa.advisor.domain.BudgetKind;
import ro.cpa.advisor.domain.MessageRole;
import ro.cpa.advisor.domain.StreamClosedException;

/** Drumul real prin ChatClient (prompt, streaming, consum), cu modelul inregistrat in loc de OpenAI. */
class SpringAiAdvisorModelTest {

    private final RecordedChatModel chat = new RecordedChatModel();
    private final SpringAiAdvisorModel advisor = new SpringAiAdvisorModel(
            ChatClient.builder(chat), new CpaProperties(new CpaProperties.Llm("test-model", 1000, 20, 2.0, 12.0), null, null, null));

    private static AdvisorRequest request(String userMessage) {
        return new AdvisorRequest(BuyerProfile.empty(), List.of(), userMessage);
    }

    @Test
    void deliversTextInOrderAndReportsTheProviderUsage() {
        chat.enqueue(Reply.of("Ana ", "are ", "mere").withUsage(500, 30));
        List<String> tokens = new ArrayList<>();

        AdvisorOutcome outcome = advisor.stream(request("salut"), tokens::add);

        assertThat(tokens).containsExactly("Ana ", "are ", "mere");
        assertThat(outcome.text()).isEqualTo("Ana are mere");
        assertThat(outcome.inputTokens()).isEqualTo(500);
        assertThat(outcome.outputTokens()).isEqualTo(30);
        assertThat(outcome.estimated()).isFalse();
        assertThat(outcome.aborted()).isFalse();
    }

    @Test
    void anAbortedStreamKeepsThePartialTextAndEstimatesUsage() {
        chat.enqueue(Reply.of("primul ", "al doilea ", "al treilea").withUsage(500, 30));
        List<String> tokens = new ArrayList<>();

        AdvisorOutcome outcome = advisor.stream(request("salut"), delta -> {
            tokens.add(delta);
            if (tokens.size() == 2) {
                throw new StreamClosedException(null);
            }
        });

        assertThat(outcome.aborted()).isTrue();
        assertThat(outcome.text()).isEqualTo("primul al doilea ");
        // O intrerupere nu e gratuita: tokenii se estimeaza, ca „opreste” sa nu poata ocoli bugetul.
        assertThat(outcome.estimated()).isTrue();
        assertThat(outcome.inputTokens()).isPositive();
        assertThat(outcome.outputTokens()).isPositive();
    }

    @Test
    void aFailureIsReportedWithoutTheProvidersMessage() {
        chat.enqueue(Reply.failing());

        assertThatThrownBy(() -> advisor.stream(request("salut"), delta -> {}))
                .isInstanceOf(AdvisorUnavailableException.class)
                .hasMessage("The advisor could not complete the answer.");
    }

    @Test
    void theModelSeesSystemPromptThenDelimitedHistoryThenTheNewTurn() {
        var profile = new BuyerProfile(
                new BigDecimal("20000"), BudgetKind.TOTAL, null, 15000, null, 4, List.of(), List.of(), null, null, List.of(), List.of(), null);
        var history = List.of(
                new ro.cpa.advisor.domain.Message(UUID.randomUUID(), UUID.randomUUID(), MessageRole.USER, "primul meu mesaj", null),
                new ro.cpa.advisor.domain.Message(UUID.randomUUID(), UUID.randomUUID(), MessageRole.ASSISTANT, "raspunsul tau", null));

        advisor.stream(new AdvisorRequest(profile, history, "al doilea {nume}"), delta -> {});

        List<Message> sent = chat.lastPrompt().getInstructions();
        assertThat(sent).extracting(m -> m.getMessageType().name()).containsExactly("SYSTEM", "USER", "ASSISTANT", "USER");
        assertThat(sent.get(0).getText()).contains("budget: 20000 EUR (TOTAL)").contains("yearly_km: 15000").contains("passengers: 4");
        assertThat(sent.get(1).getText()).isEqualTo("<user_message>\nprimul meu mesaj\n</user_message>");
        assertThat(sent.get(2).getText()).isEqualTo("raspunsul tau");
        // Acoladele din textul utilizatorului trec neschimbate: nu sunt parametri de sablon.
        assertThat(sent.get(3).getText()).isEqualTo("<user_message>\nal doilea {nume}\n</user_message>");
    }
}
