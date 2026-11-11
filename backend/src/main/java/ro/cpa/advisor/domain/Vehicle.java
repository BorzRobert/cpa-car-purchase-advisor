package ro.cpa.advisor.domain;

import java.util.List;

/** Un model din catalog cu variantele lui (sau doar cu cele care au trecut de un filtru). */
public record Vehicle(
        String id,
        String make,
        String model,
        BodyType bodyType,
        int seats,
        int bootLitres,
        Integer euroNcapStars,
        String reliabilityNotes,
        List<VehicleVariant> variants) {

    public Vehicle withVariants(List<VehicleVariant> filtered) {
        return new Vehicle(id, make, model, bodyType, seats, bootLitres, euroNcapStars, reliabilityNotes, filtered);
    }
}
