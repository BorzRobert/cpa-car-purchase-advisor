package ro.cpa.advisor.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * „Se plateste un electric pentru kilometrajul si accesul meu la incarcare?” Compara cost cu cost un electric cu o
 * masina cu ardere interna (sau hibrid), an cu an, cu acelasi calculator TCO si aceleasi ipoteze pentru ambele.
 *
 * <ul>
 *   <li>{@code simplePaybackYears}: in cati ani economia la costurile curente acopera diferenta de pret de achizitie
 *       (ignora revanzarea); `null` daca electricul nu economiseste sau nu costa mai mult la achizitie.
 *   <li>{@code tcoBreakEvenYears}: primul moment in care TCO-ul cumulat al electricului (cu depreciere) ajunge la cel al
 *       celeilalte masini, interpolat liniar in cadrul anului; `null` daca nu se intampla in orizont.
 * </ul>
 */
public final class EvVsIce {

    private EvVsIce() {}

    public record YearRow(int year, BigDecimal evCumulativeTcoEur, BigDecimal iceCumulativeTcoEur, BigDecimal evMinusIceEur) {}

    public record Comparison(
            String evVariantId,
            String iceVariantId,
            int kmPerYear,
            int horizonYears,
            BigDecimal purchasePremiumEur,
            BigDecimal evAnnualRunningCostEur,
            BigDecimal iceAnnualRunningCostEur,
            BigDecimal annualRunningSavingEur,
            BigDecimal simplePaybackYears,
            BigDecimal tcoBreakEvenYears,
            BigDecimal evTotalCostAtHorizonEur,
            BigDecimal iceTotalCostAtHorizonEur,
            List<YearRow> years,
            List<String> assumptions) {}

    public static Comparison compare(
            VehicleCard ev, VehicleCard ice, int kmPerYear, Boolean homeCharging, EnergyPrices prices, int horizonYears) {
        if (ev.powertrain() != Powertrain.ELECTRIC) {
            throw new InvalidInputException("evVariantId must be an electric variant.");
        }
        if (ice.powertrain() == Powertrain.ELECTRIC) {
            throw new InvalidInputException("iceVariantId must be a petrol, diesel or hybrid variant, not an electric one.");
        }

        List<YearRow> rows = new ArrayList<>();
        BigDecimal previousDifference = BigDecimal.ZERO;
        BigDecimal breakEven = null;
        TcoResult evAtHorizon = null;
        TcoResult iceAtHorizon = null;
        for (int year = 1; year <= horizonYears; year++) {
            TcoResult evTco = TcoCalculator.calculate(ev, year, kmPerYear, homeCharging, prices, null, null, null);
            TcoResult iceTco = TcoCalculator.calculate(ice, year, kmPerYear, homeCharging, prices, null, null, null);
            BigDecimal difference = evTco.totalCostEur().subtract(iceTco.totalCostEur());
            rows.add(new YearRow(year, evTco.totalCostEur(), iceTco.totalCostEur(), difference));
            if (breakEven == null && difference.signum() <= 0) {
                breakEven = interpolateCrossing(year - 1, previousDifference, year, difference);
            }
            previousDifference = difference;
            evAtHorizon = evTco;
            iceAtHorizon = iceTco;
        }

        BigDecimal premium = BigDecimal.valueOf((long) ev.priceEur() - ice.priceEur());
        BigDecimal saving = Money.round(iceAtHorizon.annualRunningCostEur().subtract(evAtHorizon.annualRunningCostEur()));
        BigDecimal payback = null;
        if (premium.signum() > 0 && saving.signum() > 0) {
            payback = premium.divide(saving, 1, RoundingMode.HALF_UP);
        }

        List<String> assumptions = new ArrayList<>(evAtHorizon.assumptions());
        assumptions.add("The same distance, prices and rules are applied to both cars; the other car's energy assumptions are listed in its own TCO.");
        return new Comparison(
                ev.variantId(),
                ice.variantId(),
                kmPerYear,
                horizonYears,
                Money.round(premium),
                evAtHorizon.annualRunningCostEur(),
                iceAtHorizon.annualRunningCostEur(),
                saving,
                payback,
                breakEven,
                evAtHorizon.totalCostEur(),
                iceAtHorizon.totalCostEur(),
                List.copyOf(rows),
                List.copyOf(assumptions));
    }

    // Diferenta porneste de la 0 la momentul 0 (nimic cheltuit inca) si se schimba liniar intre doua capete de an.
    private static BigDecimal interpolateCrossing(int fromYear, BigDecimal fromDifference, int toYear, BigDecimal toDifference) {
        if (fromDifference.signum() <= 0) {
            return BigDecimal.valueOf(fromYear).setScale(1, RoundingMode.HALF_UP);
        }
        BigDecimal span = fromDifference.subtract(toDifference);
        BigDecimal fraction = fromDifference.divide(span, 3, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(fromYear).add(fraction.multiply(BigDecimal.valueOf(toYear - fromYear))).setScale(1, RoundingMode.HALF_UP);
    }
}
