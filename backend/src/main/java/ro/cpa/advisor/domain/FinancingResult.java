package ro.cpa.advisor.domain;

import java.math.BigDecimal;

/**
 * Rezultatul unui calcul de finantare (credit cu rata fixa). `aprPercent` este tratata ca dobanda nominala anuala,
 * capitalizata lunar, FARA comisioane si asigurari: nu este DAE. Sumele sunt rotunjite la cent.
 */
public record FinancingResult(
        BigDecimal priceEur,
        BigDecimal downPaymentEur,
        BigDecimal financedAmountEur,
        BigDecimal aprPercent,
        int months,
        BigDecimal monthlyPaymentEur,
        BigDecimal totalPaidInInstalmentsEur,
        BigDecimal totalInterestEur,
        BigDecimal totalOutlayWithDownPaymentEur) {}
