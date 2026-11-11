package ro.cpa.advisor.ai;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import reactor.core.publisher.Flux;

/**
 * Modelul „inregistrat” din CI: raspunde cu ce a pregatit testul, fara retea si fara cheie. Inlocuieste modelul real la
 * nivelul `ChatModel`, deci tot restul drumului (ChatClient, bucla de unelte `ToolCallingAdvisor`, prompt, streaming,
 * contabilizare) ruleaza exact ca in productie. Raspunsurile se consuma pe rand: un raspuns cu apeluri de unealta este
 * urmat, la urmatorul apel al modelului, de cel de dupa executarea uneltelor — ca la un model real. Fiecare cerere
 * primita este pastrata, ca testele sa poata verifica ce a vazut modelul.
 */
public class RecordedChatModel implements ChatModel {

    public record ToolCall(String name, String argumentsJson) {}

    /** Un raspuns inregistrat: text SAU apeluri de unealta, consumul raportat si, optional, o pauza intre bucati sau un esec. */
    public record Reply(List<String> chunks, List<ToolCall> toolCalls, Integer inputTokens, Integer outputTokens, Duration delay, boolean fails) {

        public static Reply of(String... chunks) {
            return new Reply(List.of(chunks), List.of(), 120, 20, Duration.ZERO, false);
        }

        public static Reply withoutUsage(String... chunks) {
            return new Reply(List.of(chunks), List.of(), null, null, Duration.ZERO, false);
        }

        /** Modelul cere sa ruleze o unealta (argumentele sunt JSON, exact cum le-ar trimite OpenAI). */
        public static Reply calling(String toolName, String argumentsJson) {
            return new Reply(List.of(), List.of(new ToolCall(toolName, argumentsJson)), 100, 10, Duration.ZERO, false);
        }

        public static Reply failing() {
            return new Reply(List.of(), List.of(), null, null, Duration.ZERO, true);
        }

        public Reply withUsage(int input, int output) {
            return new Reply(chunks, toolCalls, input, output, delay, fails);
        }

        public Reply slow(Duration delayPerChunk) {
            return new Reply(chunks, toolCalls, inputTokens, outputTokens, delayPerChunk, fails);
        }
    }

    private static final Reply DEFAULT_REPLY = Reply.of("Salut! ", "Spune-mi ce buget ai.");

    private final ConcurrentLinkedQueue<Reply> queue = new ConcurrentLinkedQueue<>();
    private final List<Prompt> prompts = new CopyOnWriteArrayList<>();
    private final AtomicInteger callIds = new AtomicInteger();

    public void enqueue(Reply... replies) {
        for (Reply reply : replies) {
            queue.add(reply);
        }
    }

    public List<Prompt> prompts() {
        return prompts;
    }

    public Prompt lastPrompt() {
        return prompts.get(prompts.size() - 1);
    }

    public void reset() {
        queue.clear();
        prompts.clear();
    }

    @Override
    public ChatOptions getOptions() {
        // Ca la modelul real: optiunile implicite accepta unelte; fara asta ChatClient le-ar arunca din cerere.
        return OpenAiChatOptions.builder().model("recorded-model").build();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        Reply reply = next(prompt);
        if (reply.fails()) {
            throw new IllegalStateException("recorded failure");
        }
        return response(String.join("", reply.chunks()), reply);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        Reply reply = next(prompt);
        if (reply.fails()) {
            return Flux.error(new IllegalStateException("recorded failure with a secret-looking message sk-test-123"));
        }
        if (!reply.toolCalls().isEmpty()) {
            return Flux.just(toolCallResponse(reply));
        }
        Flux<ChatResponse> text = Flux.fromIterable(reply.chunks()).map(chunk -> response(chunk, null));
        if (!reply.delay().isZero()) {
            text = text.delayElements(reply.delay());
        }
        // OpenAI trimite consumul intr-o ultima bucata fara text, doar daca s-a cerut `stream_options.include_usage`.
        return reply.inputTokens() == null ? text : text.concatWith(Flux.just(usageOnly(reply)));
    }

    private Reply next(Prompt prompt) {
        prompts.add(prompt);
        Reply reply = queue.poll();
        return reply == null ? DEFAULT_REPLY : reply;
    }

    private ChatResponse toolCallResponse(Reply reply) {
        List<AssistantMessage.ToolCall> calls = reply.toolCalls().stream()
                .map(call -> new AssistantMessage.ToolCall("call_" + callIds.incrementAndGet(), "function", call.name(), call.argumentsJson()))
                .toList();
        AssistantMessage message = AssistantMessage.builder().content("").toolCalls(calls).build();
        Generation generation = new Generation(message, ChatGenerationMetadata.builder().finishReason("tool_calls").build());
        return new ChatResponse(List.of(generation), metadata(reply));
    }

    private static ChatResponse response(String text, Reply reply) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))), metadata(reply));
    }

    private static ChatResponse usageOnly(Reply reply) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(""))), metadata(reply));
    }

    private static ChatResponseMetadata metadata(Reply reply) {
        ChatResponseMetadata.Builder builder = ChatResponseMetadata.builder();
        if (reply != null && reply.inputTokens() != null) {
            builder.usage(new DefaultUsage(reply.inputTokens(), reply.outputTokens()));
        }
        return builder.build();
    }
}
