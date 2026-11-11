package ro.cpa.advisor.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Auditul de ancorare al unui raspuns: sume de bani si marci de masini pe care raspunsul le spune, dar care nu apar nici
 * in rezultatele uneltelor, nici in ce a scris cumparatorul. Regula „preturile vin doar din unelte” (plan §4.1) este in
 * prompt, dar un prompt nu este o garantie; auditul o MASOARA, pe fiecare raspuns (jurnal + metadate) si in evaluare.
 *
 * <p>Nu blocheaza nimic, intentionat: un raspuns poate calcula legitim (o diferenta de doua preturi, o rotunjire) si ar fi
 * respins gresit. Sumele se compara cu o toleranta de 1% (min. 0,5 EUR) fata de orice cifra din materialul de ancorare.
 */
public final class ResponseAudit {

    private static final double RELATIVE_TOLERANCE = 0.01;
    private static final double MIN_TOLERANCE = 0.5;

    // Suma de bani: numar urmat de moneda (29.900 EUR, 1.909,44 €, 500 lei) sau precedat de „€”.
    private static final Pattern MONEY = Pattern.compile(
            "(?<![\\p{L}\\p{N}.,])(?:€\\s?(\\d[\\d.,\\s\\u00a0\\u202f]*\\d|\\d)|(\\d[\\d.,\\s\\u00a0\\u202f]*\\d|\\d)\\s?(?:€|EUR\\b|euro\\b|eur\\b|lei\\b|RON\\b))",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER = Pattern.compile("\\d[\\d.,]*\\d|\\d");

    // Marci cunoscute pe piata din Romania/UE: daca raspunsul numeste una pe care nici cumparatorul, nici o unealta nu a
    // numit-o, modelul a adus o masina „din memorie”.
    private static final List<String> MAKES = List.of(
            "dacia", "volkswagen", "skoda", "škoda", "toyota", "renault", "peugeot", "hyundai", "tesla", "ford", "bmw", "audi",
            "mercedes", "opel", "fiat", "seat", "cupra", "kia", "honda", "mazda", "nissan", "citroen", "citroën", "volvo", "suzuki",
            "mitsubishi", "jeep", "porsche", "lexus", "subaru", "mini", "alfa romeo", "land rover", "jaguar", "byd", "mg", "polestar");

    private ResponseAudit() {}

    /** Ce nu se leaga de nimic: sumele (ca text, cum apar in raspuns) si marcile. */
    public record Findings(List<String> ungroundedFigures, List<String> ungroundedMakes) {

        public boolean clean() {
            return ungroundedFigures.isEmpty() && ungroundedMakes.isEmpty();
        }
    }

    /**
     * @param answer raspunsul consilierului
     * @param groundingTexts tot ce are voie sa fie sursa: rezultatele uneltelor, mesajele cumparatorului, istoricul si profilul
     */
    public static Findings audit(String answer, List<String> groundingTexts) {
        String grounding = String.join("\n", groundingTexts).toLowerCase(Locale.ROOT);
        List<Double> groundNumbers = new ArrayList<>();
        Matcher numbers = NUMBER.matcher(grounding);
        while (numbers.find()) {
            double value = parse(numbers.group());
            if (!Double.isNaN(value)) {
                groundNumbers.add(value);
            }
        }

        List<String> figures = new ArrayList<>();
        Matcher money = MONEY.matcher(answer);
        while (money.find()) {
            String raw = money.group(1) != null ? money.group(1) : money.group(2);
            double value = parse(raw.replaceAll("[\\s\\u00a0\\u202f]", ""));
            if (!Double.isNaN(value) && groundNumbers.stream().noneMatch(ground -> close(value, ground))) {
                figures.add(money.group().strip());
            }
        }

        List<String> makes = new ArrayList<>();
        String lowerAnswer = answer.toLowerCase(Locale.ROOT);
        for (String make : MAKES) {
            if (Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(make) + "(?![\\p{L}\\p{N}])").matcher(lowerAnswer).find()
                    && !grounding.contains(make)) {
                makes.add(make);
            }
        }
        return new Findings(List.copyOf(figures), List.copyOf(makes));
    }

    private static boolean close(double value, double ground) {
        return Math.abs(value - ground) <= Math.max(MIN_TOLERANCE, Math.abs(ground) * RELATIVE_TOLERANCE);
    }

    /** Sumele de bani pe care le spune un text, ca numere (pentru a verifica ca un raspuns spune cifra calculata). */
    public static List<Double> moneyFigures(String text) {
        List<Double> values = new ArrayList<>();
        Matcher money = MONEY.matcher(text);
        while (money.find()) {
            String raw = money.group(1) != null ? money.group(1) : money.group(2);
            double value = parse(raw.replaceAll("[\\s\\u00a0\\u202f]", ""));
            if (!Double.isNaN(value)) {
                values.add(value);
            }
        }
        return values;
    }

    /** Daca `value` se potriveste, cu aceeasi toleranta a auditului (1%, minim 0,5 EUR), cu `expected`. */
    public static boolean sameAmount(double value, double expected) {
        return close(value, expected);
    }

    // „29.900” = 29900, „1.909,44” = 1909,44, „352.28” = 352,28, „1,5” = 1,5: ultimul separator urmat de 1-2 cifre este
    // zecimal; un separator urmat de exact 3 cifre (si nimic dupa) este de mii.
    static double parse(String raw) {
        String text = raw.replaceAll("[^\\d.,]", "");
        if (text.isEmpty()) {
            return Double.NaN;
        }
        int lastDot = text.lastIndexOf('.');
        int lastComma = text.lastIndexOf(',');
        int lastSeparator = Math.max(lastDot, lastComma);
        if (lastSeparator < 0) {
            return Double.parseDouble(text);
        }
        int digitsAfter = text.length() - lastSeparator - 1;
        boolean both = lastDot >= 0 && lastComma >= 0;
        boolean decimal = both || digitsAfter != 3;
        if (decimal) {
            String integer = text.substring(0, lastSeparator).replaceAll("[.,]", "");
            String fraction = text.substring(lastSeparator + 1);
            return fraction.isEmpty() ? Double.parseDouble(integer) : Double.parseDouble(integer + "." + fraction);
        }
        return Double.parseDouble(text.replaceAll("[.,]", ""));
    }
}
