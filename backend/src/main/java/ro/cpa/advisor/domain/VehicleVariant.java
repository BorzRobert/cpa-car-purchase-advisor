package ro.cpa.advisor.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * O versiune concreta de vehicul din catalog (motor + dotare) cu preturile si consumurile ei ORIENTATIVE.
 * Campurile de cost sunt intrarile calculatorului TCO din Faza 5; nimic de aici nu se citeste din memoria modelului.
 */
public record VehicleVariant(
        String id,
        String name,
        Powertrain powertrain,
        int priceEur,
        int powerKw,
        BigDecimal consumptionLitersPer100Km,
        BigDecimal consumptionKwhPer100Km,
        Integer electricRangeKm,
        BigDecimal batteryKwh,
        int co2GramsPerKm,
        int annualInsuranceEur,
        int annualRoadTaxEur,
        int annualMaintenanceEur,
        int residualValuePercentAfter5Years,
        LocalDate dataAsOf) {}
