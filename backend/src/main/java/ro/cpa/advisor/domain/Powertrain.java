package ro.cpa.advisor.domain;

/** Tipul de motorizare din catalog; valorile se salveaza ca text in baza de date, deci nu se redenumesc fara migrare. */
public enum Powertrain {
    PETROL,
    DIESEL,
    HYBRID,
    PLUG_IN_HYBRID,
    ELECTRIC
}
