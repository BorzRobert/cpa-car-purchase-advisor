package ro.cpa.advisor.ai;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import ro.cpa.advisor.domain.BuyerProfile;
import ro.cpa.advisor.domain.ProfilePatch;
import ro.cpa.advisor.domain.VehicleCard;
import ro.cpa.advisor.domain.VehicleFilter;

/**
 * Ce poate face consilierul in numele cumparatorului, ca interfata proprie stratului `ai`. Straturile nu permit ca `ai`
 * sa apeleze serviciile (`service` poate fi folosit doar de `api`), deci sensul dependentei este inversat: `ai` isi
 * declara nevoile aici, iar un serviciu le implementeaza. Uneltele nu primesc niciodata id-ul sesiunii de la model:
 * il pun ele, din contextul cererii, deci modelul nu poate atinge datele altei sesiuni.
 */
public interface AdvisorToolbox {

    /** Toate variantele care trec filtrul, cele mai ieftine primele. */
    List<VehicleCard> searchVehicles(VehicleFilter filter);

    /** Variantele cerute dupa id, in ordinea ceruta; id-urile necunoscute lipsesc din rezultat. */
    List<VehicleCard> variants(List<String> variantIds);

    /** Salveaza modificarea si intoarce profilul rezultat, plus campurile schimbate (cu valorile curatate). */
    ProfileChange updateProfile(UUID sessionId, ProfilePatch patch);

    void addToShortlist(UUID sessionId, String variantId, String note);

    List<VehicleCard> shortlist(UUID sessionId);

    record ProfileChange(BuyerProfile profile, Map<String, Object> changed) {}
}
