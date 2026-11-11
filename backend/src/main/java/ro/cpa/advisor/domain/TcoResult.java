package ro.cpa.advisor.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Costul total de detinere pe `years` ani. `totalCostEur` = depreciere + energie + asigurare + taxa + mentenanta + dobanda
 * (adica pretul de achizitie minus valoarea la revanzare, plus costurile curente): banii „pierduti” efectiv, nu pretul
 * masinii. `assumptions` enumera tot ce s-a presupus, ca modelul sa le poata spune cumparatorului.
 */
public record TcoResult(
        String variantId,
        int years,
        int kmPerYear,
        BigDecimal purchasePriceEur,
        BigDecimal resaleValueEur,
        BigDecimal depreciationEur,
        BigDecimal energyEur,
        BigDecimal insuranceEur,
        BigDecimal roadTaxEur,
        BigDecimal maintenanceEur,
        BigDecimal interestEur,
        BigDecimal totalCostEur,
        BigDecimal costPerKmEur,
        BigDecimal costPerMonthEur,
        BigDecimal annualRunningCostEur,
        FinancingResult financing,
        List<String> assumptions) {}
