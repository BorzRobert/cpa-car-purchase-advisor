package ro.cpa.advisor.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Cifrele de referinta sunt calculate independent (formula anuitatii cu precizie mare), nu cu codul testat. */
class FinancingTest {

    @Test
    void fixedRateLoanMatchesTheAnnuityFormula() {
        FinancingResult result = Financing.calculate(new BigDecimal("20000"), new BigDecimal("5000"), new BigDecimal("6"), 48);

        assertThat(result.financedAmountEur()).isEqualByComparingTo("15000.00");
        assertThat(result.monthlyPaymentEur()).isEqualByComparingTo("352.28");
        assertThat(result.totalPaidInInstalmentsEur()).isEqualByComparingTo("16909.44");
        assertThat(result.totalInterestEur()).isEqualByComparingTo("1909.44");
        assertThat(result.totalOutlayWithDownPaymentEur()).isEqualByComparingTo("21909.44");
    }

    @Test
    void zeroInterestSplitsThePrincipalEvenly() {
        FinancingResult result = Financing.calculate(new BigDecimal("12000"), BigDecimal.ZERO, BigDecimal.ZERO, 24);

        assertThat(result.monthlyPaymentEur()).isEqualByComparingTo("500.00");
        assertThat(result.totalInterestEur()).isEqualByComparingTo("0.00");
    }

    @Test
    void paidInFullMeansNoLoanAndNoInterest() {
        FinancingResult result = Financing.calculate(new BigDecimal("10000"), new BigDecimal("10000"), new BigDecimal("9"), 36);

        assertThat(result.financedAmountEur()).isEqualByComparingTo("0.00");
        assertThat(result.monthlyPaymentEur()).isEqualByComparingTo("0.00");
        assertThat(result.totalInterestEur()).isEqualByComparingTo("0.00");
    }

    @Test
    void interestPaidBeforeAnEarlySaleIsOnlyTheInstalmentsMade() {
        BigDecimal financed = new BigDecimal("15000");

        assertThat(Financing.interestInFirstPayments(financed, new BigDecimal("6"), 48, 24)).isEqualByComparingTo("1402.95");
        // Dupa toate ratele, dobanda este cea totala (diferenta fata de rotunjirea ratei este sub 1 euro).
        assertThat(Financing.interestInFirstPayments(financed, new BigDecimal("6"), 48, 48)).isBetween(new BigDecimal("1908.50"), new BigDecimal("1910.50"));
        // Mai multe luni decat durata creditului nu adauga dobanda.
        assertThat(Financing.interestInFirstPayments(financed, new BigDecimal("6"), 48, 60))
                .isEqualByComparingTo(Financing.interestInFirstPayments(financed, new BigDecimal("6"), 48, 48));
    }

    @Test
    void invalidInputsAreRefused() {
        assertThatThrownBy(() -> Financing.calculate(new BigDecimal("0"), null, new BigDecimal("5"), 12)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> Financing.calculate(new BigDecimal("1000"), new BigDecimal("2000"), new BigDecimal("5"), 12)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> Financing.calculate(new BigDecimal("1000"), null, new BigDecimal("-1"), 12)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> Financing.calculate(new BigDecimal("1000"), null, new BigDecimal("5"), 0)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> Financing.calculate(new BigDecimal("1000"), null, new BigDecimal("5"), 121)).isInstanceOf(InvalidInputException.class);
    }
}
