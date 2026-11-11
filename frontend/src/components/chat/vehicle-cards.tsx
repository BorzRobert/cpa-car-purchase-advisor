"use client";

/**
 * Cardurile de vehicule trimise de agent (evenimentul `vehicles`), într-o formă minimă.
 *
 * DE CE minimă: widget-urile finale (scor, badge-uri, comparație) sunt treaba Fazei 7; aici cardurile doar dovedesc că
 * datele ajung de la unelte la ecran fără să treacă prin textul modelului. „Date la” stă lângă preț, cum cere contractul.
 */
import { Check, Plus } from "lucide-react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useAddToShortlist, useShortlist } from "@/hooks/use-shortlist";
import type { VehicleCard } from "@/lib/advisor-events";
import { describeError } from "@/lib/api/errors";
import { POWERTRAIN_OPTIONS, formatEur, labelOf } from "@/lib/profile-options";

function consumption(card: VehicleCard): string | null {
  if (card.consumptionLitersPer100Km) return `${card.consumptionLitersPer100Km} l/100 km`;
  if (card.consumptionKwhPer100Km) return `${card.consumptionKwhPer100Km} kWh/100 km`;
  return null;
}

export function VehicleCards({ cards }: { cards: VehicleCard[] }) {
  const shortlist = useShortlist();
  const add = useAddToShortlist();
  const saved = new Set(shortlist.data?.flatMap(item => item.vehicle?.variants?.map(variant => variant.id) ?? []));

  if (cards.length === 0) return null;

  function handleAdd(card: VehicleCard) {
    add.mutate(card.variantId, {
      onSuccess: () => toast.success("Adăugat în lista scurtă"),
      onError: error => toast.error(describeError(error).title)
    });
  }

  return (
    <div className="grid gap-2 sm:grid-cols-2">
      {cards.map(card => (
        <Card key={card.variantId} size="sm">
          <CardHeader>
            <CardTitle>
              {card.make} {card.model}
            </CardTitle>
            <p className="text-xs text-muted-foreground">{card.variantName}</p>
          </CardHeader>
          <CardContent className="flex flex-col gap-2">
            <div className="flex flex-wrap items-center gap-1.5">
              <Badge variant="secondary">{labelOf(POWERTRAIN_OPTIONS, card.powertrain)}</Badge>
              <span className="font-medium">{formatEur(card.priceEur)}</span>
              {consumption(card) && <span className="text-xs text-muted-foreground">{consumption(card)}</span>}
            </div>
            <p className="text-xs text-muted-foreground">
              Preț orientativ · date la {card.dataAsOf} · {card.seats} locuri · portbagaj {card.bootLitres} l
            </p>
            <Button
              variant="outline"
              size="xs"
              className="w-fit"
              disabled={saved.has(card.variantId) || add.isPending}
              onClick={() => handleAdd(card)}
            >
              {saved.has(card.variantId) ? <Check /> : <Plus />}
              {saved.has(card.variantId) ? "În lista scurtă" : "Lista scurtă"}
            </Button>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
