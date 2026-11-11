"use client";

/**
 * Preferințele: fereastră separată, cu navigație în stânga și conținut în dreapta.
 *
 * DE CE `Dialog` și nu un panou inline sau un ecran propriu: setările sunt o paranteză, nu o destinație. Dialogul apare
 * peste, se închide cu Escape, iar conversația rămâne exact unde era.
 *
 * DE CE starea de deschidere stă în store: dialogul se deschide de la butonul din sidebar — o componentă care nu-l
 * conține. Prin store, orice buton din aplicație poate deschide direct secțiunea potrivită.
 */

import { ListChecks, Settings, UserRound } from "lucide-react";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { AppearanceForm } from "@/components/settings/appearance-form";
import { ProfileForm } from "@/components/settings/profile-form";
import { ShortlistView } from "@/components/panel/shortlist-view";
import { useAppStore, type SettingsSection } from "@/store/useAppStore";
import { cn } from "@/lib/utils";

/**
 * Registrul de secțiuni: id → etichetă, iconiță, descriere, conținut. Navigația din stânga și panoul din dreapta se
 * generează din aceeași sursă, deci nu poți adăuga o intrare în meniu fără conținut, sau invers.
 */
const SECTIONS: {
  id: SettingsSection;
  label: string;
  icon: typeof Settings;
  description: string;
  Content: () => React.ReactNode;
}[] = [
  {
    id: "general",
    label: "General",
    icon: Settings,
    description: "Cum arată aplicația.",
    Content: AppearanceForm
  },
  {
    id: "profil",
    label: "Nevoile tale",
    icon: UserRound,
    description: "Din datele astea știe consilierul ce să caute și cum să calculeze.",
    Content: ProfileForm
  },
  {
    id: "lista",
    label: "Lista scurtă",
    icon: ListChecks,
    description: "Variantele pe care ți le-ai salvat pentru comparație.",
    Content: ShortlistView
  }
];

export function SettingsDialog() {
  const open = useAppStore(state => state.settingsOpen);
  const setOpen = useAppStore(state => state.setSettingsOpen);
  const section = useAppStore(state => state.settingsSection);
  const setSection = useAppStore(state => state.setSettingsSection);

  const active = SECTIONS.find(item => item.id === section) ?? SECTIONS[0];

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      {/*
        `p-0 gap-0` anulează spațierea implicită: marginile le desenează cele două panouri. Înălțimea fixă ține
        navigația pe loc când conținutul din dreapta e mai lung.
      */}
      <DialogContent className="grid h-[min(36rem,90vh)] w-full gap-0 overflow-hidden p-0 sm:max-w-3xl md:grid-cols-[13rem_1fr]">
        <DialogHeader className="sr-only">
          <DialogTitle>Preferințe</DialogTitle>
          <DialogDescription>Setările aplicației, nevoile tale de cumpărător și lista scurtă.</DialogDescription>
        </DialogHeader>

        {/* Pe mobil devine un rând orizontal deasupra conținutului: o coloană de 13rem ar mânca jumătate din lățime. */}
        <nav className="flex shrink-0 gap-1 overflow-x-auto border-b bg-muted/40 p-2 md:flex-col md:overflow-visible md:border-r md:border-b-0 md:p-3">
          <span className="hidden px-2 pb-1 text-xs font-medium text-muted-foreground md:block">Setări</span>
          {SECTIONS.map(item => (
            <Button
              key={item.id}
              variant="ghost"
              size="sm"
              onClick={() => setSection(item.id)}
              className={cn(
                "shrink-0 justify-start gap-2 font-normal md:w-full",
                item.id === active.id && "bg-accent text-accent-foreground"
              )}
            >
              <item.icon />
              {item.label}
            </Button>
          ))}
        </nav>

        {/* `div` nativ, nu `ScrollArea`: wrapper-ul intern al Radix se dimensionează după conținut și nu ancorează bara de jos. */}
        <div className="h-full min-h-0 overflow-y-auto">
          <div className="flex h-full min-h-0 flex-col gap-6 p-6">
            <div className="flex flex-col gap-1">
              <h2 className="text-lg font-semibold tracking-tight">{active.label}</h2>
              <p className="text-sm text-muted-foreground">{active.description}</p>
            </div>
            <Separator />
            <active.Content />
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
