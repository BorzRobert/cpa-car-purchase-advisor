package ro.cpa.advisor.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import ro.cpa.advisor.domain.ScoredVehicle.CriterionScore;

/**
 * Motorul de scoring: clasifica variante dupa un profil, cu scor pe criterii care se poate explica si reproduce. Cod pur:
 * aceleasi intrari dau acelasi clasament, fara model si fara baza de date. Nimic nu se ghiceste: ce lipseste din profil
 * face un criteriu „neaplicabil” (iese din medie) sau foloseste o valoare implicita ANUNTATA in `assumptions`.
 *
 * <p>Limite asumate (de spus cumparatorului): „fiabilitatea” nu are o sursa numerica in catalog, asa ca foloseste
 * valoarea reziduala la 5 ani drept aproximare; must-have-urile si deal-breaker-ele din profil sunt text liber si NU se
 * potrivesc automat (le judeca agentul).
 */
public final class VehicleScorer {

    public static final int DEFAULT_YEARS = 5;
    public static final int DEFAULT_KM_PER_YEAR = 15_000;

    // Costul curent pe km (energie + asigurare + taxa + mentenanta) la care scorul e 100, respectiv 0.
    private static final double RUNNING_COST_BEST_EUR_PER_KM = 0.08;
    private static final double RUNNING_COST_WORST_EUR_PER_KM = 0.25;
    // Volumul portbagajului (litri) la care scorul de spatiu e 0, respectiv 100.
    private static final int BOOT_WORST_LITRES = 250;
    private static final int BOOT_BEST_LITRES = 550;
    // Valoarea reziduala la 5 ani (%) la care scorul de „fiabilitate” e 0, respectiv 100.
    private static final int RESIDUAL_WORST_PERCENT = 35;
    private static final int RESIDUAL_BEST_PERCENT = 60;

    private VehicleScorer() {}

    public record Ranking(int years, int kmPerYear, List<String> assumptions, List<ScoredVehicle> ranked) {}

    public static Ranking rank(List<VehicleCard> candidates, BuyerProfile profile, EnergyPrices prices, int years) {
        List<String> assumptions = new ArrayList<>();
        int km = kmPerYear(profile, assumptions);
        assumptions.add("Scores are 0-100 per criterion with fixed weights (price fit 25, running cost 20, space 15, safety 15, reliability 10, usage fit 15); "
                + "a criterion without enough data is left out of the average, not guessed.");
        assumptions.add("Reliability has no numeric source in the catalogue: the 5-year residual value is used as a proxy. Must-haves and deal-breakers are free text and are NOT matched automatically.");
        assumptions.add("TCO horizon " + years + " years, cash purchase (financing is not included in the score).");

        List<ScoredVehicle> scored = new ArrayList<>();
        for (VehicleCard car : candidates) {
            scored.add(score(car, profile, prices, years, km));
        }
        scored.sort(Comparator.comparingInt(ScoredVehicle::totalScore).reversed().thenComparing(s -> s.card().variantId()));
        return new Ranking(years, km, List.copyOf(assumptions), List.copyOf(scored));
    }

    private static int kmPerYear(BuyerProfile profile, List<String> assumptions) {
        Integer stated = profile.yearlyKm();
        if (stated == null) {
            assumptions.add("Yearly distance not given: " + DEFAULT_KM_PER_YEAR + " km/year assumed.");
            return DEFAULT_KM_PER_YEAR;
        }
        if (stated < TcoCalculator.MIN_KM_PER_YEAR) {
            assumptions.add("Yearly distance " + stated + " km is below the calculator's minimum; " + TcoCalculator.MIN_KM_PER_YEAR + " km/year used.");
            return TcoCalculator.MIN_KM_PER_YEAR;
        }
        return stated;
    }

