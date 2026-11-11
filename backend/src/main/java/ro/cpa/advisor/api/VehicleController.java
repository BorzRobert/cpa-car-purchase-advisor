package ro.cpa.advisor.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.cpa.advisor.domain.BodyType;
import ro.cpa.advisor.domain.Powertrain;
import ro.cpa.advisor.domain.Vehicle;
import ro.cpa.advisor.domain.VehicleCatalogue;
import ro.cpa.advisor.domain.VehicleFilter;
import ro.cpa.advisor.service.VehicleService;

/** Catalogul curatat, doar pentru citire. Preturile sunt orientative; raspunsul spune la ce data sunt valabile. */
@RestController
@RequestMapping(value = "/api/v1/vehicles", produces = "application/json")
@Tag(name = "Vehicles")
public class VehicleController {

    private final VehicleService vehicles;

    public VehicleController(VehicleService vehicles) {
        this.vehicles = vehicles;
    }

    @GetMapping
    @Operation(
            summary = "Search the catalogue",
            description = "Filters are optional and combined with AND. Vehicles are ordered by make and model, variants by price. "
                    + "Prices are indicative; `dataAsOf` is the date the figures are valid for.")
    public VehicleCatalogue search(
            @RequestParam(required = false) BodyType bodyType,
            @RequestParam(required = false) Powertrain powertrain,
            @RequestParam(required = false) Integer maxPriceEur,
            @RequestParam(required = false) Integer minSeats) {
        return vehicles.search(new VehicleFilter(bodyType, powertrain, maxPriceEur, minSeats));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a vehicle with all its variants")
    public Vehicle get(@PathVariable String id) {
        return vehicles.get(id);
    }
}
