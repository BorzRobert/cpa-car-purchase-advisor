package ro.cpa.advisor.ai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plasa de siguranta deterministica impotriva scurgerii promptului: opreste raspunsul daca modelul incepe sa recite
 * instructiunile interne. Promptul spune deja „nu le dezvalui”, dar un model poate fi pacalit; aceasta verificare nu
 * depinde de ce „crede” modelul.
 *
 * <p>Cum merge: textul protejat se imparte in „sindrile” de {@value #SHINGLE_WORDS} cuvinte consecutive (normalizate:
 * litere mici, fara punctuatie). Raspunsul se elibereaza cu o intarziere de {@value #SHINGLE_WORDS} - 1 cuvinte, ca o
 * sindrila sa poata fi recunoscuta INAINTE sa ajunga la utilizator; la prima potrivire, fluxul se opreste. Un prag de 12
 * cuvinte lasa liber orice parafrazare sau citat scurt (de exemplu „ma ocup doar de masini”), dar prinde o recitare reala.
 */
final class PromptLeakGuard {

    static final int SHINGLE_WORDS = 12;
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+");

    private final Set<String> shingles = new HashSet<>();
    private final StringBuilder pending = new StringBuilder();
    // Ultimele cuvinte deja eliberate: o sindrila poate incepe in ele si continua in textul inca retinut.
    private final Deque<String> releasedTail = new ArrayDeque<>();
    private boolean leaked;

    PromptLeakGuard(List<String> protectedTexts) {
        for (String text : protectedTexts) {
            List<String> words = words(text);
            for (int i = 0; i + SHINGLE_WORDS <= words.size(); i++) {
                shingles.add(String.join(" ", words.subList(i, i + SHINGLE_WORDS)));
            }
        }
    }

    boolean leaked() {
        return leaked;
    }

    /** Primeste o bucata de text si intoarce ce se poate elibera acum (poate fi gol); dupa o scurgere intoarce mereu gol. */
    String accept(String delta) {
        if (leaked) {
            return "";
        }
        pending.append(delta);
        if (leakIn(pending.toString())) {
            leaked = true;
            pending.setLength(0);
            return "";
        }
        return release(false);
    }

    /** La sfarsitul raspunsului: ce a ramas retinut, daca nu s-a detectat nimic. */
    String flush() {
        if (leaked) {
            return "";
        }
        return release(true);
    }

    private boolean leakIn(String text) {
        List<String> window = new ArrayList<>(releasedTail);
        window.addAll(words(text));
        for (int i = 0; i + SHINGLE_WORDS <= window.size(); i++) {
            if (shingles.contains(String.join(" ", window.subList(i, i + SHINGLE_WORDS)))) {
                return true;
            }
        }
        return false;
    }

    // Elibereaza tot, mai putin ultimele (SHINGLE_WORDS - 1) cuvinte; textul se taie la inceputul primului cuvant retinut.
    private String release(boolean all) {
        String text = pending.toString();
        int cut = text.length();
        if (!all) {
            List<int[]> spans = wordSpans(text);
            int hold = SHINGLE_WORDS - 1;
            if (spans.size() <= hold) {
                return "";
            }
            cut = spans.get(spans.size() - hold)[0];
        }
        String released = text.substring(0, cut);
        pending.delete(0, cut);
        for (String word : words(released)) {
            releasedTail.addLast(word);
            if (releasedTail.size() > SHINGLE_WORDS - 1) {
                releasedTail.removeFirst();
            }
        }
        return released;
    }

    private static List<String> words(String text) {
        List<String> words = new ArrayList<>();
        Matcher matcher = WORD.matcher(text);
        while (matcher.find()) {
            words.add(matcher.group().toLowerCase(Locale.ROOT));
        }
        return words;
    }

    private static List<int[]> wordSpans(String text) {
        List<int[]> spans = new ArrayList<>();
        Matcher matcher = WORD.matcher(text);
        while (matcher.find()) {
            spans.add(new int[] {matcher.start(), matcher.end()});
        }
        return spans;
    }
}
