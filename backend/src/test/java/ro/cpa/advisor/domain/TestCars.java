package ro.cpa.advisor.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Masini de test cu cifrele din catalogul de pornire (V3), ca testele de calcul sa se poata verifica cu mana. */
final class TestCars {

    static final EnergyPrices PRICES = new EnergyPrices(
            new BigDecimal("1.55"), new BigDecimal("1.60"), new BigDecimal("0.30"), new BigDecimal("0.55"), LocalDate.of(2026, 11, 11));

    private TestCars() {}

    static VehicleCard golf() {
        return card("golf-1-5-tsi-130", "Volkswagen", "Golf", BodyType.HATCHBACK, Powertrain.PETROL, 29900, 5, 380, 5, "5.6", null, 900, 90, 450, 50);
    }

    static VehicleCard sandero() {
        return card("sandero-tce-90", "Dacia", "Sandero", BodyType.HATCHBACK, Powertrain.PETROL, 14500, 5, 328, null, "5.6", null, 550, 40, 350, 48);
    }

    static VehicleCard spring() {
        return card("spring-electric-65", "Dacia", "Spring", BodyType.HATCHBACK, Powertrain.ELECTRIC, 16900, 4, 308, 1, null, "13.9", 500, 0, 250, 40);
    }

    static VehicleCard corolla() {
        return card("corolla-1-8-hybrid-140", "Toyota", "Corolla", BodyType.HATCHBACK, Powertrain.HYBRID, 28900, 5, 361, 5, "4.5", null, 850, 90, 380, 56);
    }

    static VehicleCard octavia() {
        return card("octavia-combi-2-0-tdi-150", "Skoda", "Octavia Combi", BodyType.ESTATE, Powertrain.DIESEL, 34500, 5, 640, 5, "4.8", null, 1000, 100, 490, 47);
    }

    static VehicleCard golfGte() {
        return card("golf-gte-phev", "Volkswagen", "Golf", BodyType.HATCHBACK, Powertrain.PLUG_IN_HYBRID, 41900, 5, 380, 5, "1.5", "14.5", 1200, 120, 520, 42);
    }

    static VehicleCard card(
            String variantId,
            String make,
            String model,
            BodyType body,
            Powertrain powertrain,
            int price,
            int seats,
            int boot,
            Integer stars,
            String litres,
            String kwh,
            int insurance,
            int tax,
            int maintenance,
            int residualPercent) {
        return new VehicleCard(
                variantId,
                variantId.split("-")[0],
                make,
                model,
                variantId,
                body,
                powertrain,
                price,
                seats,
                boot,
                stars,
                litres == null ? null : new BigDecimal(litres),
                kwh == null ? null : new BigDecimal(kwh),
                null,
                0,
                insurance,
                tax,
                maintenance,
                residualPercent,
                "Notă de test.",
                LocalDate.of(2026, 11, 11));
    }

    static BuyerProfile profile(BudgetKind kind, String budget, Integer km, Integer city, Integer passengers, Boolean home) {
        return new BuyerProfile(
                budget == null ? null : new BigDecimal(budget),
                kind,
                null,
                km,
                city,
                passengers,
                java.util.List.of(),
                java.util.List.of(),
                home,
                null,
                java.util.List.of(),
                java.util.List.of(),
                null);
    }
}
