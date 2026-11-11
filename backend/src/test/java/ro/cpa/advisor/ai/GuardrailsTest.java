package ro.cpa.advisor.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class GuardrailsTest {

    private static final List<String> RULES = AdvisorPrompt.protectedTexts();

    private static String feed(PromptLeakGuard guard, String... chunks) {
        StringBuilder out = new StringBuilder();
        for (String chunk : chunks) {
            out.append(guard.accept(chunk));
        }
        out.append(guard.flush());
        return out.toString();
    }

    @Test
    void anOrdinaryAnswerPassesThroughUnchanged() {
        PromptLeakGuard guard = new PromptLeakGuard(RULES);

        String out = feed(guard, "Pentru un buget de 20.000 € ", "te-aș orienta spre un hibrid, ", "fiindcă faci mult oraș și nu poți încărca acasă.");

        assertThat(guard.leaked()).isFalse();
        assertThat(out).isEqualTo("Pentru un buget de 20.000 € te-aș orienta spre un hibrid, fiindcă faci mult oraș și nu poți încărca acasă.");
    }

    @Test
    void aShortQuoteOfTheRulesIsNotALeak() {
        PromptLeakGuard guard = new PromptLeakGuard(RULES);

        feed(guard, "Ma ocup doar de masini: you only help with cars and car-purchase decisions.");

        assertThat(guard.leaked()).isFalse();
    }

    @Test
    void recitingTwelveConsecutiveWordsStopsTheStreamBeforeTheyAreReleased() {
        PromptLeakGuard guard = new PromptLeakGuard(RULES);
        String rules = RULES.get(0).replaceAll("\\s+", " ");
        String leaked = rules.substring(200, 360);

        String released = feed(guard, "Desigur. ", leaked.substring(0, 40), leaked.substring(40, 90), leaked.substring(90));

        assertThat(guard.leaked()).isTrue();
        assertThat(released.replaceAll("\\s+", " ")).doesNotContain(leaked);
        // Nu a ajuns la utilizator nici macar o sindrila completa.
        assertThat(new PromptLeakGuard(RULES).accept(released)).isEmpty();
        assertThat(feed(new PromptLeakGuard(RULES), released)).isEqualTo(released);
    }

    @Test
    void theSummarizerPromptIsProtectedToo() {
        PromptLeakGuard guard = new PromptLeakGuard(RULES);

        feed(guard, AdvisorPrompt.summarizerSystem());

        assertThat(guard.leaked()).isTrue();
    }

    @Test
    void auditFlagsFiguresAndBrandsThatNoSourceBacks() {
        var findings = ResponseAudit.audit(
                "Un BMW costă 45.000 € iar Golf-ul 29.900 EUR, rata e 352,28 €.",
                List.of("{\"priceEur\":29900,\"make\":\"Volkswagen\",\"monthlyPaymentEur\":352.28}", "Caut un Golf"));

        assertThat(findings.ungroundedFigures()).singleElement().asString().contains("45.000");
        assertThat(findings.ungroundedMakes()).containsExactly("bmw");
        assertThat(findings.clean()).isFalse();
    }

    @Test
    void auditAcceptsFiguresFromToolsTheBuyerOrSmallRounding() {
        var findings = ResponseAudit.audit(
                "Prețul este 29.900 €, adică cam 29.950 EUR cu rotunjire, iar bugetul tău de 20000 lei e depășit.",
                List.of("{\"priceEur\":29900}", "Bugetul meu este 20000 lei"));

        assertThat(findings.clean()).isTrue();
    }

    @Test
    void numberFormatsAreParsedTheRomanianAndTheEnglishWay() {
        assertThat(ResponseAudit.parse("29.900")).isEqualTo(29900.0);
        assertThat(ResponseAudit.parse("1.909,44")).isEqualTo(1909.44);
        assertThat(ResponseAudit.parse("352.28")).isEqualTo(352.28);
        assertThat(ResponseAudit.parse("1,5")).isEqualTo(1.5);
        assertThat(ResponseAudit.parse("20000")).isEqualTo(20000.0);
    }
}
