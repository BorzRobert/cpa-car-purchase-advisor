package ro.cpa.advisor.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
import ro.cpa.advisor.domain.BuyerProfile;
import ro.cpa.advisor.domain.ComparisonReport;
import ro.cpa.advisor.domain.ConditionPreference;
import ro.cpa.advisor.domain.ConflictException;
import ro.cpa.advisor.domain.EvVsIce;
import ro.cpa.advisor.domain.Financing;
import ro.cpa.advisor.domain.FinancingMode;
import ro.cpa.advisor.domain.FinancingResult;
import ro.cpa.advisor.domain.InvalidInputException;
import ro.cpa.advisor.domain.LimitReachedException;
import ro.cpa.advisor.domain.NotFoundException;
import ro.cpa.advisor.domain.Powertrain;
import ro.cpa.advisor.domain.ProfilePatch;
import ro.cpa.advisor.domain.ScoredVehicle;
import ro.cpa.advisor.domain.StreamClosedException;
import ro.cpa.advisor.domain.TcoCalculator;
import ro.cpa.advisor.domain.TcoResult;
import ro.cpa.advisor.domain.VehicleCard;
import ro.cpa.advisor.domain.VehicleFilter;
import ro.cpa.advisor.domain.VehicleScorer;
import tools.jackson.databind.json.JsonMapper;

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
    private static final int DEFAULT_RANKED = 5;
    private static final int MAX_RANKED = 8;
    private static final int DEFAULT_HORIZON = 8;
    private static final String INDICATIVE = "Prices and costs are indicative figures from a curated catalogue, valid on dataAsOf; present them as such, never as offers.";

    private final AdvisorToolbox toolbox;
    private final JsonMapper json;

    public AdvisorTools(AdvisorToolbox toolbox, JsonMapper json) {
        this.toolbox = toolbox;
        this.json = json;
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
                    Side-by-side comparison of 2 to 4 catalogue variants, by variantId (ids come from searchVehicles or
                    rankVehicles). Returns each car's data, a 0-100 score per criterion explained for THIS buyer's profile,
                    the total score, and a total-cost-of-ownership summary, plus the assumptions behind the numbers.
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
            ComparisonReport report = rank(run, cards, cards.size());
            run.emit(new AdvisorEvent.Compare(report));
            String note = INDICATIVE + (missing.isEmpty() ? "" : " Unknown ids (not compared): " + missing);
            return new Outcome(new ReportResult(note, report), "Am comparat " + cards.size() + " variante");
        });
    }

    @Tool(
            name = "rankVehicles",
            description = """
                    Rank catalogue variants for THIS buyer with explainable scores: price fit, running cost, space, safety,
                    reliability proxy and usage fit (0-100 each, fixed weights), plus total cost of ownership. Without
                    candidateIds it ranks the whole catalogue and returns the best `limit` (default 5). Use it to answer
                    "which car is best for me", and explain the recommendation from the per-criterion details. A criterion
                    the profile gives no data for is left out, not guessed: ask the buyer for what is missing.""")
    public Object rankVehicles(
            @ToolParam(required = false, description = "Variant ids to rank; omit to rank the whole catalogue.") List<String> candidateIds,
            @ToolParam(required = false, description = "How many top results to return, 1-8 (default 5).") Integer limit,
            ToolContext context) {
        return run(context, "rankVehicles", "Clasific variantele pentru tine…", run -> {
            List<VehicleCard> candidates = candidateIds == null || candidateIds.isEmpty()
                    ? toolbox.searchVehicles(new VehicleFilter(null, null, null, null))
                    : toolbox.variants(candidateIds);
            if (candidates.isEmpty()) {
                throw new NotFoundException("None of the given variant ids exist in the catalogue; use searchVehicles to find valid ids.");
            }
            int wanted = limit == null ? DEFAULT_RANKED : Math.max(1, Math.min(limit, MAX_RANKED));
            ComparisonReport report = rank(run, candidates, wanted);
            run.emit(new AdvisorEvent.Compare(report));
            return new Outcome(new ReportResult(INDICATIVE, report), "Am clasificat " + candidates.size() + " variante");
        });
    }

    @Tool(
            name = "calculateTco",
            description = """
                    Total cost of ownership of one catalogue variant over `years`: depreciation, energy, insurance, road tax,
                    maintenance and, optionally, loan interest. Returns every component, cost per month and per km, and the
                    assumptions used (distance, energy prices, charging). The energy prices are indicative defaults; pass
                    fuelPriceEurPerLiter or electricityPriceEurPerKwh only when the buyer gave their own. Never compute
                    these figures yourself.""")
    public Object calculateTco(
            @ToolParam(description = "Variant id from the catalogue.") String variantId,
            @ToolParam(required = false, description = "Ownership period in years, 1-10 (default 5).") Integer years,
            @ToolParam(required = false, description = "Kilometres per year; defaults to the buyer profile, then 15000.") Integer kmPerYear,
            @ToolParam(required = false, description = "Down payment in EUR when the car is financed.") Double downPaymentEur,
            @ToolParam(required = false, description = "Loan APR in percent; give together with loanMonths to include a loan.") Double aprPercent,
            @ToolParam(required = false, description = "Loan duration in months; give together with aprPercent.") Integer loanMonths,
            @ToolParam(required = false, description = "Fuel price in EUR per litre, only if the buyer gave one.") Double fuelPriceEurPerLiter,
            @ToolParam(required = false, description = "Electricity price in EUR per kWh, only if the buyer gave one.") Double electricityPriceEurPerKwh,
            ToolContext context) {
        return run(context, "calculateTco", "Calculez costul total de deținere…", run -> {
            VehicleCard car = singleVariant(variantId);
            BuyerProfile profile = toolbox.profile(run.sessionId());
            List<String> notes = new ArrayList<>();
            int km = kmPerYear != null ? kmPerYear : profile.yearlyKm() != null ? Math.max(profile.yearlyKm(), TcoCalculator.MIN_KM_PER_YEAR) : VehicleScorer.DEFAULT_KM_PER_YEAR;
            if (kmPerYear == null) {
                notes.add(profile.yearlyKm() != null
                        ? "Distance taken from the buyer profile."
                        : "Distance not given and not in the profile: " + VehicleScorer.DEFAULT_KM_PER_YEAR + " km/year assumed.");
            }
            if ((aprPercent == null) != (loanMonths == null)) {
                throw new InvalidInputException("aprPercent and loanMonths must be given together.");
            }
            TcoCalculator.Loan loan = aprPercent == null
                    ? null
                    : new TcoCalculator.Loan(BigDecimal.valueOf(downPaymentEur == null ? 0 : downPaymentEur), BigDecimal.valueOf(aprPercent), loanMonths);
            TcoResult tco = TcoCalculator.calculate(
                    car,
                    years == null ? VehicleScorer.DEFAULT_YEARS : years,
                    km,
                    profile.homeCharging(),
                    toolbox.energyPrices(),
                    fuelPriceEurPerLiter == null ? null : BigDecimal.valueOf(fuelPriceEurPerLiter),
                    electricityPriceEurPerKwh == null ? null : BigDecimal.valueOf(electricityPriceEurPerKwh),
                    loan);
            return new Outcome(new TcoToolResult(INDICATIVE, notes, tco), "Cost total pe " + tco.years() + " ani: " + tco.totalCostEur().toPlainString() + " €");
        });
    }

    @Tool(
            name = "calculateFinancing",
            description = """
                    Monthly instalment, total interest and total outlay of a fixed-rate loan. The APR is treated as a nominal
                    yearly rate without fees or insurance (so it is not the DAE). Use it whenever the buyer asks about a loan
                    or a monthly payment; never compute it yourself.""")
    public Object calculateFinancing(
            @ToolParam(description = "Price of the car in EUR.") Double priceEur,
            @ToolParam(required = false, description = "Down payment in EUR (default 0).") Double downPaymentEur,
            @ToolParam(description = "Loan APR in percent per year.") Double aprPercent,
            @ToolParam(description = "Loan duration in months (1-120).") Integer months,
            ToolContext context) {
        return run(context, "calculateFinancing", "Calculez finanțarea…", run -> {
            if (priceEur == null || aprPercent == null || months == null) {
                throw new InvalidInputException("priceEur, aprPercent and months are required.");
            }
            FinancingResult result = Financing.calculate(
                    BigDecimal.valueOf(priceEur), BigDecimal.valueOf(downPaymentEur == null ? 0 : downPaymentEur), BigDecimal.valueOf(aprPercent), months);
            return new Outcome(
                    Map.of("note", "Nominal APR, no fees or insurance; indicative, not a bank offer.", "financing", result),
                    "Rata lunară: " + result.monthlyPaymentEur().toPlainString() + " €");
        });
    }

    @Tool(
            name = "evVsIceBreakEven",
            description = """
                    Compares an electric variant with a petrol, diesel or hybrid variant year by year for THIS buyer's mileage
                    and charging access: purchase premium, yearly running-cost saving, simple payback in years, and the year
                    when the electric's cumulative total cost (with depreciation) catches up. A null result means it does not
                    happen within the horizon. Use it for "does an electric pay off for me".""")
    public Object evVsIceBreakEven(
            @ToolParam(description = "Variant id of the ELECTRIC car.") String evVariantId,
            @ToolParam(description = "Variant id of the petrol, diesel or hybrid car to compare against.") String iceVariantId,
            @ToolParam(required = false, description = "Kilometres per year; defaults to the buyer profile, then 15000.") Integer kmPerYear,
            @ToolParam(required = false, description = "How many years to look ahead, 1-10 (default 8).") Integer horizonYears,
            ToolContext context) {
        return run(context, "evVsIceBreakEven", "Compar electricul cu mașina clasică…", run -> {
            BuyerProfile profile = toolbox.profile(run.sessionId());
            List<VehicleCard> cars = toolbox.variants(List.of(evVariantId, iceVariantId));
            VehicleCard ev = cars.stream().filter(c -> c.variantId().equals(evVariantId)).findFirst().orElseThrow(() -> new NotFoundException("Unknown evVariantId."));
            VehicleCard ice = cars.stream().filter(c -> c.variantId().equals(iceVariantId)).findFirst().orElseThrow(() -> new NotFoundException("Unknown iceVariantId."));
            int km = kmPerYear != null ? kmPerYear : profile.yearlyKm() != null ? Math.max(profile.yearlyKm(), TcoCalculator.MIN_KM_PER_YEAR) : VehicleScorer.DEFAULT_KM_PER_YEAR;
            EvVsIce.Comparison comparison = EvVsIce.compare(ev, ice, km, profile.homeCharging(), toolbox.energyPrices(), horizonYears == null ? DEFAULT_HORIZON : horizonYears);
            String summary = comparison.simplePaybackYears() == null
                    ? "Fără amortizare simplă în orizont"
                    : "Amortizare simplă: " + comparison.simplePaybackYears().toPlainString() + " ani";
            return new Outcome(Map.of("note", INDICATIVE, "comparison", comparison), summary);
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

    /** Rezultatul comparatiei/clasamentului pentru model: aceeasi structura ca in evenimentul `compare`. */
    public record ReportResult(String note, ComparisonReport report) {}

    /** Rezultatul TCO pentru model: calculul, plus ce s-a presupus in locul datelor lipsa. */
    public record TcoToolResult(String note, List<String> notes, TcoResult tco) {}

    private VehicleCard singleVariant(String variantId) {
        return toolbox.variants(List.of(variantId)).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Unknown variantId; use searchVehicles to find valid ids."));
    }

    // Clasamentul `candidates` pentru profilul sesiunii, taiat la primele `keep` (cele mai bune).
    private ComparisonReport rank(ToolRun run, List<VehicleCard> candidates, int keep) {
        VehicleScorer.Ranking all = VehicleScorer.rank(candidates, toolbox.profile(run.sessionId()), toolbox.energyPrices(), VehicleScorer.DEFAULT_YEARS);
        List<ScoredVehicle> top = all.ranked().stream().limit(keep).toList();
        return ComparisonReport.from(new VehicleScorer.Ranking(all.years(), all.kmPerYear(), all.assumptions(), top));
    }

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
            run.record(json.writeValueAsString(outcome.result()));
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
