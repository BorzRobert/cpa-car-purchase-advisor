package ro.cpa.advisor.service;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.cpa.advisor.domain.BuyerProfile;
import ro.cpa.advisor.domain.ProfilePatch;
import ro.cpa.advisor.repository.BuyerProfileEntity;
import ro.cpa.advisor.repository.BuyerProfileRepository;

/** Profilul cumparatorului: cel mult unul per sesiune, creat la prima salvare. */
@Service
public class ProfileService {

    private final BuyerProfileRepository profiles;
    private final Clock clock;

    public ProfileService(BuyerProfileRepository profiles, Clock clock) {
        this.profiles = profiles;
        this.clock = clock;
    }

    /** O sesiune care n-a completat nimic primeste un profil gol, nu 404: pentru UI „fara profil” e o stare normala. */
    @Transactional(readOnly = true)
    public BuyerProfile get(UUID sessionId) {
        return profiles.findById(sessionId).map(BuyerProfileEntity::toDomain).orElseGet(BuyerProfile::empty);
    }

    @Transactional
    public BuyerProfile replace(UUID sessionId, BuyerProfile profile) {
        BuyerProfileEntity entity = profiles.findById(sessionId).orElseGet(() -> new BuyerProfileEntity(sessionId));
        entity.apply(profile, clock.instant());
        return profiles.save(entity).toDomain();
    }

    /** Modificare partiala (folosita de agent): ce lipseste din patch ramane cum era; rezultatul trece din nou prin reguli. */
    @Transactional
    public BuyerProfile merge(UUID sessionId, ProfilePatch patch) {
        return replace(sessionId, get(sessionId).mergedWith(patch));
    }

    @Transactional
    public void clear(UUID sessionId) {
        profiles.deleteById(sessionId);
    }
}
