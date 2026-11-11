package ro.cpa.advisor.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import ro.cpa.advisor.domain.ScoredVehicle.CriterionScore;
import ro.cpa.advisor.domain.VehicleScorer.Ranking;

/**
 * Comparatia gata de afisat si de spus: aceeasi structura ajunge la UI (evenimentul `compare`, widgetul de comparatie)
 * si la model (rezultatul uneltei), deci cifrele din raspuns sunt exact cele din widget. Campurile sunt text si numere
 * simple (fara enum-uri de domeniu), ca payload-ul sa fie stabil.
 */
public record ComparisonReport(
        LocalDate dataAsOf,
        int years,
        int kmPerYear,
        List<String> assumptions,
        List<CriterionInfo> criteria,
        List<VehicleCard> vehicles,
        List<ScoreEntry> scores) {

    public record CriterionInfo(String id, String label, int weight) {}

    public record ScoreEntry(String variantId, int total, List<BreakdownEntry> breakdown, TcoSummary tco) {}

    public record BreakdownEntry(String criterion, String label, boolean applicable, int score, String detail) {}

    public record TcoSummary(
            BigDecimal totalCostEur,
            BigDecimal costPerMonthEur,
            BigDecimal costPerKmEur,
            BigDecimal annualRunningCostEur,
            BigDecimal depreciationEur,
            BigDecimal energyEur) {}

    /** Vehiculele si scorurile in ordinea clasamentului (cel mai bun primul). */
    public static ComparisonReport from(Ranking ranking) {
        List<ScoredVehicle> ranked = ranking.ranked();
        List<CriterionInfo> criteria = java.util.Arrays.stream(Criterion.values())
                .map(c -> new CriterionInfo(c.id(), c.label(), c.weight()))
                .toList();
        List<ScoreEntry> scores = ranked.stream().map(ComparisonReport::entry).toList();
        LocalDate dataAsOf = ranked.stream().map(s -> s.card().dataAsOf()).max(Comparator.naturalOrder()).orElse(null);
        return new ComparisonReport(
                dataAsOf, ranking.years(), ranking.kmPerYear(), ranking.assumptions(), criteria, ranked.stream().map(ScoredVehicle::card).toList(), scores);
    }

    private static ScoreEntry entry(ScoredVehicle scored) {
        List<BreakdownEntry> breakdown = scored.breakdown().stream().map(ComparisonReport::breakdown).toList();
        TcoResult tco = scored.tco();
        return new ScoreEntry(
                scored.card().variantId(),
                scored.totalScore(),
                breakdown,
                new TcoSummary(tco.totalCostEur(), tco.costPerMonthEur(), tco.costPerKmEur(), tco.annualRunningCostEur(), tco.depreciationEur(), tco.energyEur()));
    }

    private static BreakdownEntry breakdown(CriterionScore score) {
        return new BreakdownEntry(score.criterion().id(), score.criterion().label(), score.applicable(), score.score(), score.detail());
    }
}
