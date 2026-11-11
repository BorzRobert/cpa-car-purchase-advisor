package ro.cpa.advisor.domain;

import java.util.List;

/** Scorul unei variante pe criterii (0-100 fiecare), scorul total ponderat si TCO-ul pe care s-a bazat. */
public record ScoredVehicle(VehicleCard card, int totalScore, List<CriterionScore> breakdown, TcoResult tco) {

    /** Un criteriu: scorul, daca a contat (`applicable`) si explicatia in romana. */
    public record CriterionScore(Criterion criterion, boolean applicable, int score, String detail) {}
}
