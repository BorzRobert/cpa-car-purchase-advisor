"use client";

/**
 * Containerul panoului lateral: coloană fixă pe ecran lat, foaie de jos pe ecran îngust.
 *
 * DE CE se randează o singură variantă (nu două, una ascunsă din CSS): vezi `use-is-wide-layout.ts`. Foaia se deschide
 * din butonul din header (`PanelTrigger`) prin `panelOpen` din store; pe ecran lat panoul e mereu vizibil, deci acolo
 * butonul lipsește.
 */
import { SidePanelContent } from "@/components/panel/side-panel-content";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { useIsWideLayout } from "@/hooks/use-is-wide-layout";
import type { ComparisonReport } from "@/lib/advisor-events";
import { useAppStore } from "@/store/useAppStore";

type SidePanelProps = { comparison: ComparisonReport | null; hasVehicles: boolean };

export function SidePanel({ comparison, hasVehicles }: SidePanelProps) {
  const wide = useIsWideLayout();
  const open = useAppStore(state => state.panelOpen);
  const setOpen = useAppStore(state => state.setPanelOpen);

  if (wide) {
    return (
      <aside aria-label="Panou lateral" className="flex w-80 shrink-0 flex-col border-l bg-card/40">
        <ScrollArea className="min-h-0 flex-1">
          <SidePanelContent comparison={comparison} hasVehicles={hasVehicles} />
        </ScrollArea>
      </aside>
    );
  }

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetContent side="bottom" className="max-h-[85svh] gap-0 rounded-t-xl">
        <SheetHeader>
          <SheetTitle>Panou: nevoi, listă scurtă, cost</SheetTitle>
          <SheetDescription>Rezumatul căutării tale, actualizat pe măsură ce consilierul lucrează.</SheetDescription>
        </SheetHeader>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <SidePanelContent comparison={comparison} hasVehicles={hasVehicles} />
        </div>
      </SheetContent>
    </Sheet>
  );
}
