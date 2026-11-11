package ro.cpa.advisor.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ro.cpa.advisor.domain.TestCars.PRICES;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Cifrele de referinta sunt calculate independent (script cu aceleasi formule documentate), nu cu codul testat. */
class TcoCalculatorTest {

    @Test
    void petrolCarOverFiveYears() {
        TcoResult tco = TcoCalculator.calculate(TestCars.golf(), 5, 15_000, null, PRICES, null, null, null);

        // 5,6 l/100 km * 750 de sute de km * 1,55 EUR; valoare reziduala 50% dupa 5 ani.
        assertThat(tco.energyEur()).isEqualByComparingTo("6510.00");
        assertThat(tco.resaleValueEur()).isEqualByComparingTo("14950.00");
        assertThat(tco.depreciationEur()).isEqualByComparingTo("14950.00");
        assertThat(tco.insuranceEur()).isEqualByComparingTo("4500.00");
        assertThat(tco.roadTaxEur()).isEqualByComparingTo("450.00");
        assertThat(tco.maintenanceEur()).isEqualByComparingTo("2250.00");
        assertThat(tco.interestEur()).isEqualByComparingTo("0.00");
        assertThat(tco.totalCostEur()).isEqualByComparingTo("28660.00");
        assertThat(tco.costPerKmEur()).isEqualByComparingTo("0.382");
        assertThat(tco.costPerMonthEur()).isEqualByComparingTo("477.67");
        assertThat(tco.annualRunningCostEur()).isEqualByComparingTo("2742.00");
    }

    @Test
    void totalIsDepreciationPlusRunningCostsPlusInterest() {
        TcoResult tco = TcoCalculator.calculate(TestCars.corolla(), 4, 12_000, null, PRICES, null, null, new TcoCalculator.Loan(new BigDecimal("5000"), new BigDecimal("7"), 36));

        BigDecimal sum = tco.depreciationEur().add(tco.energyEur()).add(tco.insuranceEur()).add(tco.roadTaxEur()).add(tco.maintenanceEur()).add(tco.interestEur());
        assertThat(tco.totalCostEur()).isEqualByComparingTo(sum);
        // Si invers: pretul de achizitie minus revanzarea este exact deprecierea.
        assertThat(tco.purchasePriceEur().subtract(tco.resaleValueEur())).isEqualByComparingTo(tco.depreciationEur());
    }

    @Test
    void depreciationFollowsAConstantYearlyRate() {
        // 50% dupa 5 ani => dupa 3 ani ramane 50%^(3/5) din pret.
        assertThat(TcoCalculator.calculate(TestCars.golf(), 3, 15_000, null, PRICES, null, null, null).resaleValueEur()).isEqualByComparingTo("19726.64");
    }

    @Test
    void electricCarDependsOnWhereItCharges() {
        TcoResult withoutHome = TcoCalculator.calculate(TestCars.spring(), 5, 15_000, false, PRICES, null, null, null);
        TcoResult withHome = TcoCalculator.calculate(TestCars.spring(), 5, 15_000, true, PRICES, null, null, null);
        TcoResult unknown = TcoCalculator.calculate(TestCars.spring(), 5, 15_000, null, PRICES, null, null, null);

        // Fara incarcare acasa: tot curentul la tariful public (0,55); cu incarcare acasa: 80% la 0,30 si 20% la 0,55 = 0,35.
        assertThat(withoutHome.energyEur()).isEqualByComparingTo("5733.75");
        assertThat(withHome.energyEur()).isEqualByComparingTo("3648.75");
        assertThat(withHome.totalCostEur()).isEqualByComparingTo("17538.75");
        assertThat(withoutHome.totalCostEur()).isEqualByComparingTo("19623.75");
        // Necunoscut = fara incarcare acasa (nu subestimam costul), iar ipoteza este spusa.
        assertThat(unknown.energyEur()).isEqualByComparingTo(withoutHome.energyEur());
        assertThat(unknown.assumptions()).anyMatch(a -> a.contains("Home charging unknown"));
    }

    @Test
    void theBuyersOwnEnergyPricesReplaceTheDefaults() {
        TcoResult tco = TcoCalculator.calculate(TestCars.golf(), 5, 15_000, null, PRICES, new BigDecimal("2.00"), null, null);

        assertThat(tco.energyEur()).isEqualByComparingTo("8400.00");
        assertThat(tco.assumptions()).anyMatch(a -> a.contains("given by the buyer"));
    }

    @Test
    void dieselUsesTheDieselPrice() {
        // 4,8 l/100 km * 750 * 1,60 EUR
        assertThat(TcoCalculator.calculate(TestCars.octavia(), 5, 15_000, null, PRICES, null, null, null).energyEur()).isEqualByComparingTo("5760.00");
    }

    @Test
    void aLoanAddsOnlyTheInterestPaidWhileTheCarIsOwned() {
        TcoCalculator.Loan loan = new TcoCalculator.Loan(new BigDecimal("5000"), new BigDecimal("6"), 48);

        TcoResult full = TcoCalculator.calculate(TestCars.golf(), 5, 15_000, null, PRICES, null, null, loan);
        TcoResult sold = TcoCalculator.calculate(TestCars.golf(), 2, 15_000, null, PRICES, null, null, loan);

        // Credit de 24.900 EUR pe 48 de luni: dobanda totala a fost calculata independent (3.169,31 pe 48 de luni).
        assertThat(full.interestEur()).isEqualByComparingTo("3169.31");
        assertThat(full.financing().monthlyPaymentEur()).isPositive();
        // Vanzare dupa 2 ani: se numara doar dobanda primelor 24 de rate, soldul ramas se achita din vanzare.
        assertThat(sold.interestEur()).isLessThan(full.interestEur()).isPositive();
    }

    @Test
    void plugInHybridUsesBothEnergiesAndSaysItsFiguresAssumeCharging() {
        TcoResult tco = TcoCalculator.calculate(TestCars.golfGte(), 5, 15_000, true, PRICES, null, null, null);

        // 1,5 l * 750 * 1,55 + 14,5 kWh * 750 * 0,35 = 1743,75 + 3806,25
        assertThat(tco.energyEur()).isEqualByComparingTo("5550.00");
        assertThat(tco.assumptions()).anyMatch(a -> a.contains("Plug-in hybrid"));
    }

    @Test
    void impossibleInputsAreRefused() {
        assertThatThrownBy(() -> TcoCalculator.calculate(TestCars.golf(), 0, 15_000, null, PRICES, null, null, null)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> TcoCalculator.calculate(TestCars.golf(), 11, 15_000, null, PRICES, null, null, null)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> TcoCalculator.calculate(TestCars.golf(), 5, 500, null, PRICES, null, null, null)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> TcoCalculator.calculate(TestCars.golf(), 5, 15_000, null, PRICES, new BigDecimal("-1"), null, null)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void everyResultStatesItsAssumptions() {
        TcoResult tco = TcoCalculator.calculate(TestCars.golf(), 5, 15_000, null, PRICES, null, null, null);

        assertThat(tco.assumptions())
                .anyMatch(a -> a.contains("indicative catalogue figures dated 2026-11-11"))
                .anyMatch(a -> a.contains("15000 km/year"))
                .anyMatch(a -> a.contains("Fuel price 1.55"))
                .anyMatch(a -> a.contains("Depreciation"));
    }
}
