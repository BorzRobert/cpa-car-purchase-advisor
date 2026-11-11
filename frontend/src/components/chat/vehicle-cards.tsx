"use client";

/**
 * Cardurile de vehicule ale unui răspuns (evenimentul `vehicles`), într-o grilă.
 *
 * DE CE logica listei scurte stă aici și nu în fiecare card: lista se citește o singură dată pentru toate cardurile, iar
 * un card nu trebuie să știe de TanStack Query ca să poată fi desenat (și testat) doar din date.
 */
import { useMemo } from "react";
import { toast } from "sonner";
import { VehicleCard } from "@/components/chat/vehicle-card";
import { useAddToShortlist, useShortlist } from "@/hooks/use-shortlist";
import type { ComparisonReport, VehicleCard as VehicleCardData } from "@/lib/advisor-events";
import { describeError } from "@/lib/api/errors";
import { vehicleBadges } from "@/lib/vehicle-badges";

type VehicleCardsProps = { cards: VehicleCardData[]; report: ComparisonReport | null };

export function VehicleCards({ cards, report }: VehicleCardsProps) {
  const shortlist = useShortlist();
  const add = useAddToShortlist();
  const saved = useMemo(
    () => new Set(shortlist.data?.flatMap(item => item.vehicle?.variants?.map(variant => variant.id) ?? [])),
    [shortlist.data]
  );
  const scores = useMemo(() => new Map(report?.scores.map(score => [score.variantId, score.total]) ?? []), [report]);

  if (cards.length === 0) return null;

  function handleAdd(card: VehicleCardData) {
    add.mutate(card.variantId, {
      onSuccess: () => toast.success("Adăugat în lista scurtă"),
      onError: error => toast.error(describeError(error).title)
    });
  }

  return (
    <ul className="grid w-full gap-3 sm:grid-cols-2" aria-label="Variante găsite">
      {cards.map(card => (
        <li key={card.variantId} className="min-w-0">
          <VehicleCard
            card={card}
            score={scores.get(card.variantId)}
            badges={vehicleBadges(card, report)}
            saved={saved.has(card.variantId)}
            saving={add.isPending}
            onAdd={handleAdd}
          />
        </li>
      ))}
    </ul>
  );
}
