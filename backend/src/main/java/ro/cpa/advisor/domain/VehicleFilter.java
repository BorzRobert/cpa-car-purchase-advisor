package ro.cpa.advisor.domain;

import java.util.List;

/** Criteriile de cautare in catalog; un camp `null` inseamna „fara restrictie”. Regula de potrivire sta aici, nu in SQL. */
public record VehicleFilter(BodyType bodyType, Powertrain powertrain, Integer maxPriceEur, Integer minSeats) {

    public VehicleFilter {
        if (maxPriceEur != null && maxPriceEur < 0) {
            throw new InvalidInputException("maxPriceEur must not be negative.");
        }
        if (minSeats != null && minSeats < 1) {
            throw new InvalidInputException("minSeats must be at least 1.");
        }
    }

    /** Modelul fara variantele care nu trec filtrul; `null` daca nu ramane nicio varianta sau modelul nu se potriveste. */
    public Vehicle apply(Vehicle vehicle) {
        if (bodyType != null && vehicle.bodyType() != bodyType) {
            return null;
        }
        if (minSeats != null && vehicle.seats() < minSeats) {
            return null;
        }
        List<VehicleVariant> matching = vehicle.variants().stream()
                .filter(v -> powertrain == null || v.powertrain() == powertrain)
                .filter(v -> maxPriceEur == null || v.priceEur() <= maxPriceEur)
                .toList();
        return matching.isEmpty() ? null : vehicle.withVariants(matching);
    }
}
