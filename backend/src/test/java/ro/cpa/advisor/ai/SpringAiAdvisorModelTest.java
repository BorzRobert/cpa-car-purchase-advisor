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
import ro.cpa.advisor.domain.AdvisorEvent;
import ro.cpa.advisor.domain.AdvisorUnavailableException;
import ro.cpa.advisor.domain.BudgetKind;
import ro.cpa.advisor.domain.BuyerProfile;
import ro.cpa.advisor.domain.EnergyPrices;
import ro.cpa.advisor.domain.MessageRole;
import ro.cpa.advisor.domain.ProfilePatch;
import ro.cpa.advisor.domain.StreamClosedException;
import ro.cpa.advisor.domain.VehicleCard;
import ro.cpa.advisor.domain.VehicleFilter;

/** Drumul real prin ChatClient (prompt, streaming, consum), cu modelul inregistrat in loc de OpenAI. */
class SpringAiAdvisorModelTest {

    private final RecordedChatModel chat = new RecordedChatModel();
    private final AdvisorToolbox toolbox = new AdvisorToolbox() {
        @Override
        public List<VehicleCard> searchVehicles(VehicleFilter filter) {
            return List.of();
        }

        @Override
        public List<VehicleCard> variants(List<String> variantIds) {
            return List.of();
        }

        @Override
        public ProfileChange updateProfile(UUID sessionId, ProfilePatch patch) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addToShortlist(UUID sessionId, String variantId, String note) {}

        @Override
        public List<VehicleCard> shortlist(UUID sessionId) {
            return List.of();
        }

        @Override
        public BuyerProfile profile(UUID sessionId) {
            return BuyerProfile.empty();
        }

        @Override
        public EnergyPrices energyPrices() {
            throw new UnsupportedOperationException();
        }
    };
    private final SpringAiAdvisorModel advisor = new SpringAiAdvisorModel(
            ChatClient.builder(chat),
            new AdvisorTools(toolbox, tools.jackson.databind.json.JsonMapper.builder().build()),
            new CpaProperties(new CpaProperties.Llm("test-model", 1000, 20, 10, 8, 2.0, 12.0), null, null, null, null));

    private static AdvisorRequest request(String userMessage) {
        return new AdvisorRequest(UUID.randomUUID(), BuyerProfile.empty(), null, List.of(), userMessage);
    }

    private static List<String> deltas(List<AdvisorEvent> events) {
        return events.stream().filter(AdvisorEvent.Token.class::isInstance).map(e -> ((AdvisorEvent.Token) e).delta()).toList();
    }

    @Test
    void deliversTextInOrderAndReportsTheProviderUsage() {
        chat.enqueue(Reply.of("Ana ", "are ", "mere").withUsage(500, 30));
        List<AdvisorEvent> events = new ArrayList<>();

        AdvisorOutcome outcome = advisor.stream(request("salut"), events::add);

        assertThat(String.join("", deltas(events))).isEqualTo("Ana are mere");
        assertThat(outcome.text()).isEqualTo("Ana are mere");
        assertThat(outcome.inputTokens()).isEqualTo(500);
        assertThat(outcome.outputTokens()).isEqualTo(30);
        assertThat(outcome.estimated()).isFalse();
        assertThat(outcome.aborted()).isFalse();
    }

    @Test
    void anAbortedStreamKeepsThePartialTextAndEstimatesUsage() {
        chat.enqueue(Reply.of("a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 ", "b1 b2 b3 b4 b5 b6 b7 b8 b9 b10 b11 b12 ", "c1 c2 c3 c4 c5 c6 c7 c8 c9 c10 c11 c12").withUsage(500, 30));
        List<AdvisorEvent> events = new ArrayList<>();

        AdvisorOutcome outcome = advisor.stream(request("salut"), event -> {
            events.add(event);
            if (events.size() == 2) {
                throw new StreamClosedException(null);
            }
        });

        assertThat(outcome.aborted()).isTrue();
        assertThat(outcome.text()).isNotEmpty().startsWith("a1 a2");
        // O intrerupere nu e gratuita: tokenii se estimeaza, ca „opreste” sa nu poata ocoli bugetul.
        assertThat(outcome.estimated()).isTrue();
        assertThat(outcome.inputTokens()).isPositive();
        assertThat(outcome.outputTokens()).isPositive();
    }

    @Test
    void aFailureIsReportedWithoutTheProvidersMessage() {
        chat.enqueue(Reply.failing());

        assertThatThrownBy(() -> advisor.stream(request("salut"), event -> {}))
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

        advisor.stream(new AdvisorRequest(UUID.randomUUID(), profile, null, history, "al doilea {nume}"), event -> {});

        List<Message> sent = chat.lastPrompt().getInstructions();
        assertThat(sent).extracting(m -> m.getMessageType().name()).containsExactly("SYSTEM", "USER", "ASSISTANT", "USER");
        assertThat(sent.get(0).getText()).contains("budget: 20000 EUR (TOTAL)").contains("yearly_km: 15000").contains("passengers: 4");
        assertThat(sent.get(1).getText()).isEqualTo("<user_message>\nprimul meu mesaj\n</user_message>");
        assertThat(sent.get(2).getText()).isEqualTo("raspunsul tau");
        // Acoladele din textul utilizatorului trec neschimbate: nu sunt parametri de sablon.
        assertThat(sent.get(3).getText()).isEqualTo("<user_message>\nal doilea {nume}\n</user_message>");
    }
}