    private static ScoredVehicle score(VehicleCard car, BuyerProfile profile, EnergyPrices prices, int years, int km) {
        TcoResult tco = TcoCalculator.calculate(car, years, km, profile.homeCharging(), prices, null, null, null);
        List<CriterionScore> breakdown = List.of(
                priceFit(car, profile, tco, years),
                runningCost(tco, km),
                space(car, profile),
                safety(car),
                reliability(car),
                usageFit(car, profile));

        int weighted = 0;
        int weights = 0;
        for (CriterionScore criterion : breakdown) {
            if (criterion.applicable()) {
                weighted += criterion.score() * criterion.criterion().weight();
                weights += criterion.criterion().weight();
            }
        }
        int total = weights == 0 ? 0 : (int) Math.round(weighted / (double) weights);
        return new ScoredVehicle(car, total, breakdown, tco);
    }

    // ---------- criterii ----------

    private static CriterionScore priceFit(VehicleCard car, BuyerProfile profile, TcoResult tco, int years) {
        BigDecimal budget = profile.budgetAmount();
        if (budget == null || budget.signum() == 0) {
            return new CriterionScore(Criterion.PRICE_FIT, false, 0, "Nu ai spus încă bugetul.");
        }
        boolean monthly = profile.budgetKind() == BudgetKind.MONTHLY;
        BigDecimal compared = monthly ? tco.costPerMonthEur() : BigDecimal.valueOf(car.priceEur());
        double ratio = compared.doubleValue() / budget.doubleValue();
        int score = priceScore(ratio);
        String detail = monthly
                ? "Costul total pe lună (" + years + " ani, include depreciere) este " + compared.toPlainString() + " € față de bugetul de " + budget.toPlainString() + " €/lună."
                : "Prețul de listă este " + car.priceEur() + " € față de bugetul de " + budget.toPlainString() + " €.";
        return new CriterionScore(Criterion.PRICE_FIT, true, score, detail);
    }

    // Sub 85% din buget e „confortabil” (100); pana la buget scade usor; peste buget scade repede si ajunge la 0 la +15%.
    static int priceScore(double ratio) {
        if (ratio <= 0.85) {
            return 100;
        }
        if (ratio <= 1.0) {
            return (int) Math.round(100 - (ratio - 0.85) / 0.15 * 30);
        }
        if (ratio <= 1.15) {
            return (int) Math.round(70 - (ratio - 1.0) / 0.15 * 70);
        }
        return 0;
    }

    private static CriterionScore runningCost(TcoResult tco, int km) {
        double perKm = tco.annualRunningCostEur().doubleValue() / km;
        int score = scale(perKm, RUNNING_COST_WORST_EUR_PER_KM, RUNNING_COST_BEST_EUR_PER_KM);
        return new CriterionScore(
                Criterion.RUNNING_COST,
                true,
                score,
                "Costuri curente (energie, asigurare, taxă, mentenanță) de aproximativ " + tco.annualRunningCostEur().toPlainString() + " €/an, adică "
                        + String.format(java.util.Locale.ROOT, "%.2f", perKm) + " €/km la " + km + " km/an.");
    }

    private static CriterionScore space(VehicleCard car, BuyerProfile profile) {
        if (profile.passengers() != null && car.seats() < profile.passengers()) {
            return new CriterionScore(Criterion.SPACE, true, 0, "Are " + car.seats() + " locuri, iar tu ai nevoie de " + profile.passengers() + ".");
        }
        int score = scale(car.bootLitres(), BOOT_WORST_LITRES, BOOT_BEST_LITRES);
        String seats = profile.passengers() == null ? car.seats() + " locuri" : car.seats() + " locuri (ai nevoie de " + profile.passengers() + ")";
        return new CriterionScore(Criterion.SPACE, true, score, seats + " și portbagaj de " + car.bootLitres() + " l.");
    }

    private static CriterionScore safety(VehicleCard car) {
        Integer stars = car.euroNcapStars();
        if (stars == null) {
            return new CriterionScore(Criterion.SAFETY, false, 0, "Nu avem o evaluare Euro NCAP verificată pentru acest model; nu o presupunem.");
        }
        return new CriterionScore(Criterion.SAFETY, true, Math.min(100, stars * 20), "Euro NCAP: " + stars + " din 5 stele.");
    }

    private static CriterionScore reliability(VehicleCard car) {
        int score = scale(car.residualValuePercentAfter5Years(), RESIDUAL_WORST_PERCENT, RESIDUAL_BEST_PERCENT);
        return new CriterionScore(
                Criterion.RELIABILITY,
                true,
                score,
                "Păstrează " + car.residualValuePercentAfter5Years() + "% din valoare după 5 ani (aproximare pentru cerere și fiabilitate). Notă: " + car.reliabilityNotes());
    }

