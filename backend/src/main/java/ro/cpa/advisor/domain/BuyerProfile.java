package ro.cpa.advisor.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nevoile cumparatorului, completate treptat (de formular acum, de agent in Faza 5), deci orice camp poate lipsi.
 * Constructorul curata textul liber si verifica regulile de domeniu: profilul intra in prompt, deci nu primeste
 * nimic necurat. Listele nu sunt niciodata `null`.
 */
public record BuyerProfile(
        BigDecimal budgetAmount,
        BudgetKind budgetKind,
        FinancingMode financingMode,
        Integer yearlyKm,
        Integer cityPercent,
        Integer passengers,
        List<BodyType> bodyTypes,
        List<Powertrain> powertrains,
        Boolean homeCharging,
        ConditionPreference condition,
        List<String> mustHaves,
        List<String> dealBreakers,
        Instant updatedAt) {

    public static final int MAX_ITEMS = 10;
    public static final int MAX_ITEM_CHARS = 80;

    public BuyerProfile {
        if (budgetAmount != null && budgetAmount.signum() < 0) {
            throw new InvalidInputException("budgetAmount must not be negative.");
        }
        if (budgetAmount != null && budgetKind == null) {
            throw new InvalidInputException("budgetKind is required when budgetAmount is set.");
        }
        if (yearlyKm != null && (yearlyKm < 0 || yearlyKm > 200_000)) {
            throw new InvalidInputException("yearlyKm must be between 0 and 200000.");
        }
        if (cityPercent != null && (cityPercent < 0 || cityPercent > 100)) {
            throw new InvalidInputException("cityPercent must be between 0 and 100.");
        }
        if (passengers != null && (passengers < 1 || passengers > 9)) {
            throw new InvalidInputException("passengers must be between 1 and 9.");
        }
        bodyTypes = bodyTypes == null ? List.of() : bodyTypes.stream().distinct().toList();
        powertrains = powertrains == null ? List.of() : powertrains.stream().distinct().toList();
        mustHaves = TextRules.singleLineList(mustHaves, MAX_ITEMS, MAX_ITEM_CHARS, "mustHaves");
        dealBreakers = TextRules.singleLineList(dealBreakers, MAX_ITEMS, MAX_ITEM_CHARS, "dealBreakers");
    }

    /** Profilul unei sesiuni care nu a completat nimic inca. */
    public static BuyerProfile empty() {
        return new BuyerProfile(null, null, null, null, null, null, List.of(), List.of(), null, null, List.of(), List.of(), null);
    }

    public BuyerProfile withUpdatedAt(Instant instant) {
        return new BuyerProfile(budgetAmount, budgetKind, financingMode, yearlyKm, cityPercent, passengers, bodyTypes,
                powertrains, homeCharging, condition, mustHaves, dealBreakers, instant);
    }

    /** Profilul dupa o modificare partiala: campurile `null` din patch raman cum erau; rezultatul trece din nou prin reguli. */
    public BuyerProfile mergedWith(ProfilePatch patch) {
        return new BuyerProfile(
                patch.budgetAmount() != null ? patch.budgetAmount() : budgetAmount,
                patch.budgetKind() != null ? patch.budgetKind() : budgetKind,
                patch.financingMode() != null ? patch.financingMode() : financingMode,
                patch.yearlyKm() != null ? patch.yearlyKm() : yearlyKm,
                patch.cityPercent() != null ? patch.cityPercent() : cityPercent,
                patch.passengers() != null ? patch.passengers() : passengers,
                patch.bodyTypes() != null ? patch.bodyTypes() : bodyTypes,
                patch.powertrains() != null ? patch.powertrains() : powertrains,
                patch.homeCharging() != null ? patch.homeCharging() : homeCharging,
                patch.condition() != null ? patch.condition() : condition,
                patch.mustHaves() != null ? patch.mustHaves() : mustHaves,
                patch.dealBreakers() != null ? patch.dealBreakers() : dealBreakers,
                updatedAt);
    }

    /** Campurile completate, cu numele lor, in ordinea formularului (pentru evenimentul `profile`). */
    public Map<String, Object> filledFields() {
        Map<String, Object> fields = new LinkedHashMap<>();
        put(fields, "budgetAmount", budgetAmount);
        put(fields, "budgetKind", budgetKind);
        put(fields, "financingMode", financingMode);
        put(fields, "yearlyKm", yearlyKm);
        put(fields, "cityPercent", cityPercent);
        put(fields, "passengers", passengers);
        put(fields, "bodyTypes", bodyTypes.isEmpty() ? null : bodyTypes);
        put(fields, "powertrains", powertrains.isEmpty() ? null : powertrains);
        put(fields, "homeCharging", homeCharging);
        put(fields, "condition", condition);
        put(fields, "mustHaves", mustHaves.isEmpty() ? null : mustHaves);
        put(fields, "dealBreakers", dealBreakers.isEmpty() ? null : dealBreakers);
        return fields;
    }

    private static void put(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }
}
