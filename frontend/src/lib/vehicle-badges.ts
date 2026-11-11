/**
 * Insigne pentru cardurile de vehicule, calculate din datele cardului și (dacă există) din clasament.
 *
 * DE CE regulile sunt date: o insignă nouă e o intrare în registru, nu un `if` în componentă. Insignele spun doar ce
 * rezultă direct din cifre (cel mai bun scor, cel mai mic cost lunar, 5 stele Euro NCAP, autonomie electrică), nu opinii.
 */
import type { ComparisonReport, VehicleCard } from "@/lib/advisor-events";

export type VehicleBadge = { id: string; label: string };

type BadgeContext = { card: VehicleCard; report: ComparisonReport | null };

type BadgeRule = { id: string; label: string; applies: (context: BadgeContext) => boolean };

function isCheapestPerMonth({ card, report }: BadgeContext): boolean {
  if (!report || report.scores.length < 2) return false;
  const cheapest = report.scores.reduce((best, score) =>
    score.tco.costPerMonthEur < best.tco.costPerMonthEur ? score : best
  );
  return cheapest.variantId === card.variantId;
}

const BADGE_RULES: BadgeRule[] = [
  {
    id: "best-score",
    label: "Cel mai bun scor",
    applies: ({ card, report }) => (report?.scores.length ?? 0) > 1 && report?.scores[0]?.variantId === card.variantId
  },
  { id: "cheapest", label: "Cel mai mic cost lunar", applies: isCheapestPerMonth },
  { id: "ncap", label: "5★ Euro NCAP", applies: ({ card }) => card.euroNcapStars === 5 },
  { id: "range", label: "Autonomie 400+ km", applies: ({ card }) => (card.electricRangeKm ?? 0) >= 400 }
];

export function vehicleBadges(card: VehicleCard, report: ComparisonReport | null): VehicleBadge[] {
  return BADGE_RULES.filter(rule => rule.applies({ card, report })).map(({ id, label }) => ({ id, label }));
}
