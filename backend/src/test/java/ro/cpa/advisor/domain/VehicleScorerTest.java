package ro.cpa.advisor.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static ro.cpa.advisor.domain.TestCars.PRICES;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import ro.cpa.advisor.domain.ScoredVehicle.CriterionScore;
import ro.cpa.advisor.domain.VehicleScorer.Ranking;

class VehicleScorerTest {

    private static CriterionScore criterion(ScoredVehicle scored, Criterion criterion) {
        return scored.breakdown().stream().filter(c -> c.criterion() == criterion).findFirst().orElseThrow();
    }

    private static ScoredVehicle scoreOne(VehicleCard car, BuyerProfile profile) {
        return VehicleScorer.rank(List.of(car), profile, PRICES, 5).ranked().getFirst();
    }

    @Test
    void weightsAreFixedAndSumToOneHundred() {
        assertThat(java.util.Arrays.stream(Criterion.values()).mapToInt(Criterion::weight).sum()).isEqualTo(100);
    }

    @Test
    void priceFitIsComfortableUnderBudgetAndFallsToZeroFifteenPercentOver() {
        assertThat(VehicleScorer.priceScore(0.50)).isEqualTo(100);
        assertThat(VehicleScorer.priceScore(0.85)).isEqualTo(100);
        assertThat(VehicleScorer.priceScore(0.925)).isEqualTo(85);
        assertThat(VehicleScorer.priceScore(1.0)).isEqualTo(70);
        assertThat(VehicleScorer.priceScore(1.075)).isEqualTo(35);
        assertThat(VehicleScorer.priceScore(1.15)).isEqualTo(0);
        assertThat(VehicleScorer.priceScore(2.0)).isEqualTo(0);
    }

    @Test
    void aTotalBudgetIsComparedWithThePriceAndAMonthlyOneWithTheMonthlyCostOfOwnership() {
        CriterionScore total = criterion(scoreOne(TestCars.golf(), TestCars.profile(BudgetKind.TOTAL, "30000", null, null, null, null)), Criterion.PRICE_FIT);
        // 29.900 / 30.000 = 0,9967 => 100 - (0,9967 - 0,85) / 0,15 * 30 = 70,67
        assertThat(total.score()).isEqualTo(71);
        assertThat(total.detail()).contains("29900").contains("30000");

        ScoredVehicle monthlyScored = scoreOne(TestCars.golf(), TestCars.profile(BudgetKind.MONTHLY, "600", null, null, null, null));
        CriterionScore monthly = criterion(monthlyScored, Criterion.PRICE_FIT);
        // Cost total 28.660 EUR pe 60 de luni = 477,67 EUR/luna: 477,67 / 600 = 0,796 => sub 85% din buget.
        assertThat(monthly.score()).isEqualTo(100);
        assertThat(monthly.detail()).contains("477.67").contains("600");
    }

    @Test
    void withoutABudgetPriceFitIsLeftOutInsteadOfGuessed() {
        CriterionScore price = criterion(scoreOne(TestCars.golf(), BuyerProfile.empty()), Criterion.PRICE_FIT);

        assertThat(price.applicable()).isFalse();
        assertThat(price.detail()).contains("bugetul");
    }

    @Test
    void spaceNeedsEnoughSeatsAndRewardsBoot() {
        assertThat(criterion(scoreOne(TestCars.golf(), TestCars.profile(null, null, null, null, 6, null)), Criterion.SPACE).score()).isEqualTo(0);
        // (380 - 250) / 300 = 43%
        assertThat(criterion(scoreOne(TestCars.golf(), TestCars.profile(null, null, null, null, 4, null)), Criterion.SPACE).score()).isEqualTo(43);
        assertThat(criterion(scoreOne(TestCars.octavia(), BuyerProfile.empty()), Criterion.SPACE).score()).isEqualTo(100);
        assertThat(criterion(scoreOne(TestCars.sandero(), BuyerProfile.empty()), Criterion.SPACE).score()).isEqualTo(26);
    }

    @Test
    void safetyUsesTheRatingAndNeverAssumesAMissingOne() {
        assertThat(criterion(scoreOne(TestCars.golf(), BuyerProfile.empty()), Criterion.SAFETY).score()).isEqualTo(100);
        assertThat(criterion(scoreOne(TestCars.spring(), BuyerProfile.empty()), Criterion.SAFETY).score()).isEqualTo(20);
        CriterionScore unknown = criterion(scoreOne(TestCars.sandero(), BuyerProfile.empty()), Criterion.SAFETY);
        assertThat(unknown.applicable()).isFalse();
        assertThat(unknown.detail()).contains("nu o presupunem");
    }

    @Test
    void reliabilityIsAnHonestlyLabelledProxyOfResaleValue() {
        CriterionScore golf = criterion(scoreOne(TestCars.golf(), BuyerProfile.empty()), Criterion.RELIABILITY);

        // (50 - 35) / 25 = 60%
        assertThat(golf.score()).isEqualTo(60);
        assertThat(golf.detail()).contains("aproximare").contains("Notă de test.");
    }

    @Test
    void usageFitFollowsHomeChargingForElectricsAndCityShareForHybrids() {
        BuyerProfile noHome = TestCars.profile(null, null, null, 80, null, false);
        BuyerProfile home = TestCars.profile(null, null, null, 80, null, true);

        assertThat(criterion(scoreOne(TestCars.spring(), noHome), Criterion.USAGE_FIT).score()).isEqualTo(25);
        assertThat(criterion(scoreOne(TestCars.spring(), home), Criterion.USAGE_FIT).score()).isEqualTo(90);
        assertThat(criterion(scoreOne(TestCars.corolla(), noHome), Criterion.USAGE_FIT).score()).isEqualTo(90);
        assertThat(criterion(scoreOne(TestCars.golfGte(), noHome), Criterion.USAGE_FIT).score()).isEqualTo(30);
        // Diesel in oraș, la kilometraj mic, este o alegere slaba.
        assertThat(criterion(scoreOne(TestCars.octavia(), TestCars.profile(null, null, 8000, 80, null, null)), Criterion.USAGE_FIT).score()).isEqualTo(30);
        assertThat(criterion(scoreOne(TestCars.octavia(), TestCars.profile(null, null, 30000, 20, null, null)), Criterion.USAGE_FIT).score()).isEqualTo(90);
    }

