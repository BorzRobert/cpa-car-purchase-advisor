package ro.cpa.advisor.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import ro.cpa.advisor.domain.BodyType;
import ro.cpa.advisor.domain.Vehicle;
import ro.cpa.advisor.domain.VehicleVariant;

/** Un model din catalogul curatat; randurile vin din migrarile Flyway, aplicatia doar le citeste. */
@Entity
@Table(name = "vehicle")
public class VehicleEntity {

    @Id
    private String id;

    @Column(nullable = false, length = 40)
    private String make;

    @Column(nullable = false, length = 60)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "body_type", nullable = false, length = 20)
    private BodyType bodyType;

    @Column(nullable = false)
    private int seats;

    @Column(name = "boot_litres", nullable = false)
    private int bootLitres;

    @Column(name = "euro_ncap_stars")
    private Integer euroNcapStars;

    @Column(name = "reliability_notes", nullable = false, length = 500)
    private String reliabilityNotes;

    protected VehicleEntity() {}

    public String getId() {
        return id;
    }

    public Vehicle toDomain(List<VehicleVariant> variants) {
        return new Vehicle(id, make, model, bodyType, seats, bootLitres, euroNcapStars, reliabilityNotes, variants);
    }
}
