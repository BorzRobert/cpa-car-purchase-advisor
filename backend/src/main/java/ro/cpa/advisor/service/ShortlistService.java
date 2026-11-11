package ro.cpa.advisor.service;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.cpa.advisor.config.CpaProperties;
import ro.cpa.advisor.domain.ConflictException;
import ro.cpa.advisor.domain.LimitReachedException;
import ro.cpa.advisor.domain.NotFoundException;
import ro.cpa.advisor.domain.ShortlistItem;
import ro.cpa.advisor.domain.TextRules;
import ro.cpa.advisor.domain.Vehicle;
import ro.cpa.advisor.domain.VehicleVariant;
import ro.cpa.advisor.repository.ShortlistItemEntity;
import ro.cpa.advisor.repository.ShortlistItemRepository;
import ro.cpa.advisor.repository.VehicleEntity;
import ro.cpa.advisor.repository.VehicleRepository;
import ro.cpa.advisor.repository.VehicleVariantEntity;
import ro.cpa.advisor.repository.VehicleVariantRepository;

/** Lista scurta a unei sesiuni: variante din catalog salvate pentru comparatie ulterioara. */
@Service
public class ShortlistService {

    private final ShortlistItemRepository items;
    private final VehicleVariantRepository variants;
    private final VehicleRepository vehicles;
    private final CpaProperties properties;
    private final Clock clock;

    public ShortlistService(
            ShortlistItemRepository items,
            VehicleVariantRepository variants,
            VehicleRepository vehicles,
            CpaProperties properties,
            Clock clock) {
        this.items = items;
        this.variants = variants;
        this.vehicles = vehicles;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ShortlistItem> list(UUID sessionId) {
        return toDomain(items.findBySessionIdOrderByCreatedAtDesc(sessionId));
    }

    @Transactional
    public ShortlistItem add(UUID sessionId, String variantId, String rawNote) {
        if (!variants.existsById(variantId)) {
            throw new NotFoundException("Vehicle variant not found.");
        }
        if (items.existsBySessionIdAndVariantId(sessionId, variantId)) {
            throw new ConflictException("already-in-shortlist", "This vehicle is already in the shortlist.");
        }
        if (items.countBySessionId(sessionId) >= properties.limits().maxShortlistItems()) {
            throw new LimitReachedException("The shortlist is full. Remove a vehicle to add another.");
        }
        String note = TextRules.singleLine(rawNote, ShortlistItem.MAX_NOTE_CHARS, "note");
        ShortlistItemEntity saved = items.save(new ShortlistItemEntity(UUID.randomUUID(), sessionId, variantId, note, clock.instant()));
        return toDomain(List.of(saved)).getFirst();
    }

    @Transactional
    public void remove(UUID sessionId, UUID itemId) {
        items.delete(items.findByIdAndSessionId(itemId, sessionId).orElseThrow(() -> new NotFoundException("Shortlist item not found.")));
    }

    // Doua interogari pentru toate elementele, nu una per element: lista are cel mult `maxShortlistItems` randuri.
    private List<ShortlistItem> toDomain(List<ShortlistItemEntity> entities) {
        Map<String, VehicleVariantEntity> variantsById = variants
                .findAllById(entities.stream().map(ShortlistItemEntity::getVariantId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(VehicleVariantEntity::getId, Function.identity()));
        Map<String, VehicleEntity> vehiclesById = vehicles
                .findAllById(variantsById.values().stream().map(VehicleVariantEntity::getVehicleId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(VehicleEntity::getId, Function.identity()));

        return entities.stream()
                .map(item -> {
                    VehicleVariantEntity variant = variantsById.get(item.getVariantId());
                    VehicleVariant variantView = variant.toDomain();
                    Vehicle vehicle = vehiclesById.get(variant.getVehicleId()).toDomain(List.of(variantView));
                    return new ShortlistItem(item.getId(), vehicle, item.getNote(), item.getCreatedAt());
                })
                .toList();
    }
}