    @Test
    void statedPreferencesCountInUsageFit() {
        BuyerProfile wantsEstate = new BuyerProfile(
                null, null, null, null, null, null, List.of(BodyType.ESTATE), List.of(Powertrain.HYBRID), null, null, List.of(), List.of(), null);

        CriterionScore estateDiesel = criterion(scoreOne(TestCars.octavia(), wantsEstate), Criterion.USAGE_FIT);
        CriterionScore hatchHybrid = criterion(scoreOne(TestCars.corolla(), wantsEstate), Criterion.USAGE_FIT);

        // Octavia: caroseria se potriveste (100), motorizarea nu (20) => 60. Corolla: motorizarea da (100), caroseria nu (20) => 60.
        assertThat(estateDiesel.score()).isEqualTo(60);
        assertThat(hatchHybrid.score()).isEqualTo(60);
        assertThat(estateDiesel.detail()).contains("caroseria este printre cele dorite").contains("motorizarea nu este printre cele dorite");
    }

    @Test
    void criteriaWithoutDataLeaveTheAverageInsteadOfDraggingItDown() {
        // Profil gol: raman doar costurile curente (20), spatiul (15), siguranta (15) si fiabilitatea (10).
        ScoredVehicle golf = scoreOne(TestCars.golf(), BuyerProfile.empty());

        assertThat(golf.breakdown()).filteredOn(CriterionScore::applicable).extracting(c -> c.criterion()).containsExactly(
                Criterion.RUNNING_COST, Criterion.SPACE, Criterion.SAFETY, Criterion.RELIABILITY);
        // Costuri curente: 2.742 EUR/an / 15.000 km = 0,1828 EUR/km => (0,25 - 0,1828) / 0,17 = 40; spatiu 43; siguranta 100; fiabilitate 60.
        // (40*20 + 43*15 + 100*15 + 60*10) / 60 = 59,08
        assertThat(criterion(golf, Criterion.RUNNING_COST).score()).isEqualTo(40);
        assertThat(golf.totalScore()).isEqualTo(59);
    }

    @Test
    void theRankingOrdersByTotalScoreAndIsReproducible() {
        BuyerProfile family = TestCars.profile(BudgetKind.TOTAL, "35000", 15000, 50, 5, false);
        List<VehicleCard> candidates = List.of(TestCars.spring(), TestCars.golf(), TestCars.octavia(), TestCars.corolla(), TestCars.sandero());

        Ranking first = VehicleScorer.rank(candidates, family, PRICES, 5);
        Ranking again = VehicleScorer.rank(candidates, family, PRICES, 5);

        assertThat(first.ranked()).extracting(ScoredVehicle::totalScore).isSortedAccordingTo(java.util.Comparator.reverseOrder());
        assertThat(first.ranked().stream().map(s -> s.card().variantId()).toList()).isEqualTo(again.ranked().stream().map(s -> s.card().variantId()).toList());
        // O familie de 5, fara incarcare acasa, nu are ce cauta pe primul loc cu o electrica de 4 locuri.
        assertThat(first.ranked().getLast().card().variantId()).isEqualTo("spring-electric-65");
        assertThat(first.ranked().getFirst().tco().years()).isEqualTo(5);
    }

    @Test
    void assumptionsSayWhatWasDefaultedAndWhatCannotBeScored() {
        Ranking ranking = VehicleScorer.rank(List.of(TestCars.golf()), BuyerProfile.empty(), PRICES, 5);

        assertThat(ranking.kmPerYear()).isEqualTo(15_000);
        assertThat(ranking.assumptions())
                .anyMatch(a -> a.contains("Yearly distance not given: 15000"))
                .anyMatch(a -> a.contains("Reliability has no numeric source"))
                .anyMatch(a -> a.contains("Must-haves and deal-breakers"));
    }

    @Test
    void theComparisonReportCarriesTheSameNumbersAsTheRanking() {
        BuyerProfile profile = TestCars.profile(BudgetKind.TOTAL, "30000", 15000, null, null, null);
        Ranking ranking = VehicleScorer.rank(List.of(TestCars.golf(), TestCars.corolla()), profile, PRICES, 5);

        ComparisonReport report = ComparisonReport.from(ranking);

        assertThat(report.vehicles()).extracting(VehicleCard::variantId).isEqualTo(ranking.ranked().stream().map(s -> s.card().variantId()).toList());
        assertThat(report.criteria()).extracting(ComparisonReport.CriterionInfo::id)
                .containsExactly("price_fit", "running_cost", "space", "safety", "reliability", "usage_fit");
        ComparisonReport.ScoreEntry first = report.scores().getFirst();
        assertThat(first.total()).isEqualTo(ranking.ranked().getFirst().totalScore());
        assertThat(first.tco().totalCostEur()).isEqualByComparingTo(ranking.ranked().getFirst().tco().totalCostEur());
        assertThat(first.breakdown()).hasSize(6);
        assertThat(report.dataAsOf()).isEqualTo(java.time.LocalDate.of(2026, 11, 11));
        assertThat(report.kmPerYear()).isEqualTo(15000);
        assertThat(first.tco().costPerMonthEur()).isGreaterThan(BigDecimal.ZERO);
    }
}
