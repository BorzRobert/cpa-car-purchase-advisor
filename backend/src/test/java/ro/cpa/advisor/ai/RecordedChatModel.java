package ro.cpa.advisor.ai;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * Modelul „inregistrat” din CI: raspunde cu bucati de text pregatite de test, fara retea si fara cheie. Inlocuieste
 * modelul real la nivelul `ChatModel`, deci tot restul drumului (ChatClient, prompt, streaming, contabilizare) ruleaza
 * exact ca in productie. Fiecare cerere primita este pastrata, ca testele sa poata verifica ce a vazut modelul.
 */
public class RecordedChatModel implements ChatModel {

    /** Un raspuns inregistrat: bucatile de text, consumul raportat si, optional, o pauza intre bucati sau un esec. */
    public record Reply(List<String> chunks, Integer inputTokens, Integer outputTokens, Duration delay, boolean fails) {

        public static Reply of(String... chunks) {
            return new Reply(List.of(chunks), 120, 20, Duration.ZERO, false);
        }

        public static Reply withoutUsage(String... chunks) {
            return new Reply(List.of(chunks), null, null, Duration.ZERO, false);
        }

        public static Reply failing() {
            return new Reply(List.of(), null, null, Duration.ZERO, true);
        }

        public Reply withUsage(int input, int output) {
            return new Reply(chunks, input, output, delay, fails);
        }

        public Reply slow(Duration delayPerChunk) {
            return new Reply(chunks, inputTokens, outputTokens, delayPerChunk, fails);
        }
    }

    private static final Reply DEFAULT_REPLY = Reply.of("Salut! ", "Spune-mi ce buget ai.");

    private final ConcurrentLinkedQueue<Reply> queue = new ConcurrentLinkedQueue<>();
    private final List<Prompt> prompts = new CopyOnWriteArrayList<>();

    public void enqueue(Reply reply) {
        queue.add(reply);
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
