package ro.cpa.advisor.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleVariantRepository extends JpaRepository<VehicleVariantEntity, String> {

    List<VehicleVariantEntity> findByVehicleId(String vehicleId);
}
