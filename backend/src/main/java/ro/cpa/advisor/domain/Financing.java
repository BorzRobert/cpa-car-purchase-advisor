package ro.cpa.advisor.domain;

import java.math.BigDecimal;

/**
 * Calculul de finantare: rata lunara (anuitate), dobanda totala si dobanda platita pana la o anumita luna (pentru TCO,
 * cand masina se vinde inainte de sfarsitul creditului). Cod pur, fara baza de date: acelasi rezultat pentru aceleasi
 * cifre, verificabil cu teste. Cifrele vin ca argumente, nu din memoria modelului.
 */
public final class Financing {

    public static final int MAX_MONTHS = 120;
    private static final BigDecimal TWELVE_HUNDRED = BigDecimal.valueOf(1200);

    private Financing() {}

    public static FinancingResult calculate(BigDecimal priceEur, BigDecimal downPaymentEur, BigDecimal aprPercent, int months) {
        if (priceEur == null || priceEur.signum() <= 0) {
            throw new InvalidInputException("priceEur must be positive.");
        }
        BigDecimal down = downPaymentEur == null ? BigDecimal.ZERO : downPaymentEur;
        if (down.signum() < 0 || down.compareTo(priceEur) > 0) {
            throw new InvalidInputException("downPaymentEur must be between 0 and the price.");
        }
        BigDecimal apr = aprPercent == null ? BigDecimal.ZERO : aprPercent;
        if (apr.signum() < 0 || apr.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidInputException("aprPercent must be between 0 and 100.");
        }
        if (months < 1 || months > MAX_MONTHS) {
            throw new InvalidInputException("months must be between 1 and " + MAX_MONTHS + ".");
        }

        BigDecimal financed = priceEur.subtract(down);
        BigDecimal payment = payment(financed, apr, months);
        BigDecimal totalPaid = Money.round(payment.multiply(BigDecimal.valueOf(months)));
        // Fara credit (suma financiata zero) nu exista nici dobanda; altfel dobanda este ce se plateste peste capital.
        BigDecimal interest = financed.signum() == 0 ? BigDecimal.ZERO.setScale(2) : Money.round(totalPaid.subtract(financed));
        return new FinancingResult(
                Money.round(priceEur),
                Money.round(down),
                Money.round(financed),
                apr,
                months,
                payment,
                totalPaid,
                interest.max(BigDecimal.ZERO.setScale(2)),
                Money.round(totalPaid.add(down)));
    }

    /**
     * Dobanda platita in primele `payments` rate, calculata pe soldul real (fara rotunjirile ratei). Folosita de TCO
     * cand masina se vinde dupa mai putine luni decat durata creditului: soldul ramas se achita din vanzare, nu este cost.
     */
    public static BigDecimal interestInFirstPayments(BigDecimal financedAmountEur, BigDecimal aprPercent, int months, int payments) {
        if (financedAmountEur.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal rate = monthlyRate(aprPercent);
        BigDecimal installment = exactPayment(financedAmountEur, rate, months);
        BigDecimal balance = financedAmountEur;
        BigDecimal interest = BigDecimal.ZERO;
        for (int i = 0; i < Math.min(payments, months); i++) {
            BigDecimal monthlyInterest = balance.multiply(rate, Money.PRECISION);
            interest = interest.add(monthlyInterest);
            balance = balance.add(monthlyInterest).subtract(installment);
        }
        return Money.round(interest);
    }

    private static BigDecimal payment(BigDecimal financed, BigDecimal aprPercent, int months) {
        if (financed.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return Money.round(exactPayment(financed, monthlyRate(aprPercent), months));
    }

    private static BigDecimal monthlyRate(BigDecimal aprPercent) {
        return aprPercent.divide(TWELVE_HUNDRED, Money.PRECISION);
    }

    // Anuitatea: P * r * (1+r)^n / ((1+r)^n - 1); fara dobanda, suma se imparte egal.
    private static BigDecimal exactPayment(BigDecimal financed, BigDecimal rate, int months) {
        if (rate.signum() == 0) {
            return financed.divide(BigDecimal.valueOf(months), Money.PRECISION);
        }
        BigDecimal growth = BigDecimal.ONE.add(rate).pow(months, Money.PRECISION);
        return financed.multiply(rate).multiply(growth).divide(growth.subtract(BigDecimal.ONE), Money.PRECISION);
    }
}
