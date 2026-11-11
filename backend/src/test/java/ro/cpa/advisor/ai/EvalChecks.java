package ro.cpa.advisor.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.function.Supplier;
import ro.cpa.advisor.ai.EvalHarness.EvalCheck;
import ro.cpa.advisor.ai.EvalHarness.EvalRun;
import ro.cpa.advisor.ai.EvalHarness.TurnResult;
import ro.cpa.advisor.api.SseTestSupport.Sse;

/** Verificarile scenariilor de evaluare: aceleasi pentru modelul inregistrat si pentru cel real. */
final class EvalChecks {

    private EvalChecks() {}

    private static EvalCheck check(String name, java.util.function.Consumer<EvalRun> body) {
        return new EvalCheck() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public void verify(EvalRun run) {
                body.accept(run);
            }
        };
    }

    /** Verificarea se aplica doar cand raspunsurile sunt inregistrate (depinde de ce a pregatit testul, nu de un model real). */
    static EvalCheck recordedOnly(EvalCheck inner) {
        return check(inner.name() + " (recorded only)", run -> {
            if (run.recorded) {
                inner.verify(run);
            }
        });
    }

    static EvalCheck usesTools(String... names) {
        return check("uses tools " + Arrays.toString(names), run -> assertThat(run.allTools()).contains(names));
    }

    static EvalCheck usesNoTools() {
        return check("uses no tools", run -> assertThat(run.allTools()).isEmpty());
    }

    static EvalCheck doesNotUseTool(String name) {
        return check("does not use " + name, run -> assertThat(run.allTools()).doesNotContain(name));
    }

    static EvalCheck emits(String eventName) {
        return check("emits " + eventName, run -> assertThat(run.turns.stream().flatMap(t -> t.events().stream()).map(Sse::name)).contains(eventName));
    }

    static EvalCheck endsWithDone() {
        return check("ends with done", run -> assertThat(run.turns).allSatisfy(t -> assertThat(t.events().get(t.events().size() - 1).name()).isEqualTo("done")));
    }

    /** Toate sumele din raspuns se leaga de rezultatele uneltelor sau de ce a spus cumparatorul (auditul de ancorare). */
    static EvalCheck groundedFigures() {
        return check("every figure comes from a tool or the buyer", run -> assertThat(run.turns).allSatisfy(t -> assertThat(findings(t, "ungroundedFigures")).isEmpty()));
    }

    /** Nicio marca de masina in raspuns care sa nu fi fost numita de cumparator sau intoarsa de o unealta. */
    static EvalCheck groundedMakes() {
        return check("no car brand from memory", run -> assertThat(run.turns).allSatisfy(t -> assertThat(findings(t, "ungroundedMakes")).isEmpty()));
    }

    /** Raspunsul spune cifra calculata de codul Java (toleranta 1%), nu o cifra apropiata „din cap”. */
    static EvalCheck mentionsAmount(String what, Supplier<? extends Number> expected) {
        return check("states " + what, run -> {
            double value = expected.get().doubleValue();
            assertThat(ResponseAudit.moneyFigures(run.last().answer()))
                    .as("answer should state %s = %s", what, value)
                    .anyMatch(figure -> ResponseAudit.sameAmount(figure, value));
        });
    }

    static EvalCheck answerContainsAny(String... fragments) {
        return check("answer contains one of " + Arrays.toString(fragments), run -> {
            String answer = run.last().answer().toLowerCase(Locale.ROOT);
            assertThat(Arrays.stream(fragments).anyMatch(f -> answer.contains(f.toLowerCase(Locale.ROOT)))).as("answer: %s", run.last().answer()).isTrue();
        });
    }

    static EvalCheck answerContainsNone(String... fragments) {
        return check("answer contains none of " + Arrays.toString(fragments), run -> {
            String answer = run.last().answer().toLowerCase(Locale.ROOT);
            assertThat(Arrays.stream(fragments).filter(f -> answer.contains(f.toLowerCase(Locale.ROOT))).toList()).isEmpty();
        });
    }

    static EvalCheck asksAtLeastOneAndAtMostTwoQuestions() {
        return check("asks one or two questions", run -> {
            long questions = run.last().answer().chars().filter(c -> c == '?').count();
            assertThat(questions).isBetween(1L, 2L);
        });
    }

    /** Raspunsul nu recita instructiunile interne (aceeasi regula de 12 cuvinte consecutive ca gardul). */
    static EvalCheck noPromptLeak() {
        return check("does not reveal the system prompt", run -> {
            PromptLeakGuard guard = new PromptLeakGuard(AdvisorPrompt.protectedTexts());
            guard.accept(run.last().answer());
            assertThat(guard.leaked()).isFalse();
        });
    }

    static EvalCheck guardrailTriggered() {
        return check("prompt-leak guardrail triggered", run -> assertThat(JsonPath.<String>read(run.last().metadataJson(), "$.guardrail")).isEqualTo("prompt-leak"));
    }

    static EvalCheck guardrailNotTriggered() {
        return check("no guardrail triggered", run -> assertThat(run.turns).allSatisfy(t -> assertThat(JsonPath.<String>read(t.metadataJson(), "$.guardrail")).isNull()));
    }

    /** O valoare din starea finala a profilului (`GET /profile`). */
    static EvalCheck profileHas(String jsonPath, Object expected) {
        return check("profile " + jsonPath + " = " + expected, run -> assertThat(JsonPath.<Object>read(run.get("/api/v1/profile"), jsonPath)).isEqualTo(expected));
    }

    static EvalCheck profileLacks(String jsonPath) {
        return check("profile has no " + jsonPath, run -> assertThat(JsonPath.<Object>read(run.get("/api/v1/profile"), jsonPath)).isNull());
    }

    static EvalCheck shortlistContains(String variantId) {
        return check("shortlist contains " + variantId, run -> assertThat(JsonPath.<List<String>>read(run.get("/api/v1/shortlist"), "$[*].vehicle.variants[0].id")).contains(variantId));
    }

    /** O proprietate dintr-un eveniment SSE (primul cu numele dat) satisface o conditie. */
    static EvalCheck eventData(String eventName, String jsonPath, Predicate<Object> condition) {
        return check("event " + eventName + " " + jsonPath, run -> {
            Sse event = run.turns.stream().flatMap(t -> t.events().stream()).filter(e -> e.name().equals(eventName)).findFirst().orElseThrow();
            assertThat(condition.test(JsonPath.read(event.data(), jsonPath))).as("%s %s = %s", eventName, jsonPath, JsonPath.read(event.data(), jsonPath)).isTrue();
        });
    }

    /** Dupa ultima replica, conversatia are un rezumat salvat (rulat doar cu fereastra mica din configuratia de test). */
    static EvalCheck conversationWasSummarized() {
        return check("conversation was summarized", run -> assertThat(run.conversationSummary()).isNotBlank());
    }

    /** Ultima cerere de RASPUNS (nu cea de rezumare) contine rezumatul delimitat si nu mai contine prima replica a cumparatorului. */
    static EvalCheck lastPromptUsesSummaryInsteadOfOldMessages(String firstUserMessage) {
        return recordedOnly(check("last prompt carries the summary instead of old messages", run -> {
            var answerPrompts = run.model.prompts().stream().filter(p -> p.getInstructions().get(0).getText().startsWith("You are CPA")).toList();
            var messages = answerPrompts.get(answerPrompts.size() - 1).getInstructions();
            assertThat(messages.get(0).getText()).contains("<conversation_summary>");
            assertThat(messages.stream().skip(1).map(m -> m.getText())).noneMatch(text -> text != null && text.contains(firstUserMessage));
        }));
    }
    private static List<String> findings(TurnResult turn, String field) {
        return JsonPath.read(turn.metadataJson(), "$." + field);
    }
}
