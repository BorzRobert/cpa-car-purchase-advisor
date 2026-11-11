package ro.cpa.advisor.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculatorul de cost total de detinere (TCO). Cod pur: aceleasi intrari dau mereu acelasi rezultat, iar toate cifrele
 * vin din catalog, din preturile de energie configurate sau din ce a spus cumparatorul, nu din memoria modelului.
 *
 * <p>Regulile (toate raportate ca ipoteze in rezultat): deprecierea urmeaza o rata anuala constanta dedusa din valoarea
 * reziduala la 5 ani din catalog; asigurarea, taxa si mentenanta sunt sume anuale fixe (fara inflatie si fara uzura);
 * fara reduceri, taxe de inmatriculare sau comisioane de credit.
 */
public final class TcoCalculator {

    public static final int MIN_KM_PER_YEAR = 1_000;
    public static final int MAX_KM_PER_YEAR = 200_000;
    public static final int MAX_YEARS = 10;

    // Cat din energia unui electric se incarca acasa (restul la statii publice); fara incarcare acasa, totul e public.
    private static final BigDecimal HOME_SHARE_WITH_HOME_CHARGING = new BigDecimal("0.8");

    private TcoCalculator() {}

    /** Creditul de luat in calcul: avansul, dobanda nominala anuala si numarul de luni. */
    public record Loan(BigDecimal downPaymentEur, BigDecimal aprPercent, int months) {}

    /**
     * @param homeCharging `null` = necunoscut; la electrice si hibrizi plug-in se presupune ca nu are, ca sa nu
     *     subestimam costul, si ipoteza apare in rezultat
     * @param fuelPriceOverride pretul combustibilului dat de cumparator (EUR/litru); `null` = pretul implicit
     * @param electricityPriceOverride pretul curentului dat de cumparator (EUR/kWh); `null` = pretul implicit
     * @param loan `null` = achizitie cash
     */
    public static TcoResult calculate(
            VehicleCard car,
            int years,
            int kmPerYear,
            Boolean homeCharging,
            EnergyPrices prices,
            BigDecimal fuelPriceOverride,
            BigDecimal electricityPriceOverride,
            Loan loan) {
        if (years < 1 || years > MAX_YEARS) {
            throw new InvalidInputException("years must be between 1 and " + MAX_YEARS + ".");
        }
        if (kmPerYear < MIN_KM_PER_YEAR || kmPerYear > MAX_KM_PER_YEAR) {
            throw new InvalidInputException("kmPerYear must be between " + MIN_KM_PER_YEAR + " and " + MAX_KM_PER_YEAR + ".");
        }
        if (fuelPriceOverride != null && fuelPriceOverride.signum() <= 0 || electricityPriceOverride != null && electricityPriceOverride.signum() <= 0) {
            throw new InvalidInputException("Energy prices must be positive.");
        }

        List<String> assumptions = new ArrayList<>();
        assumptions.add("Prices and costs are indicative catalogue figures dated " + car.dataAsOf() + "; no discounts, registration fees or inflation.");
        assumptions.add("Distance: " + kmPerYear + " km/year for " + years + " years.");

        BigDecimal hundredsOfKm = BigDecimal.valueOf((long) kmPerYear * years).divide(BigDecimal.valueOf(100), Money.PRECISION);
        BigDecimal energy = energyCost(car, hundredsOfKm, homeCharging, prices, fuelPriceOverride, electricityPriceOverride, assumptions);

        BigDecimal price = BigDecimal.valueOf(car.priceEur());
        double residualAfter5 = car.residualValuePercentAfter5Years() / 100.0;
        BigDecimal resale = Money.of(car.priceEur() * StrictMath.pow(residualAfter5, years / 5.0));
        BigDecimal depreciation = Money.round(price.subtract(resale));
        assumptions.add("Depreciation uses a constant yearly rate derived from the catalogue's 5-year residual value ("
                + car.residualValuePercentAfter5Years() + "% of the new price).");

        BigDecimal insurance = Money.round(BigDecimal.valueOf((long) car.annualInsuranceEur() * years));
        BigDecimal roadTax = Money.round(BigDecimal.valueOf((long) car.annualRoadTaxEur() * years));
        BigDecimal maintenance = Money.round(BigDecimal.valueOf((long) car.annualMaintenanceEur() * years));
        assumptions.add("Insurance, road tax and maintenance are flat yearly amounts from the catalogue (no inflation, no ageing effects).");

        FinancingResult financing = null;
        BigDecimal interest = BigDecimal.ZERO.setScale(2);
        if (loan != null) {
            financing = Financing.calculate(price, loan.downPaymentEur(), loan.aprPercent(), loan.months());
            interest = Financing.interestInFirstPayments(financing.financedAmountEur(), financing.aprPercent(), loan.months(), years * 12);
            assumptions.add("Loan: APR treated as a nominal yearly rate, no fees or insurance; the balance left at sale is repaid from the sale price and is not counted as a cost.");
        }

        BigDecimal total = depreciation.add(energy).add(insurance).add(roadTax).add(maintenance).add(interest);
        BigDecimal annualRunning = Money.round(energy.divide(BigDecimal.valueOf(years), Money.PRECISION)
                .add(BigDecimal.valueOf(car.annualInsuranceEur() + car.annualRoadTaxEur() + car.annualMaintenanceEur())));
        BigDecimal perKm = total.divide(BigDecimal.valueOf((long) kmPerYear * years), 3, RoundingMode.HALF_UP);
        BigDecimal perMonth = Money.round(total.divide(BigDecimal.valueOf(years * 12L), Money.PRECISION));

        return new TcoResult(
                car.variantId(),
                years,
                kmPerYear,
                Money.round(price),
                resale,
                depreciation,
                energy,
                insurance,
                roadTax,
                maintenance,
                interest,
                Money.round(total),
                perKm,
                perMonth,
                annualRunning,
                financing,
                List.copyOf(assumptions));
    }

