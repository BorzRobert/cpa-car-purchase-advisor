package ro.cpa.advisor.domain;

/**
 * Criteriile scorului „de ce aceasta masina”. Ponderile sunt fixe si publice (suma 100), ca orice scor sa se poata
 * explica si reproduce; un criteriu fara date suficiente nu se ghiceste, ci se marcheaza „neaplicabil” si iese din medie.
 */
public enum Criterion {
    PRICE_FIT("price_fit", "Potrivire cu bugetul", 25),
    RUNNING_COST("running_cost", "Costuri curente", 20),
    SPACE("space", "Spațiu", 15),
    SAFETY("safety", "Siguranță", 15),
    RELIABILITY("reliability", "Fiabilitate și valoare reziduală", 10),
    USAGE_FIT("usage_fit", "Potrivire cu modul de utilizare", 15);

    private final String id;
    private final String label;
    private final int weight;

    Criterion(String id, String label, int weight) {
        this.id = id;
        this.label = label;
        this.weight = weight;
    }

    /** Identificatorul stabil din payload (`criteria[].id`, `scores[].breakdown[].criterion`). */
    public String id() {
        return id;
    }

    /** Eticheta in romana, gata de afisat. */
    public String label() {
        return label;
    }

    public int weight() {
        return weight;
    }
}
