/**
 * „Costul total” curent în panoul lateral: descompunerea TCO pentru varianta de pe primul loc din ultima comparație.
 *
 * DE CE doar prima variantă: panoul e un rezumat care rămâne vizibil cât scrii; comparația completă, cu toate variantele,
 * stă în conversație. Cifrele sunt cele din rezultatul uneltei, nu un calcul propriu.
 */
import { TcoBreakdown } from "@/components/chat/tco-breakdown";
import type { ComparisonReport } from "@/lib/advisor-events";

export function RunningTco({ report }: { report: ComparisonReport | null }) {
  const best = report?.scores[0];
  if (!report || !best) {
    return (
      <p className="text-sm text-muted-foreground">
        Cere o comparație („Compară un Golf cu un Corolla pe 5 ani”) și costul total apare aici.
      </p>
    );
  }

  const card = report.vehicles.find(vehicle => vehicle.variantId === best.variantId);
  const label = card ? `${card.make} ${card.model} · ${card.variantName}` : best.variantId;

  return (
    <div className="flex flex-col gap-2">
      <TcoBreakdown tco={best.tco} label={`${label} (locul 1)`} />
      <p className="text-xs text-muted-foreground">
        Pe {report.years} ani, {report.kmPerYear.toLocaleString("ro-RO")} km/an · estimare orientativă
        {report.dataAsOf ? `, date la ${report.dataAsOf}` : ""}
      </p>
    </div>
  );
}
