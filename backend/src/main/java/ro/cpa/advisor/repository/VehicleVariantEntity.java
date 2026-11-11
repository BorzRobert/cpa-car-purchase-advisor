package ro.cpa.advisor.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import ro.cpa.advisor.domain.Powertrain;
import ro.cpa.advisor.domain.VehicleVariant;

@Entity
@Table(name = "vehicle_variant")
public class VehicleVariantEntity {

    @Id
    private String id;

    @Column(name = "vehicle_id", nullable = false, length = 60)
    private String vehicleId;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Powertrain powertrain;

    @Column(name = "price_eur", nullable = false)
    private int priceEur;

    @Column(name = "power_kw", nullable = false)
    private int powerKw;

    @Column(name = "consumption_l_100km", precision = 4, scale = 1)
    private BigDecimal consumptionLitersPer100Km;

    @Column(name = "consumption_kwh_100km", precision = 4, scale = 1)
    private BigDecimal consumptionKwhPer100Km;

    @Column(name = "electric_range_km")
    private Integer electricRangeKm;

    @Column(name = "battery_kwh", precision = 5, scale = 1)
    private BigDecimal batteryKwh;

    @Column(name = "co2_g_km", nullable = false)
    private int co2GramsPerKm;

    @Column(name = "annual_insurance_eur", nullable = false)
    private int annualInsuranceEur;

    @Column(name = "annual_road_tax_eur", nullable = false)
    private int annualRoadTaxEur;

    @Column(name = "annual_maintenance_eur", nullable = false)
    private int annualMaintenanceEur;

    @Column(name = "residual_value_pct_5y", nullable = false)
    private int residualValuePercentAfter5Years;

    @Column(name = "data_as_of", nullable = false)
    private LocalDate dataAsOf;

    protected VehicleVariantEntity() {}

    public String getId() {
        return id;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public LocalDate getDataAsOf() {
        return dataAsOf;
    }

    public VehicleVariant toDomain() {
        return new VehicleVariant(id, name, powertrain, priceEur, powerKw, consumptionLitersPer100Km,
                consumptionKwhPer100Km, electricRangeKm, batteryKwh, co2GramsPerKm, annualInsuranceEur, annualRoadTaxEur,
                annualMaintenanceEur, residualValuePercentAfter5Years, dataAsOf);
    }
}
