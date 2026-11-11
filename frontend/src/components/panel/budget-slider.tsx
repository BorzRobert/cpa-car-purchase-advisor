"use client";

/**
 * Cursorul de buget cu comutatorul „total / pe lună”.
 *
 * DE CE se salvează la eliberarea cursorului (`onValueCommitted`) și nu la fiecare pas: o cerere pe pixel ar umple backend-ul
 * cu `PUT`-uri, iar profilul trebuie să aibă mereu o valoare pe care utilizatorul a confirmat-o. În timpul tragerii se arată
 * valoarea locală.
 * DE CE `PUT` cu tot profilul: backend-ul înlocuiește tot profilul, deci pornim de la profilul curent și schimbăm doar bugetul.
 */
import { useState } from "react";
import { toast } from "sonner";
import { OptionToggle } from "@/components/settings/option-toggle";
import { Slider } from "@/components/ui/slider";
import { useSaveProfile } from "@/hooks/use-profile";
import { describeError } from "@/lib/api/errors";
import type { BuyerProfile } from "@/lib/api/endpoints";
import { BUDGET_RANGES, clampBudget, type BudgetKind } from "@/lib/budget";
import { toDraft, toRequest } from "@/lib/profile-draft";
import { BUDGET_KIND_OPTIONS, formatEur } from "@/lib/profile-options";

export function BudgetSlider({ profile }: { profile: BuyerProfile }) {
  const save = useSaveProfile();
  const kind: BudgetKind = profile.budgetKind ?? "TOTAL";
  const range = BUDGET_RANGES[kind];
  const saved = profile.budgetAmount === undefined || profile.budgetAmount === null ? null : profile.budgetAmount;
  const [dragging, setDragging] = useState<number | null>(null);
  const shown = dragging ?? (saved === null ? null : clampBudget(saved, kind));

  function persist(budgetAmount: number, budgetKind: BudgetKind) {
    const { request } = toRequest({
      ...toDraft(profile),
      budgetAmount: String(budgetAmount),
      budgetKind: [budgetKind]
    });
    save.mutate(request, { onError: error => toast.error(describeError(error).title) });
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center justify-between gap-2">
        <span className="font-mono text-sm font-semibold tabular-nums" aria-live="polite">
          {shown === null ? "Buget neprecizat" : `${formatEur(shown)}${kind === "MONTHLY" ? " / lună" : ""}`}
        </span>
        <OptionToggle
          label="Tipul bugetului"
          options={BUDGET_KIND_OPTIONS}
          value={[kind]}
          onChange={([next]) => {
            // Cel puțin o opțiune rămâne activă: a doua apăsare pe cea curentă nu o deselectează.
            if (!next || next === kind) return;
            persist(clampBudget(saved ?? range.min, next), next);
          }}
        />
      </div>
      <Slider
        thumbLabel={`Buget ${kind === "MONTHLY" ? "lunar" : "total"}`}
        min={range.min}
        max={range.max}
        step={range.step}
        value={[shown ?? range.min]}
        onValueChange={value => setDragging(Array.isArray(value) ? value[0] : value)}
        onValueCommitted={value => {
          setDragging(null);
          persist(Array.isArray(value) ? value[0] : value, kind);
        }}
        disabled={save.isPending}
      />
      <div className="flex justify-between font-mono text-[11px] text-muted-foreground tabular-nums">
        <span>{formatEur(range.min)}</span>
        <span>{formatEur(range.max)}</span>
      </div>
    </div>
  );
}