    private static BigDecimal energyCost(
            VehicleCard car,
            BigDecimal hundredsOfKm,
            Boolean homeCharging,
            EnergyPrices prices,
            BigDecimal fuelOverride,
            BigDecimal electricityOverride,
            List<String> assumptions) {
        boolean usesFuel = car.powertrain() != Powertrain.ELECTRIC;
        boolean usesElectricity = car.powertrain() == Powertrain.ELECTRIC || car.powertrain() == Powertrain.PLUG_IN_HYBRID;
        BigDecimal cost = BigDecimal.ZERO;

        if (usesFuel) {
            BigDecimal litres = require(car.consumptionLitersPer100Km(), car, "fuel consumption");
            BigDecimal fuelPrice = fuelOverride != null ? fuelOverride : car.powertrain() == Powertrain.DIESEL ? prices.dieselEurPerLiter() : prices.petrolEurPerLiter();
            cost = cost.add(litres.multiply(hundredsOfKm).multiply(fuelPrice));
            assumptions.add("Fuel price " + fuelPrice.stripTrailingZeros().toPlainString() + " EUR/litre ("
                    + (fuelOverride != null ? "given by the buyer" : "default, dated " + prices.dataAsOf()) + "); catalogue consumption figures are official, real use is usually higher.");
        }
        if (usesElectricity) {
            BigDecimal kwh = require(car.consumptionKwhPer100Km(), car, "electricity consumption");
            BigDecimal electricityPrice;
            if (electricityOverride != null) {
                electricityPrice = electricityOverride;
                assumptions.add("Electricity price " + electricityPrice.stripTrailingZeros().toPlainString() + " EUR/kWh, given by the buyer.");
            } else {
                BigDecimal homeShare = Boolean.TRUE.equals(homeCharging) ? HOME_SHARE_WITH_HOME_CHARGING : BigDecimal.ZERO;
                electricityPrice = homeShare.multiply(prices.homeElectricityEurPerKwh())
                        .add(BigDecimal.ONE.subtract(homeShare).multiply(prices.publicElectricityEurPerKwh()));
                assumptions.add(homeCharging == null
                        ? "Home charging unknown: assumed none, so all electricity is priced at the public charging rate ("
                                + prices.publicElectricityEurPerKwh().toPlainString() + " EUR/kWh, dated " + prices.dataAsOf() + ")."
                        : Boolean.TRUE.equals(homeCharging)
                                ? "With home charging: 80% of electricity at the home rate (" + prices.homeElectricityEurPerKwh().toPlainString()
                                        + " EUR/kWh), 20% at the public rate (" + prices.publicElectricityEurPerKwh().toPlainString() + " EUR/kWh), dated " + prices.dataAsOf() + "."
                                : "Without home charging all electricity is priced at the public rate (" + prices.publicElectricityEurPerKwh().toPlainString()
                                        + " EUR/kWh, dated " + prices.dataAsOf() + ").");
            }
            cost = cost.add(kwh.multiply(hundredsOfKm).multiply(electricityPrice));
        }
        if (car.powertrain() == Powertrain.PLUG_IN_HYBRID) {
            assumptions.add("Plug-in hybrid: uses the catalogue's official combined fuel and electricity figures, which assume regular charging; without it real fuel use is higher.");
        }
        return Money.round(cost);
    }

    private static BigDecimal require(BigDecimal value, VehicleCard car, String what) {
        if (value == null) {
            throw new InvalidInputException("The catalogue has no " + what + " for " + car.variantId() + ", so its running cost cannot be computed.");
        }
        return value;
    }
}
