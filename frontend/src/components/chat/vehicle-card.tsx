"use client";

/**
 * Un card de vehicul trimis de agent: scor, insigne, preț cu „date la”, cifrele care contează și butonul de listă scurtă.
 *
 * DE CE scorul vine separat de card: evenimentul `vehicles` aduce doar datele variantei; scorul există abia când agentul
 * a făcut o comparație (`compare`). Fără comparație cardul arată la fel, doar fără inel.
 */
import { Check, Plus } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Price } from "@/components/chat/price";
import { ScoreRing } from "@/components/chat/score-ring";
import { GlossaryTerm } from "@/components/help/glossary-term";
import type { VehicleCard as VehicleCardData } from "@/lib/advisor-events";
import type { GlossaryId } from "@/lib/glossary";
import { POWERTRAIN_OPTIONS, labelOf } from "@/lib/profile-options";
import type { VehicleBadge } from "@/lib/vehicle-badges";

type VehicleCardProps = {
  card: VehicleCardData;
  score?: number;
  badges: VehicleBadge[];
  saved: boolean;
  saving: boolean;
  onAdd: (card: VehicleCardData) => void;
};

/** Cifrele cheie ale cardului, ca registru: o cifră nouă e o intrare, iar cele lipsă (ex. consum pe electric) se omit. */
const FACTS: {
  id: string;
  label: string;
  glossary?: GlossaryId;
  value: (card: VehicleCardData) => string | null;
}[] = [
  {
    id: "consumption",
    label: "Consum",
    value: card =>
      card.consumptionLitersPer100Km
        ? `${card.consumptionLitersPer100Km} l/100 km`
        : card.consumptionKwhPer100Km
          ? `${card.consumptionKwhPer100Km} kWh/100 km`
          : null
  },
  {
    id: "range",
    label: "Autonomie",
    value: card => (card.electricRangeKm ? `${card.electricRangeKm} km` : null)
  },
  { id: "seats", label: "Locuri", value: card => String(card.seats) },
  { id: "boot", label: "Portbagaj", value: card => `${card.bootLitres} l` },
  {
    id: "ncap",
    label: "Euro NCAP",
    value: card => (card.euroNcapStars ? `${card.euroNcapStars} ${card.euroNcapStars === 1 ? "stea" : "stele"}` : null)
  },
  {
    id: "residual",
    label: "Valoare reziduală",
    glossary: "residual",
    value: card => `${card.residualValuePercentAfter5Years}% după 5 ani`
  }
];

export function VehicleCard({ card, score, badges, saved, saving, onAdd }: VehicleCardProps) {
  const facts = FACTS.flatMap(fact => {
    const value = fact.value(card);
    return value ? [{ id: fact.id, label: fact.label, glossary: fact.glossary, value }] : [];
  });

  return (
    <Card size="sm" className="gap-3">
      <CardHeader className="flex flex-row items-start justify-between gap-3">
        <div className="flex min-w-0 flex-col gap-0.5">
          <CardTitle className="truncate">
            {card.make} {card.model}
          </CardTitle>
          <p className="truncate text-xs text-muted-foreground">{card.variantName}</p>
        </div>
        {score !== undefined && <ScoreRing score={score} />}
      </CardHeader>

      <CardContent className="flex flex-col gap-3">
        <div className="flex flex-wrap gap-1.5">
          <Badge variant="secondary">{labelOf(POWERTRAIN_OPTIONS, card.powertrain)}</Badge>
          {badges.map(badge => (
            <Badge key={badge.id} className="bg-signal text-signal-foreground">
              {badge.label}
            </Badge>
          ))}
        </div>

        <Price valueEur={card.priceEur} dataAsOf={card.dataAsOf} />

        <dl className="grid grid-cols-2 gap-x-3 gap-y-1.5 text-xs">
          {facts.map(fact => (
            <div key={fact.id} className="flex flex-col">
              <dt className="text-muted-foreground">
                {fact.glossary ? <GlossaryTerm id={fact.glossary}>{fact.label}</GlossaryTerm> : fact.label}
              </dt>
              <dd className="font-mono tabular-nums">{fact.value}</dd>
            </div>
          ))}
        </dl>

        <Button
          variant={saved ? "secondary" : "outline"}
          size="xs"
          className="w-fit"
          disabled={saved || saving}
          onClick={() => onAdd(card)}
        >
          {saved ? <Check /> : <Plus />}
          {saved ? "În lista scurtă" : "Adaugă în lista scurtă"}
        </Button>
      </CardContent>
    </Card>
  );
}
