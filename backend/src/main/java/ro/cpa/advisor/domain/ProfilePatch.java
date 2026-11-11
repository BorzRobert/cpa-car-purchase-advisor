package ro.cpa.advisor.domain;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * O modificare partiala a profilului, ceruta de agent dupa ce cumparatorul a spus ceva nou: un camp `null` inseamna
 * „nu se schimba”, o lista data INLOCUIESTE lista veche. Valorile trec prin aceleasi reguli ca profilul completat din
 * formular (`BuyerProfile`), deci textul liber ajunge curatat si limitat, indiferent de cine l-a scris.
 */
public record ProfilePatch(
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
        List<String> dealBreakers) {

    /** Campurile modificate, cu numele din profil, pentru evenimentul `profile` (panoul lateral). Gol = nimic de schimbat. */
    public Map<String, Object> changes() {
        Map<String, Object> changes = new LinkedHashMap<>();
        put(changes, "budgetAmount", budgetAmount);
        put(changes, "budgetKind", budgetKind);
        put(changes, "financingMode", financingMode);
        put(changes, "yearlyKm", yearlyKm);
        put(changes, "cityPercent", cityPercent);
        put(changes, "passengers", passengers);
        put(changes, "bodyTypes", bodyTypes);
        put(changes, "powertrains", powertrains);
        put(changes, "homeCharging", homeCharging);
        put(changes, "condition", condition);
        put(changes, "mustHaves", mustHaves);
        put(changes, "dealBreakers", dealBreakers);
        return changes;
    }

    private static void put(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }
}
