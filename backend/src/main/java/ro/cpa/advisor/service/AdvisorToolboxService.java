package ro.cpa.advisor.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import ro.cpa.advisor.ai.AdvisorToolbox;
import ro.cpa.advisor.domain.BuyerProfile;
import ro.cpa.advisor.domain.ProfilePatch;
import ro.cpa.advisor.domain.VehicleCard;
import ro.cpa.advisor.domain.VehicleFilter;

/**
 * Leaga uneltele consilierului (`ai`) de serviciile aplicatiei. Nu adauga reguli: profilul, catalogul si lista scurta
 * isi pastreaza validarile si limitele, indiferent daca sunt apelate de un controller sau de model.
 */
@Service
public class AdvisorToolboxService implements AdvisorToolbox {

    private final VehicleService vehicles;
    private final ProfileService profiles;
    private final ShortlistService shortlist;

    public AdvisorToolboxService(VehicleService vehicles, ProfileService profiles, ShortlistService shortlist) {
        this.vehicles = vehicles;
        this.profiles = profiles;
        this.shortlist = shortlist;
    }

    @Override
    public List<VehicleCard> searchVehicles(VehicleFilter filter) {
        return vehicles.searchCards(filter);
    }

    @Override
    public List<VehicleCard> variants(List<String> variantIds) {
        return vehicles.cards(variantIds);
    }

    @Override
    public ProfileChange updateProfile(UUID sessionId, ProfilePatch patch) {
        BuyerProfile saved = profiles.merge(sessionId, patch);
        // Evenimentul poarta valorile CURATATE (cum s-au salvat), nu textul brut dat de model.
        Map<String, Object> savedFields = saved.filledFields();
        Map<String, Object> changed = new LinkedHashMap<>();
        for (String field : patch.changes().keySet()) {
            changed.put(field, savedFields.get(field));
        }
        return new ProfileChange(saved, changed);
    }

    @Override
    public void addToShortlist(UUID sessionId, String variantId, String note) {
        shortlist.add(sessionId, variantId, note);
    }

    @Override
    public List<VehicleCard> shortlist(UUID sessionId) {
        return shortlist.list(sessionId).stream()
                .map(item -> VehicleCard.of(item.vehicle(), item.vehicle().variants().getFirst()))
                .toList();
    }
}
