"use client";

/**
 * Stepper-ul etapelor călătoriei (Nevoi → Căutare → Comparație → Listă scurtă).
 *
 * DE CE etapele se deduc din date (`lib/journey.ts`): bifa „Comparație” înseamnă că există o comparație pe ecran, nu că
 * modelul a spus ceva despre ea. Etapa curentă e marcată și pentru cititoarele de ecran (`aria-current="step"`), iar
 * starea e scrisă în text, nu doar în culoare.
 */
import { Check } from "lucide-react";
import { journeyStates, type JourneyFacts, type StageState } from "@/lib/journey";
import { cn } from "@/lib/utils";

const STATE_LABEL: Record<StageState, string> = { done: "terminat", current: "în curs", upcoming: "urmează" };

const MARKER_CLASS: Record<StageState, string> = {
  done: "border-signal bg-signal text-signal-foreground",
  current: "border-primary bg-primary text-primary-foreground",
  upcoming: "border-border bg-background text-muted-foreground"
};

export function JourneyStepper({ facts }: { facts: JourneyFacts }) {
  const states = journeyStates(facts);

  return (
    <ol className="grid grid-cols-4 gap-1" aria-label="Etapele cumpărării">
      {states.map(({ stage, state }, index) => (
        <li
          key={stage.id}
          className="flex flex-col items-center gap-1 text-center"
          aria-current={state === "current" ? "step" : undefined}
          title={stage.hint}
        >
          <span
            className={cn(
              "grid size-6 place-items-center rounded-full border font-mono text-xs font-semibold",
              MARKER_CLASS[state]
            )}
            aria-hidden="true"
          >
            {state === "done" ? <Check className="size-3.5" /> : index + 1}
          </span>
          <span className={cn("text-[11px] leading-tight", state === "upcoming" && "text-muted-foreground")}>
            {stage.label}
            <span className="sr-only"> ({STATE_LABEL[state]})</span>
          </span>
        </li>
      ))}
    </ol>
  );
}
