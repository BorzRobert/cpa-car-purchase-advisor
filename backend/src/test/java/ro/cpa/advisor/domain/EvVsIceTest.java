package ro.cpa.advisor.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ro.cpa.advisor.domain.TestCars.PRICES;

import org.junit.jupiter.api.Test;

/** Referintele (an cu an) sunt calculate independent, cu aceleasi formule documentate in `TcoCalculator`. */
class EvVsIceTest {

    @Test
    void anElectricWithHomeChargingBreaksEvenInTheSecondYear() {
        EvVsIce.Comparison result = EvVsIce.compare(TestCars.spring(), TestCars.sandero(), 15_000, true, PRICES, 8);

        assertThat(result.purchasePremiumEur()).isEqualByComparingTo("2400.00");
        assertThat(result.evAnnualRunningCostEur()).isEqualByComparingTo("1479.75");
        assertThat(result.iceAnnualRunningCostEur()).isEqualByComparingTo("2242.00");
        assertThat(result.annualRunningSavingEur()).isEqualByComparingTo("762.25");
        assertThat(result.simplePaybackYears()).isEqualByComparingTo("3.1");
        // Anul 1: electricul este cu 87,94 mai scump; anul 2: cu 27,68 mai ieftin => incruciseaza la 1 + 87,94/115,62.
        assertThat(result.tcoBreakEvenYears()).isEqualByComparingTo("1.8");
        assertThat(result.years()).hasSize(8);
        assertThat(result.years().get(0).evMinusIceEur()).isEqualByComparingTo("87.94");
        assertThat(result.years().get(1).evMinusIceEur()).isEqualByComparingTo("-27.68");
        assertThat(result.years().get(7).evMinusIceEur()).isEqualByComparingTo("-3118.28");
    }

    @Test
    void withoutHomeChargingTheSavingIsSmallAndTheTcoNeverCatchesUpWithinTheHorizon() {
        EvVsIce.Comparison result = EvVsIce.compare(TestCars.spring(), TestCars.sandero(), 15_000, false, PRICES, 8);

        assertThat(result.annualRunningSavingEur()).isEqualByComparingTo("345.25");
        assertThat(result.simplePaybackYears()).isEqualByComparingTo("7.0");
        assertThat(result.tcoBreakEvenYears()).isNull();
        assertThat(result.years().get(7).evMinusIceEur()).isEqualByComparingTo("217.72");
    }

    @Test
    void lowMileageMakesTheElectricHardToJustify() {
        EvVsIce.Comparison result = EvVsIce.compare(TestCars.spring(), TestCars.sandero(), 5_000, false, PRICES, 8);

        assertThat(result.simplePaybackYears()).isEqualByComparingTo("9.9");
        assertThat(result.tcoBreakEvenYears()).isNull();
    }

    @Test
    void highMileageMakesTheElectricCheaperFromTheStart() {
        EvVsIce.Comparison result = EvVsIce.compare(TestCars.spring(), TestCars.sandero(), 30_000, true, PRICES, 8);

        assertThat(result.tcoBreakEvenYears()).isEqualByComparingTo("0");
        assertThat(result.simplePaybackYears()).isEqualByComparingTo("1.8");
    }

    @Test
    void thePairMustBeAnElectricAgainstANonElectric() {
        assertThatThrownBy(() -> EvVsIce.compare(TestCars.golf(), TestCars.sandero(), 15_000, true, PRICES, 5))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("electric");
        assertThatThrownBy(() -> EvVsIce.compare(TestCars.spring(), TestCars.spring(), 15_000, true, PRICES, 5)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void aCheaperElectricHasNoPaybackBecauseThereIsNoPremiumToRecover() {
        VehicleCard cheapEv = TestCars.card("cheap-ev", "X", "Y", BodyType.HATCHBACK, Powertrain.ELECTRIC, 12000, 4, 300, null, null, "14.0", 500, 0, 250, 40);

        assertThat(EvVsIce.compare(cheapEv, TestCars.sandero(), 15_000, true, PRICES, 5).simplePaybackYears()).isNull();
    }
}
