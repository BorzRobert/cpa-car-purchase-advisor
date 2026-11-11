package ro.cpa.advisor.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ro.cpa.advisor.domain.BodyType;
import ro.cpa.advisor.domain.BudgetKind;
import ro.cpa.advisor.domain.BuyerProfile;
import ro.cpa.advisor.domain.ConditionPreference;
import ro.cpa.advisor.domain.FinancingMode;
import ro.cpa.advisor.domain.Powertrain;
import ro.cpa.advisor.domain.SessionPrincipal;
import ro.cpa.advisor.service.ProfileService;

/** Profilul cumparatorului: o resursa singulara per sesiune, inlocuita integral la fiecare PUT. */
@RestController
@RequestMapping(value = "/api/v1/profile", produces = "application/json")
@Tag(name = "Profile")
public class ProfileController {

    private final ProfileService profiles;

    public ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    /** Toate campurile sunt optionale: profilul se completeaza treptat. Limitele fine se aplica in domeniu. */
    public record ProfileRequest(
            @PositiveOrZero BigDecimal budgetAmount,
            BudgetKind budgetKind,
            FinancingMode financingMode,
            @Min(0) @Max(200_000) Integer yearlyKm,
            @Min(0) @Max(100) Integer cityPercent,
            @Min(1) @Max(9) Integer passengers,
            @Size(max = 5) List<@NotNull BodyType> bodyTypes,
            @Size(max = 5) List<@NotNull Powertrain> powertrains,
            Boolean homeCharging,
            ConditionPreference condition,
            @Size(max = BuyerProfile.MAX_ITEMS) List<@NotNull @Size(max = BuyerProfile.MAX_ITEM_CHARS) String> mustHaves,
            @Size(max = BuyerProfile.MAX_ITEMS) List<@NotNull @Size(max = BuyerProfile.MAX_ITEM_CHARS) String> dealBreakers) {

        BuyerProfile toDomain() {
            return new BuyerProfile(budgetAmount, budgetKind, financingMode, yearlyKm, cityPercent, passengers, bodyTypes,
                    powertrains, homeCharging, condition, mustHaves, dealBreakers, null);
        }
    }

    @GetMapping
    @Operation(summary = "Get the buyer profile", description = "An empty profile (all fields null, `updatedAt` null) if nothing was saved yet.")
    public BuyerProfile get(@AuthenticationPrincipal SessionPrincipal principal) {
        return profiles.get(principal.sessionId());
    }

    @PutMapping
    @Operation(summary = "Replace the buyer profile", description = "Send the whole profile; fields left out are cleared.")
    public BuyerProfile replace(@AuthenticationPrincipal SessionPrincipal principal, @RequestBody @Valid ProfileRequest request) {
        return profiles.replace(principal.sessionId(), request.toDomain());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Clear the buyer profile")
    public void clear(@AuthenticationPrincipal SessionPrincipal principal) {
        profiles.clear(principal.sessionId());
    }
}
