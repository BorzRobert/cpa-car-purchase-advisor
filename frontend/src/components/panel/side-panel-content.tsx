"use client";

/**
 * Conținutul panoului lateral: etapa curentă, nevoile, lista scurtă și costul total curent.
 *
 * DE CE e separat de containerul panoului: același conținut stă lângă conversație pe ecran lat și într-o foaie de jos pe
 * ecran îngust. Secțiunile sunt un registru (titlu + conținut), deci o secțiune nouă nu atinge structura.
 * Panoul citește din cache-ul TanStack Query (profil, listă scurtă) și din comparația trimisă ca prop, deci se actualizează
 * singur când agentul schimbă ceva.
 */
import { JourneyStepper } from "@/components/panel/journey-stepper";
import { NeedsSection } from "@/components/panel/needs-section";
import { RunningTco } from "@/components/panel/running-tco";
import { ShortlistView } from "@/components/panel/shortlist-view";
import { GlossaryTerm } from "@/components/help/glossary-term";
import { useProfile } from "@/hooks/use-profile";
import { useShortlist } from "@/hooks/use-shortlist";
import type { ComparisonReport } from "@/lib/advisor-events";
import { profileChips } from "@/lib/profile-chips";

type SidePanelContentProps = { comparison: ComparisonReport | null; hasVehicles: boolean };

export function SidePanelContent({ comparison, hasVehicles }: SidePanelContentProps) {
  const profile = useProfile();
  const shortlist = useShortlist();

  const facts = {
    hasProfile: profile.data ? profileChips(profile.data).length > 0 : false,
    hasVehicles,
    hasComparison: comparison !== null,
    shortlistCount: shortlist.data?.length ?? 0
  };

  const sections: { id: string; title: React.ReactNode; body: React.ReactNode }[] = [
    { id: "needs", title: "Nevoile tale", body: <NeedsSection /> },
    { id: "shortlist", title: "Lista scurtă", body: <ShortlistView /> },
    {
      id: "tco",
      title: <GlossaryTerm id="tco">Cost total de deținere</GlossaryTerm>,
      body: <RunningTco report={comparison} />
    }
  ];

  return (
    <div className="flex flex-col gap-5 p-4">
      <JourneyStepper facts={facts} />
      {sections.map(section => (
        <section key={section.id} aria-labelledby={`panel-${section.id}`} className="flex flex-col gap-2.5">
          <h2 id={`panel-${section.id}`} className="text-sm font-semibold tracking-tight">
            {section.title}
          </h2>
          {section.body}
        </section>
      ))}
    </div>
  );
}
