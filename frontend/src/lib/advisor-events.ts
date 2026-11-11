/**
 * Evenimentele fluxului SSE al consilierului, așa cum le documentează `docs/architecture/communication.md`.
 *
 * DE CE sunt scrise aici și nu generate: evenimentele SSE nu pot fi descrise în OpenAPI (nu sunt răspunsuri JSON
 * obișnuite), deci tipurile lor se definesc o singură dată, în acest fișier, după documentul de comunicare. Enum-urile
 * (tip caroserie, motorizare) se iau din schema generată, ca să nu existe două liste.
 *
 * DE CE evenimentele necunoscute se ignoră: contractul permite doar schimbări aditive, deci un eveniment nou al unui
 * backend mai nou nu trebuie să strice un frontend mai vechi.
 */
import type { components } from "@/lib/api/schema";
import type { ProblemPayload } from "@/lib/api/errors";
import type { SseFrame } from "@/lib/sse";

export type BodyType = NonNullable<components["schemas"]["BuyerProfile"]["bodyTypes"]>[number];
export type Powertrain = NonNullable<components["schemas"]["BuyerProfile"]["powertrains"]>[number];

/** O variantă de mașină de arătat (`VehicleCard` din backend). Cifrele sunt orientative; `dataAsOf` se afișează lângă preț. */
export type VehicleCard = {
  variantId: string;
  vehicleId: string;
  make: string;
  model: string;
  variantName: string;
  bodyType: BodyType;
  powertrain: Powertrain;
  priceEur: number;
  seats: number;
  bootLitres: number;
  euroNcapStars?: number | null;
  consumptionLitersPer100Km?: number | null;
  consumptionKwhPer100Km?: number | null;
  electricRangeKm?: number | null;
  co2GramsPerKm: number;
  annualInsuranceEur: number;
  annualRoadTaxEur: number;
  annualMaintenanceEur: number;
  residualValuePercentAfter5Years: number;
  reliabilityNotes?: string | null;
  dataAsOf: string;
};

type ComparisonCriterion = { id: string; label: string; weight: number };

type ComparisonScore = {
  variantId: string;
  total: number;
  breakdown: { criterion: string; label: string; applicable: boolean; score: number; detail: string }[];
  tco: {
    totalCostEur: number;
    costPerMonthEur: number;
    costPerKmEur: number;
    annualRunningCostEur: number;
    depreciationEur: number;
    energyEur: number;
  };
};

/** Rezultatul `compareVehicles` / `rankVehicles`; `vehicles` și `scores` vin în ordinea clasamentului. */
export type ComparisonReport = {
  dataAsOf: string | null;
  years: number;
  kmPerYear: number;
  assumptions: string[];
  criteria: ComparisonCriterion[];
  vehicles: VehicleCard[];
  scores: ComparisonScore[];
};

export type AdvisorEvent =
  | { type: "meta"; messageId: string; conversationId: string }
  | { type: "token"; delta: string }
  | { type: "tool"; name: string; status: "started" | "finished"; summary: string }
  | { type: "vehicles"; cards: VehicleCard[]; dataAsOf: string }
  | { type: "profile"; patch: Record<string, unknown> }
  | { type: "compare"; report: ComparisonReport }
  | { type: "error"; problem: ProblemPayload }
  | { type: "done"; usage: { input: number; output: number } };

/** Transformă un cadru SSE în eveniment tipat; `null` pentru evenimente necunoscute sau cu un corp care nu e JSON. */
export function parseAdvisorEvent(frame: SseFrame): AdvisorEvent | null {
  let payload: unknown;
  try {
    payload = JSON.parse(frame.data);
  } catch {
    return null;
  }
  const data = payload as Record<string, unknown>;

  switch (frame.event) {
    case "meta":
      return { type: "meta", ...(data as { messageId: string; conversationId: string }) };
    case "token":
      return { type: "token", delta: String(data.delta ?? "") };
    case "tool":
      return { type: "tool", ...(data as { name: string; status: "started" | "finished"; summary: string }) };
    case "vehicles":
      return { type: "vehicles", ...(data as { cards: VehicleCard[]; dataAsOf: string }) };
    case "profile":
      return { type: "profile", patch: (data.patch ?? {}) as Record<string, unknown> };
    case "compare":
      return { type: "compare", report: data as unknown as ComparisonReport };
    case "error":
      return { type: "error", problem: data as ProblemPayload };
    case "done":
      return { type: "done", usage: (data.usage ?? { input: 0, output: 0 }) as { input: number; output: number } };
    default:
      return null;
  }
}
