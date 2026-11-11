package ro.cpa.advisor.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * O varianta de vehicul, aplatizata cu modelul ei, in forma in care o vad si modelul (rezultat de unealta), si UI-ul
 * (cardul din evenimentul `vehicles`). Toate cifrele vin din catalog si sunt ORIENTATIVE: orice afisare le insoteste
 * de `dataAsOf`.
 */
public record VehicleCard(
        String variantId,
        String vehicleId,
        String make,
        String model,
        String variantName,
        BodyType bodyType,
        Powertrain powertrain,
        int priceEur,
        int seats,
        int bootLitres,
        Integer euroNcapStars,
        BigDecimal consumptionLitersPer100Km,
        BigDecimal consumptionKwhPer100Km,
        Integer electricRangeKm,
        int co2GramsPerKm,
        int annualInsuranceEur,
        int annualRoadTaxEur,
        int annualMaintenanceEur,
        int residualValuePercentAfter5Years,
        String reliabilityNotes,
        LocalDate dataAsOf) {

    public static VehicleCard of(Vehicle vehicle, VehicleVariant variant) {
        return new VehicleCard(
                variant.id(),
                vehicle.id(),
                vehicle.make(),
                vehicle.model(),
                variant.name(),
                vehicle.bodyType(),
                variant.powertrain(),
                variant.priceEur(),
                vehicle.seats(),
                vehicle.bootLitres(),
                vehicle.euroNcapStars(),
                variant.consumptionLitersPer100Km(),
                variant.consumptionKwhPer100Km(),
                variant.electricRangeKm(),
                variant.co2GramsPerKm(),
                variant.annualInsuranceEur(),
                variant.annualRoadTaxEur(),
                variant.annualMaintenanceEur(),
                variant.residualValuePercentAfter5Years(),
                vehicle.reliabilityNotes(),
                variant.dataAsOf());
    }
}
