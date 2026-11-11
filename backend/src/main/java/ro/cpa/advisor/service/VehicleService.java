package ro.cpa.advisor.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.cpa.advisor.domain.NotFoundException;
import ro.cpa.advisor.domain.Vehicle;
import ro.cpa.advisor.domain.VehicleCard;
import ro.cpa.advisor.domain.VehicleCatalogue;
import ro.cpa.advisor.domain.VehicleFilter;
import ro.cpa.advisor.domain.VehicleVariant;
import ro.cpa.advisor.repository.VehicleEntity;
import ro.cpa.advisor.repository.VehicleRepository;
import ro.cpa.advisor.repository.VehicleVariantEntity;
import ro.cpa.advisor.repository.VehicleVariantRepository;

/**
 * Catalogul curatat (read-only). E mic si marginit prin migrari, deci incarcam tot si filtram in domeniu: raspunsul
 * nu are nevoie de paginare, iar regulile de potrivire se testeaza fara baza de date.
 */
@Service
public class VehicleService {

    private final VehicleRepository vehicles;
    private final VehicleVariantRepository variants;

    public VehicleService(VehicleRepository vehicles, VehicleVariantRepository variants) {
        this.vehicles = vehicles;
        this.variants = variants;
    }

    @Transactional(readOnly = true)
    public VehicleCatalogue search(VehicleFilter filter) {
        List<VehicleVariantEntity> allVariants = variants.findAll();
        Map<String, List<VehicleVariant>> byVehicle = allVariants.stream()
                .collect(Collectors.groupingBy(
                        VehicleVariantEntity::getVehicleId,
                        Collectors.mapping(VehicleVariantEntity::toDomain, Collectors.toList())));

        List<Vehicle> result = vehicles.findAll().stream()
                .map(v -> v.toDomain(sortedByPrice(byVehicle.getOrDefault(v.getId(), List.of()))))
                .map(filter::apply)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Vehicle::make).thenComparing(Vehicle::model))
                .toList();

        LocalDate dataAsOf = allVariants.stream().map(VehicleVariantEntity::getDataAsOf).max(LocalDate::compareTo).orElse(null);
        return new VehicleCatalogue(dataAsOf, result);
    }

    @Transactional(readOnly = true)
    public Vehicle get(String vehicleId) {
        VehicleEntity vehicle = vehicles.findById(vehicleId).orElseThrow(() -> new NotFoundException("Vehicle not found."));
        List<VehicleVariant> found = variants.findByVehicleId(vehicleId).stream().map(VehicleVariantEntity::toDomain).toList();
        return vehicle.toDomain(sortedByPrice(found));
    }

    /** Toate variantele care trec filtrul, aplatizate in carduri, cele mai ieftine primele. */
    @Transactional(readOnly = true)
    public List<VehicleCard> searchCards(VehicleFilter filter) {
        return search(filter).vehicles().stream()
                .flatMap(vehicle -> vehicle.variants().stream().map(variant -> VehicleCard.of(vehicle, variant)))
                .sorted(Comparator.comparingInt(VehicleCard::priceEur).thenComparing(VehicleCard::variantId))
                .toList();
    }

    /** Cardurile variantelor cerute, in ordinea ceruta; id-urile necunoscute si dublurile lipsesc din rezultat. */
    @Transactional(readOnly = true)
    public List<VehicleCard> cards(List<String> variantIds) {
        Map<String, VehicleCard> byId = searchCards(new VehicleFilter(null, null, null, null)).stream()
                .collect(Collectors.toMap(VehicleCard::variantId, card -> card));
        return variantIds.stream().distinct().map(byId::get).filter(Objects::nonNull).toList();
    }

    private static List<VehicleVariant> sortedByPrice(List<VehicleVariant> list) {
        return list.stream().sorted(Comparator.comparingInt(VehicleVariant::priceEur)).toList();
    }
}
