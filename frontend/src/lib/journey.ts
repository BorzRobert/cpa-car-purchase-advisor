/**
 * Etapele călătoriei de cumpărare și etapa în care te afli, deduse din ce știe aplicația (nu din ce zice modelul).
 *
 * DE CE deduse din date: profil completat → căutare făcută → comparație făcută → listă scurtă; sunt fapte verificabile
 * din profil, din evenimentele fluxului și din lista scurtă. Funcția e pură ca să se poată testa fără interfață.
 * Registrul de etape e folosit și de ghid (exemplele de întrebări sunt grupate pe etape), deci eticheta stă într-un singur loc.
 */
export type JourneyStageId = "needs" | "search" | "compare" | "shortlist";

export type JourneyStage = { id: JourneyStageId; label: string; hint: string };

export const JOURNEY_STAGES: JourneyStage[] = [
  { id: "needs", label: "Nevoi", hint: "Spune-i consilierului bugetul și cum conduci." },
  { id: "search", label: "Căutare", hint: "Cere variante potrivite profilului tău." },
  { id: "compare", label: "Comparație", hint: "Compară variantele pe scor și cost total." },
  { id: "shortlist", label: "Listă scurtă", hint: "Salvează favoritele ca să decizi liniștit." }
];

export type JourneyFacts = {
  hasProfile: boolean;
  hasVehicles: boolean;
  hasComparison: boolean;
  shortlistCount: number;
};

export type StageState = "done" | "current" | "upcoming";

/** Etapele sunt „bifate” cât timp faptul lor e adevărat; prima nebifată e cea curentă. */
export function journeyStates(facts: JourneyFacts): { stage: JourneyStage; state: StageState }[] {
  const reached: Record<JourneyStageId, boolean> = {
    needs: facts.hasProfile,
    search: facts.hasVehicles || facts.hasComparison,
    compare: facts.hasComparison,
    shortlist: facts.shortlistCount > 0
  };
  const firstOpen = JOURNEY_STAGES.findIndex(stage => !reached[stage.id]);
  return JOURNEY_STAGES.map((stage, index) => ({
    stage,
    state: firstOpen === -1 || index < firstOpen ? "done" : index === firstOpen ? "current" : "upcoming"
  }));
}
