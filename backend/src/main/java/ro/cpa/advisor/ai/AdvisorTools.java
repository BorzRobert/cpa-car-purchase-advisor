package ro.cpa.advisor.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import ro.cpa.advisor.domain.AdvisorEvent;
import ro.cpa.advisor.domain.BodyType;
import ro.cpa.advisor.domain.BudgetKind;
import ro.cpa.advisor.domain.ConditionPreference;
import ro.cpa.advisor.domain.ConflictException;
import ro.cpa.advisor.domain.FinancingMode;
import ro.cpa.advisor.domain.InvalidInputException;
import ro.cpa.advisor.domain.LimitReachedException;
import ro.cpa.advisor.domain.NotFoundException;
import ro.cpa.advisor.domain.Powertrain;
import ro.cpa.advisor.domain.ProfilePatch;
import ro.cpa.advisor.domain.StreamClosedException;
import ro.cpa.advisor.domain.VehicleCard;
import ro.cpa.advisor.domain.VehicleFilter;

/**
 * Uneltele deterministe ale consilierului. Descrierile sunt pentru model (engleza): ele sunt singura sursa dupa care
 * alege ce unealta sa cheme. Fiecare unealta:
 * <ul>
 *   <li>lucreaza doar pe sesiunea din contextul cererii (modelul nu poate cere datele altcuiva);
 *   <li>anunta UI-ul cu evenimentul `tool` (pornit/terminat) si, unde are sens, cu `vehicles` sau `profile`;
 *   <li>transforma o eroare de domeniu in rezultat `{"error": ...}`, ca modelul sa o poata explica, nu sa pice cererea.
 * </ul>
 */
@Component
public class AdvisorTools {

    private static final Logger log = LoggerFactory.getLogger(AdvisorTools.class);
    private static final int DEFAULT_RESULTS = 8;
    private static final int MAX_RESULTS = 10;
    private static final int MAX_COMPARED = 4;
    private static final String INDICATIVE = "Prices and costs are indicative figures from a curated catalogue, valid on dataAsOf; present them as such, never as offers.";

    private final AdvisorToolbox toolbox;

    public AdvisorTools(AdvisorToolbox toolbox) {
        this.toolbox = toolbox;
    }

    /** Ce vede modelul dupa o cautare sau o comparatie. */
    public record VehiclesResult(LocalDate dataAsOf, int totalMatches, int shown, String note, List<VehicleCard> vehicles) {}

    @Tool(
            name = "updateBuyerProfile",
            description = """
                    Save what the buyer just told you about their needs, so it is remembered and shown in the profile panel.
                    Call it as soon as the buyer states or changes a need; send only the fields that changed.
                    List fields (bodyTypes, powertrains, mustHaves, dealBreakers) REPLACE the previous list, so include the
                    existing items you want to keep. Never guess a value the buyer did not say.""")
    public Object updateBuyerProfile(
            @ToolParam(required = false, description = "Budget amount in EUR (a total price or a monthly payment, see budgetKind).") Double budgetAmount,
            @ToolParam(required = false, description = "Whether budgetAmount is the TOTAL price or a MONTHLY payment. Required with budgetAmount.") BudgetKind budgetKind,
            @ToolParam(required = false, description = "CASH or FINANCE.") FinancingMode financingMode,
            @ToolParam(required = false, description = "Kilometres driven per year (0-200000).") Integer yearlyKm,
            @ToolParam(required = false, description = "Share of city driving, 0-100 (the rest is highway/country roads).") Integer cityPercent,
            @ToolParam(required = false, description = "Number of people usually travelling (1-9).") Integer passengers,
            @ToolParam(required = false, description = "Preferred body types.") List<BodyType> bodyTypes,
            @ToolParam(required = false, description = "Preferred powertrains.") List<Powertrain> powertrains,
            @ToolParam(required = false, description = "Whether the buyer can charge at home (relevant for electric and plug-in hybrid).") Boolean homeCharging,
            @ToolParam(required = false, description = "NEW, USED or ANY.") ConditionPreference condition,
            @ToolParam(required = false, description = "Short must-have requirements, at most 10 items of 80 characters.") List<String> mustHaves,
            @ToolParam(required = false, description = "Short deal-breakers, at most 10 items of 80 characters.") List<String> dealBreakers,
            ToolContext context) {
        ProfilePatch patch = new ProfilePatch(
                budgetAmount == null ? null : BigDecimal.valueOf(budgetAmount),
                budgetKind,
                financingMode,
                yearlyKm,
                cityPercent,
                passengers,
                bodyTypes,
                powertrains,
                homeCharging,
                condition,
                mustHaves,
                dealBreakers);
        return run(context, "updateBuyerProfile", "Actualizez profilul…", run -> {
            if (patch.changes().isEmpty()) {
                throw new InvalidInputException("Nothing to update: send at least one field.");
            }
            AdvisorToolbox.ProfileChange change = toolbox.updateProfile(run.sessionId(), patch);
            run.emit(new AdvisorEvent.ProfileUpdated(change.changed()));
            return new Outcome(Map.of("saved", true, "profile", change.profile().filledFields()), "Profil actualizat");
        });
    }

