"use client";

/**
 * „Nevoile tale” în panoul lateral: chip-uri cu ce știe consilierul până acum, plus cursorul de buget.
 *
 * DE CE chip-uri și nu formularul întreg: panoul e o oglindă rapidă, care se schimbă sub ochii tăi când agentul învață
 * ceva (evenimentul `profile`). Pentru modificări amănunțite există formularul din Preferințe, la un click distanță.
 */
import { Pencil } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { BudgetSlider } from "@/components/panel/budget-slider";
import { useProfile } from "@/hooks/use-profile";
import type { BuyerProfile } from "@/lib/api/endpoints";
import { profileChips } from "@/lib/profile-chips";
import { useAppStore } from "@/store/useAppStore";

export function NeedsSection() {
  const profile = useProfile();
  const openSettings = useAppStore(state => state.openSettings);

  if (profile.isError) {
    return (
      <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
        <p>Nu am putut încărca profilul.</p>
        <Button variant="outline" size="xs" onClick={() => void profile.refetch()}>
          Reîncearcă
        </Button>
      </div>
    );
  }

  if (!profile.data) {
    return (
      <div className="flex flex-col gap-2" aria-hidden>
        <Skeleton className="h-6 w-3/4" />
        <Skeleton className="h-6 w-1/2" />
        <Skeleton className="h-8 w-full" />
      </div>
    );
  }

  const data: BuyerProfile = profile.data;
  const chips = profileChips(data);

  return (
    <div className="flex flex-col gap-3">
      {chips.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          Încă nu știu nimic despre nevoile tale. Spune-mi bugetul, câți kilometri faci pe an și câți pasageri sunt de
          obicei în mașină.
        </p>
      ) : (
        <ul className="flex flex-wrap gap-1.5" aria-label="Nevoile tale">
          {chips.map(chip => (
            <li key={chip.id}>
              <Badge variant="secondary" className="h-auto max-w-full py-0.5 whitespace-normal">
                {chip.label}
              </Badge>
            </li>
          ))}
        </ul>
      )}
      <BudgetSlider profile={data} />
      <Button variant="ghost" size="xs" className="w-fit" onClick={() => openSettings("profil")}>
        <Pencil />
        Editează nevoile
      </Button>
    </div>
  );
}
