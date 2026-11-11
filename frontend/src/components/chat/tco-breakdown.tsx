/**
 * Costul total de deținere al unei variante ca bară stivuită: depreciere, energie, asigurare + taxe + mentenanță.
 *
 * DE CE bară stivuită: arată dintr-o privire ce anume face o mașină scumpă (de obicei deprecierea, nu benzina). Segmentele
 * sunt calculate în `lib/tco-breakdown.ts`; aici doar se desenează, iar cifrele din legendă sunt cele din bară, ca
 * informația să nu depindă de culoare.
 */
import { GlossaryTerm } from "@/components/help/glossary-term";
import type { ComparisonReport } from "@/lib/advisor-events";
import { tcoSegments, type TcoSegment } from "@/lib/tco-breakdown";
import { formatEur } from "@/lib/profile-options";

type Tco = ComparisonReport["scores"][number]["tco"];

/** Culoarea fiecărui segment; cheile sunt id-urile din `tcoSegments`, deci un segment nou nu compilează fără culoare. */
const SEGMENT_CLASS: Record<TcoSegment["id"], string> = {
  depreciation: "bg-chart-3",
  energy: "bg-chart-2",
  fixed: "bg-chart-1"
};

export function TcoBreakdown({ tco, label }: { tco: Tco; label: string }) {
  const segments = tcoSegments(tco);
  const description = segments.map(segment => `${segment.label} ${formatEur(segment.valueEur)}`).join(", ");

  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex items-baseline justify-between gap-2 text-xs">
        <span className="truncate font-medium">{label}</span>
        <span className="shrink-0 font-mono tabular-nums">
          <GlossaryTerm id="tco">TCO</GlossaryTerm> {formatEur(tco.totalCostEur)}
        </span>
      </div>
      <div
        role="img"
        aria-label={`Descompunerea costului total pentru ${label}: ${description}`}
        className="flex h-2.5 w-full gap-px overflow-hidden rounded-full bg-muted"
      >
        {segments
          .filter(segment => segment.share > 0)
          .map(segment => (
            <div key={segment.id} className={SEGMENT_CLASS[segment.id]} style={{ width: `${segment.share * 100}%` }} />
          ))}
      </div>
      <ul className="flex flex-wrap gap-x-3 gap-y-0.5 text-[11px] text-muted-foreground">
        {segments.map(segment => (
          <li key={segment.id} className="flex items-center gap-1">
            <span className={`size-2 rounded-sm ${SEGMENT_CLASS[segment.id]}`} aria-hidden="true" />
            {segment.glossary ? <GlossaryTerm id={segment.glossary}>{segment.label}</GlossaryTerm> : segment.label}
            <span className="font-mono tabular-nums">{formatEur(segment.valueEur)}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
