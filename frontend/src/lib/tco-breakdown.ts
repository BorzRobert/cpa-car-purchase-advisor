/**
 * Descompunerea costului total de deținere (TCO) în segmente pentru bara stivuită.
 *
 * DE CE „restul” se calculează și nu se citește: backend-ul trimite totalul, depreciarea și energia; asigurarea, taxa și
 * mentenanța sunt diferența. Așa segmentele însumează exact totalul afișat, oricum ar rotunji backend-ul.
 * Cifrele rămân cele din rezultatul uneltei; aici nu se calculează nimic nou despre costul real.
 */
import type { ComparisonReport } from "@/lib/advisor-events";
import type { GlossaryId } from "@/lib/glossary";

type Tco = ComparisonReport["scores"][number]["tco"];

export type TcoSegment = {
  id: "depreciation" | "energy" | "fixed";
  label: string;
  /** Termenul din glosar care explică segmentul, dacă are unul. */
  glossary?: GlossaryId;
  valueEur: number;
  share: number;
};

export function tcoSegments(tco: Tco): TcoSegment[] {
  const depreciation = Math.max(0, tco.depreciationEur);
  const energy = Math.max(0, tco.energyEur);
  const fixed = Math.max(0, tco.totalCostEur - depreciation - energy);
  const total = depreciation + energy + fixed;
  const parts: Omit<TcoSegment, "share">[] = [
    { id: "depreciation", label: "Depreciere", glossary: "depreciation", valueEur: depreciation },
    { id: "energy", label: "Energie", valueEur: energy },
    { id: "fixed", label: "Asigurare, taxe, mentenanță", valueEur: fixed }
  ];
  return parts.map(part => ({ ...part, share: total > 0 ? part.valueEur / total : 0 }));
}
