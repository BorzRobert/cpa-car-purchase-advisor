package ro.cpa.advisor.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Normalizarea textului primit de la browser, intr-un singur loc. Orice text liber ajunge candva intr-un prompt
 * (profil, mesaje), deci il curatam la intrare: fara caractere de control sau invizibile (zero-width, suprascriere
 * de directie), cu spatii colapsate. Un text prea lung se refuza explicit; nu se taie in tacere.
 */
public final class TextRules {

    private TextRules() {}

    /** Text pe un singur rand (titluri, elemente de lista). Intoarce `null` daca nu ramane nimic. */
    public static String singleLine(String raw, int maxChars, String field) {
        if (raw == null) {
            return null;
        }
        String cleaned = strip(raw, false).replaceAll("\\s+", " ").trim();
        return finish(cleaned, maxChars, field);
    }

    /** Text pe mai multe randuri (mesaje): pastreaza `\n`, normalizeaza `\r\n`. Intoarce `null` daca nu ramane nimic. */
    public static String multiLine(String raw, int maxChars, String field) {
        if (raw == null) {
            return null;
        }
        String cleaned = strip(raw.replace("\r\n", "\n").replace('\r', '\n'), true).trim();
        return finish(cleaned, maxChars, field);
    }

    /** Lista de texte scurte: curata fiecare element, elimina golurile si dublurile, refuza o lista prea lunga. */
    public static List<String> singleLineList(List<String> raw, int maxItems, int maxChars, String field) {
        if (raw == null) {
            return List.of();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String item : raw) {
            String cleaned = singleLine(item, maxChars, field);
            if (cleaned != null) {
                unique.add(cleaned);
            }
        }
        if (unique.size() > maxItems) {
            throw new InvalidInputException(field + " accepts at most " + maxItems + " items.");
        }
        return List.copyOf(new ArrayList<>(unique));
    }

    private static String strip(String text, boolean keepNewlines) {
        StringBuilder out = new StringBuilder(text.length());
        text.codePoints().forEach(cp -> {
            boolean newline = cp == '\n' || cp == '\t';
            if (newline && keepNewlines) {
                out.appendCodePoint(cp);
            } else if (Character.isISOControl(cp) || Character.getType(cp) == Character.FORMAT) {
                out.append(' ');
            } else {
                out.appendCodePoint(cp);
            }
        });
        return out.toString();
    }

    private static String finish(String cleaned, int maxChars, String field) {
        if (cleaned.isEmpty()) {
            return null;
        }
        if (cleaned.codePointCount(0, cleaned.length()) > maxChars) {
            throw new InvalidInputException(field + " must have at most " + maxChars + " characters.");
        }
        return cleaned;
    }
}
