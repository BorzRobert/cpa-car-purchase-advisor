package ro.cpa.advisor.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Preturile energiei folosite de calculatorul TCO (EUR). Sunt cifre ORIENTATIVE din configuratie, cu data lor: nu vin din
 * memoria modelului, iar cumparatorul le poate inlocui cu preturile lui. Rezultatele le raporteaza mereu ca ipoteze.
 */
public record EnergyPrices(
        BigDecimal petrolEurPerLiter,
        BigDecimal dieselEurPerLiter,
        BigDecimal homeElectricityEurPerKwh,
        BigDecimal publicElectricityEurPerKwh,
        LocalDate dataAsOf) {}
