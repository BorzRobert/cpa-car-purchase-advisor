package ro.cpa.advisor.domain;

import java.time.LocalDate;
import java.util.List;

/** Rezultatul unei cautari in catalog, cu data la care sunt valabile cifrele (afisata in UI ca „date la”). */
public record VehicleCatalogue(LocalDate dataAsOf, List<Vehicle> vehicles) {}