    private static CriterionScore usageFit(VehicleCard car, BuyerProfile profile) {
        List<Integer> parts = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        Fit powertrain = powertrainFit(car, profile);
        if (powertrain != null) {
            parts.add(powertrain.score());
            notes.add(powertrain.note());
        }
        if (!profile.powertrains().isEmpty()) {
            boolean match = profile.powertrains().contains(car.powertrain());
            parts.add(match ? 100 : 20);
            notes.add(match ? "motorizarea este printre cele dorite" : "motorizarea nu este printre cele dorite");
        }
        if (!profile.bodyTypes().isEmpty()) {
            boolean match = profile.bodyTypes().contains(car.bodyType());
            parts.add(match ? 100 : 20);
            notes.add(match ? "caroseria este printre cele dorite" : "caroseria nu este printre cele dorite");
        }
        if (parts.isEmpty()) {
            return new CriterionScore(
                    Criterion.USAGE_FIT, false, 0, "Nu știm încă cum folosești mașina (oraș/autostradă, kilometri, încărcare acasă, preferințe).");
        }
        int score = (int) Math.round(parts.stream().mapToInt(Integer::intValue).average().orElse(0));
        return new CriterionScore(Criterion.USAGE_FIT, true, score, String.join("; ", notes) + ".");
    }

    private record Fit(int score, String note) {}

    // Potrivirea motorizarii cu felul de utilizare; `null` cand profilul nu spune nimic despre utilizare (doar preferinte).
    private static Fit powertrainFit(VehicleCard car, BuyerProfile profile) {
        Boolean home = profile.homeCharging();
        Integer city = profile.cityPercent();
        Integer km = profile.yearlyKm();
        boolean anyUsage = home != null || city != null || km != null;
        return switch (car.powertrain()) {
            case ELECTRIC -> home == null
                    ? unknown(anyUsage, 50, "electrică, încărcarea acasă nu e cunoscută")
                    : home ? new Fit(90, "electrică, cu încărcare acasă") : new Fit(25, "electrică, fără încărcare acasă (depinzi de stații publice)");
            case PLUG_IN_HYBRID -> home == null
                    ? unknown(anyUsage, 50, "plug-in hybrid, încărcarea acasă nu e cunoscută")
                    : home ? new Fit(80, "plug-in hybrid, cu încărcare acasă") : new Fit(30, "plug-in hybrid, fără încărcare acasă (își pierde avantajul)");
            case HYBRID -> city == null
                    ? unknown(anyUsage, 70, "hibrid, potrivire generală")
                    : city >= 60 ? new Fit(90, "hibrid, potrivit pentru trafic urban")
                            : city >= 30 ? new Fit(75, "hibrid, util în mod mixt") : new Fit(60, "hibrid, avantaj mai mic pe autostradă");
            case DIESEL -> km == null || city == null
                    ? unknown(anyUsage, 50, "diesel, kilometrajul și tipul de drum nu sunt cunoscute")
                    : km >= 25_000 && city <= 40 ? new Fit(90, "diesel, potrivit pentru kilometraj mare pe drumuri deschise")
                            : km >= 15_000 && city <= 60 ? new Fit(65, "diesel, acceptabil pentru kilometraj mediu")
                                    : new Fit(30, "diesel, mai puțin potrivit pentru oraș și kilometraj mic");
            case PETROL -> km != null && km >= 30_000
                    ? new Fit(45, "benzină, mai puțin economică la kilometraj mare")
                    : unknown(anyUsage, 60, "benzină, alegere neutră");
        };
    }

    private static Fit unknown(boolean anyUsage, int neutral, String note) {
        return anyUsage ? new Fit(neutral, note) : null;
    }

    // Interpolare liniara a lui `value` intre `worst` (0) si `best` (100), taiata la 0..100.
    private static int scale(double value, double worst, double best) {
        double fraction = (value - worst) / (best - worst);
        return (int) Math.round(Math.max(0, Math.min(1, fraction)) * 100);
    }
}
