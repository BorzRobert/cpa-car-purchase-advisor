package ro.cpa.advisor.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import ro.cpa.advisor.domain.BodyType;
import ro.cpa.advisor.domain.BudgetKind;
import ro.cpa.advisor.domain.BuyerProfile;
import ro.cpa.advisor.domain.ConditionPreference;
import ro.cpa.advisor.domain.FinancingMode;
import ro.cpa.advisor.domain.Powertrain;

/** Profilul unei sesiuni: cheia primara este chiar id-ul sesiunii (cel mult un profil per sesiune). */
@Entity
@Table(name = "buyer_profile")
public class BuyerProfileEntity {

    @Id
    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "budget_amount", precision = 12, scale = 2)
    private BigDecimal budgetAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_kind", length = 10)
    private BudgetKind budgetKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "financing_mode", length = 10)
    private FinancingMode financingMode;

    @Column(name = "yearly_km")
    private Integer yearlyKm;

    @Column(name = "city_percent")
    private Integer cityPercent;

    private Integer passengers;

    @Convert(converter = BodyTypeListConverter.class)
    @Column(name = "body_types", length = 200)
    private List<BodyType> bodyTypes;

    @Convert(converter = PowertrainListConverter.class)
    @Column(length = 200)
    private List<Powertrain> powertrains;

    @Column(name = "home_charging")
    private Boolean homeCharging;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_pref", length = 10)
    private ConditionPreference condition;

    @Convert(converter = TextListConverter.class)
    @Column(name = "must_haves", length = 1000)
    private List<String> mustHaves;

    @Convert(converter = TextListConverter.class)
    @Column(name = "deal_breakers", length = 1000)
    private List<String> dealBreakers;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BuyerProfileEntity() {}

    public BuyerProfileEntity(UUID sessionId) {
        this.sessionId = sessionId;
    }

    /** Inlocuieste tot continutul: profilul se trimite intreg, ca un camp sters in formular sa dispara si din baza. */
    public void apply(BuyerProfile profile, Instant now) {
        this.budgetAmount = profile.budgetAmount();
        this.budgetKind = profile.budgetKind();
        this.financingMode = profile.financingMode();
        this.yearlyKm = profile.yearlyKm();
        this.cityPercent = profile.cityPercent();
        this.passengers = profile.passengers();
        this.bodyTypes = profile.bodyTypes();
        this.powertrains = profile.powertrains();
        this.homeCharging = profile.homeCharging();
        this.condition = profile.condition();
        this.mustHaves = profile.mustHaves();
        this.dealBreakers = profile.dealBreakers();
        this.updatedAt = now;
    }

    public BuyerProfile toDomain() {
        return new BuyerProfile(budgetAmount, budgetKind, financingMode, yearlyKm, cityPercent, passengers, bodyTypes,
                powertrains, homeCharging, condition, mustHaves, dealBreakers, updatedAt);
    }
}