    @Tool(
            name = "searchVehicles",
            description = """
                    Search the vehicle catalogue. Returns up to `limit` matching variants, cheapest first, with price,
                    consumption, running-cost inputs, safety and reliability notes, and the dataAsOf date of the figures.
                    Use it for ANY question about which cars exist, their prices or specs; never answer those from memory.
                    The catalogue is small and curated: if nothing matches, say so and relax a criterion.""")
    public Object searchVehicles(
            @ToolParam(required = false, description = "Only this body type.") BodyType bodyType,
            @ToolParam(required = false, description = "Only this powertrain.") Powertrain powertrain,
            @ToolParam(required = false, description = "Maximum list price in EUR.") Integer maxPriceEur,
            @ToolParam(required = false, description = "Minimum number of seats.") Integer minSeats,
            @ToolParam(required = false, description = "How many results to return, 1-10 (default 8).") Integer limit,
            ToolContext context) {
        return run(context, "searchVehicles", "Caut mașini potrivite…", run -> {
            List<VehicleCard> matches = toolbox.searchVehicles(new VehicleFilter(bodyType, powertrain, maxPriceEur, minSeats));
            int wanted = limit == null ? DEFAULT_RESULTS : Math.max(1, Math.min(limit, MAX_RESULTS));
            List<VehicleCard> shown = matches.stream().limit(wanted).toList();
            run.emit(new AdvisorEvent.Vehicles(shown, newest(shown)));
            return new Outcome(
                    new VehiclesResult(newest(shown), matches.size(), shown.size(), INDICATIVE, shown),
                    "Am găsit " + matches.size() + (matches.size() == 1 ? " variantă" : " variante"));
        });
    }

    @Tool(
            name = "compareVehicles",
            description = """
                    Side-by-side data for 2 to 4 catalogue variants, by variantId (ids come from searchVehicles).
                    Use it when the buyer wants to compare specific cars. Unknown ids are listed in the note.""")
    public Object compareVehicles(
            @ToolParam(description = "Between 2 and 4 variant ids from the catalogue.") List<String> variantIds, ToolContext context) {
        return run(context, "compareVehicles", "Compar variantele…", run -> {
            if (variantIds == null || variantIds.size() < 2 || variantIds.size() > MAX_COMPARED) {
                throw new InvalidInputException("Provide between 2 and " + MAX_COMPARED + " variant ids.");
            }
            List<VehicleCard> cards = toolbox.variants(variantIds);
            List<String> missing = variantIds.stream().filter(id -> cards.stream().noneMatch(c -> c.variantId().equals(id))).toList();
            if (cards.size() < 2) {
                throw new NotFoundException("Fewer than two of the given variant ids exist in the catalogue; use searchVehicles to find valid ids.");
            }
            run.emit(new AdvisorEvent.Vehicles(cards, newest(cards)));
            String note = INDICATIVE + (missing.isEmpty() ? "" : " Unknown ids (not compared): " + missing);
            return new Outcome(new VehiclesResult(newest(cards), cards.size(), cards.size(), note, cards), "Am comparat " + cards.size() + " variante");
        });
    }

    @Tool(
            name = "addToShortlist",
            description = "Save a catalogue variant (by variantId) to the buyer's shortlist, with an optional short note. Call it only when the buyer asks to save or shortlist a car.")
    public Object addToShortlist(
            @ToolParam(description = "Variant id from the catalogue.") String variantId,
            @ToolParam(required = false, description = "Optional short note (at most 500 characters).") String note,
            ToolContext context) {
        return run(context, "addToShortlist", "Adaug în lista scurtă…", run -> {
            toolbox.addToShortlist(run.sessionId(), variantId, note);
            return new Outcome(Map.of("added", true, "variantId", variantId), "Adăugat în lista scurtă");
        });
    }

    @Tool(name = "getShortlist", description = "Read the buyer's current shortlist (saved variants, newest first).")
    public Object getShortlist(ToolContext context) {
        return run(context, "getShortlist", "Citesc lista scurtă…", run -> {
            List<VehicleCard> cards = toolbox.shortlist(run.sessionId());
            if (!cards.isEmpty()) {
                run.emit(new AdvisorEvent.Vehicles(cards, newest(cards)));
            }
            return new Outcome(new VehiclesResult(newest(cards), cards.size(), cards.size(), INDICATIVE, cards), "Lista scurtă: " + cards.size() + " variante");
        });
    }

    // ---------- infrastructura comuna ----------

    private record Outcome(Object result, String summary) {}

    private static LocalDate newest(List<VehicleCard> cards) {
        return cards.stream().map(VehicleCard::dataAsOf).max(Comparator.naturalOrder()).orElse(null);
    }

    /**
     * Ruleaza o unealta cu anuntarea ei in UI. O eroare de domeniu (date gresite, id necunoscut, plafon) devine un
     * rezultat pe care modelul il poate explica; orice alta eroare ramane generica, fara detalii interne spre model.
     */
    private Object run(ToolContext context, String tool, String startSummary, Function<ToolRun, Outcome> body) {
        ToolRun run = (ToolRun) context.getContext().get(ToolRun.CONTEXT_KEY);
        if (!run.allowCall()) {
            return Map.of("error", "Tool call limit reached for this answer. Answer with what you already have.");
        }
        run.emit(new AdvisorEvent.Tool(tool, AdvisorEvent.Tool.STARTED, startSummary));
        try {
            Outcome outcome = body.apply(run);
            run.emit(new AdvisorEvent.Tool(tool, AdvisorEvent.Tool.FINISHED, outcome.summary()));
            return outcome.result();
        } catch (StreamClosedException closed) {
            throw closed;
        } catch (InvalidInputException | NotFoundException | ConflictException | LimitReachedException expected) {
            run.emit(new AdvisorEvent.Tool(tool, AdvisorEvent.Tool.FINISHED, "Operațiunea nu a reușit"));
            return Map.of("error", expected.getMessage());
        } catch (RuntimeException unexpected) {
            log.warn("Tool {} failed: {}", tool, unexpected.getClass().getSimpleName());
            run.emit(new AdvisorEvent.Tool(tool, AdvisorEvent.Tool.FINISHED, "Operațiunea nu a reușit"));
            return Map.of("error", "The tool failed unexpectedly; continue without it.");
        }
    }
}
