"use client";

/**
 * Butonul din header care deschide panoul lateral pe ecran îngust.
 *
 * DE CE e permis în header, deși centrul rămâne minimal: e perechea lui `SidebarTrigger` (un comutator de panou, nu o
 * bară de acțiuni) și apare doar acolo unde panoul nu are loc lângă conversație. Pe ecran lat nu se randează.
 */
import { PanelRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useIsWideLayout } from "@/hooks/use-is-wide-layout";
import { useAppStore } from "@/store/useAppStore";

export function PanelTrigger() {
  const wide = useIsWideLayout();
  const setOpen = useAppStore(state => state.setPanelOpen);
  if (wide) return null;

  return (
    <Button
      variant="ghost"
      size="icon-sm"
      className="ml-auto"
      aria-label="Deschide panoul cu nevoile, lista scurtă și costul"
      onClick={() => setOpen(true)}
    >
      <PanelRight />
    </Button>
  );
}
