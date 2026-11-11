/**
 * Clasamentul trimis de agent (evenimentul `compare`), într-o formă minimă: o tabelă cu scorul total și costul pe lună.
 *
 * DE CE minimă: widget-ul de comparație cu bare pe criterii și TCO este treaba Fazei 7. Cifrele sunt exact cele din
 * rezultatul uneltei, deci coincid cu ce citează modelul în text.
 */
import type { ComparisonReport } from "@/lib/advisor-events";
import { formatEur } from "@/lib/profile-options";

export function ComparisonSummary({ report }: { report: ComparisonReport }) {
  const byId = new Map(report.vehicles.map(card => [card.variantId, card]));

  return (
    <div className="overflow-x-auto rounded-lg border bg-background">
      <table className="w-full text-left text-xs">
        <caption className="px-3 py-2 text-left text-muted-foreground">
          Clasament pe {report.years} ani, {report.kmPerYear.toLocaleString("ro-RO")} km/an · estimare orientativă
          {report.dataAsOf ? `, date la ${report.dataAsOf}` : ""}
        </caption>
        <thead className="border-y bg-muted/50">
          <tr>
            <th className="px-3 py-1.5 font-medium">#</th>
            <th className="px-3 py-1.5 font-medium">Variantă</th>
            <th className="px-3 py-1.5 font-medium">Scor</th>
            <th className="px-3 py-1.5 font-medium">Cost total</th>
            <th className="px-3 py-1.5 font-medium">Pe lună</th>
          </tr>
        </thead>
        <tbody>
          {report.scores.map((score, index) => {
            const card = byId.get(score.variantId);
            return (
              <tr key={score.variantId} className="border-b last:border-0">
                <td className="px-3 py-1.5">{index + 1}</td>
                <td className="px-3 py-1.5">
                  {card ? `${card.make} ${card.model} · ${card.variantName}` : score.variantId}
                </td>
                <td className="px-3 py-1.5">{score.total}/100</td>
                <td className="px-3 py-1.5">{formatEur(score.tco.totalCostEur)}</td>
                <td className="px-3 py-1.5">{formatEur(score.tco.costPerMonthEur)}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
