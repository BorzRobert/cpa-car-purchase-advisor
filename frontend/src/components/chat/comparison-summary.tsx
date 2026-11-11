/**
 * Clasamentul trimis de agent (evenimentul `compare`): tabel cu scor total și bare pe criterii, apoi costul total
 * descompus pe fiecare variantă.
 *
 * DE CE tabel cu prima coloană fixă: pe 360 px nu încap patru variante lângă etichete; tabelul se derulează pe orizontală,
 * dar numele criteriului rămâne la vedere. Cifrele sunt exact cele din rezultatul uneltei, deci coincid cu ce citează
 * modelul în text; nimic nu se recalculează aici.
 */
import { ScoreBar } from "@/components/chat/score-bar";
import { TcoBreakdown } from "@/components/chat/tco-breakdown";
import { GlossaryTerm } from "@/components/help/glossary-term";
import type { ComparisonReport } from "@/lib/advisor-events";
import { formatEur } from "@/lib/profile-options";

export function ComparisonSummary({ report }: { report: ComparisonReport }) {
  const byId = new Map(report.vehicles.map(card => [card.variantId, card]));
  const nameOf = (variantId: string) => {
    const card = byId.get(variantId);
    return card ? `${card.make} ${card.model} · ${card.variantName}` : variantId;
  };

  return (
    <section
      aria-label="Comparație"
      className="flex w-full min-w-0 flex-col gap-4 rounded-lg border bg-card p-3 text-card-foreground"
    >
      <div className="overflow-x-auto">
        <table className="w-full min-w-max border-separate border-spacing-0 text-left text-xs">
          <caption className="pb-2 text-left text-muted-foreground">
            Clasament pe {report.years} ani, {report.kmPerYear.toLocaleString("ro-RO")} km/an · estimare orientativă
            {report.dataAsOf ? `, date la ${report.dataAsOf}` : ""}
          </caption>
          <thead>
            <tr>
              <th scope="col" className="sticky left-0 bg-card py-1.5 pr-3 font-medium">
                Criteriu
              </th>
              {report.scores.map((score, index) => {
                const card = byId.get(score.variantId);
                return (
                  <th key={score.variantId} scope="col" className="px-2 py-1.5 text-left align-top font-medium">
                    <span className="font-mono text-muted-foreground tabular-nums">{index + 1}.</span>{" "}
                    {card ? `${card.make} ${card.model}` : score.variantId}
                    {card && (
                      <span className="block text-[11px] font-normal text-muted-foreground">{card.variantName}</span>
                    )}
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody>
            <tr>
              <th scope="row" className="sticky left-0 border-t bg-card py-2 pr-3 font-semibold">
                Scor total
              </th>
              {report.scores.map(score => (
                <td key={score.variantId} className="border-t px-2 py-2">
                  <ScoreBar label={`Scor total, ${nameOf(score.variantId)}`} score={score.total} />
                </td>
              ))}
            </tr>
            {report.criteria.map(criterion => (
              <tr key={criterion.id}>
                <th scope="row" className="sticky left-0 border-t bg-card py-2 pr-3 font-normal">
                  {criterion.label}
                  <span className="ml-1 text-muted-foreground">({criterion.weight}%)</span>
                </th>
                {report.scores.map(score => {
                  const entry = score.breakdown.find(item => item.criterion === criterion.id);
                  return (
                    <td key={score.variantId} className="border-t px-2 py-2" title={entry?.detail}>
                      <ScoreBar
                        label={`${criterion.label}, ${nameOf(score.variantId)}`}
                        score={entry && entry.applicable ? entry.score : null}
                      />
                    </td>
                  );
                })}
              </tr>
            ))}
            <tr>
              <th scope="row" className="sticky left-0 border-t bg-card py-2 pr-3 font-normal">
                <GlossaryTerm id="tco">Cost total</GlossaryTerm>
              </th>
              {report.scores.map(score => (
                <td key={score.variantId} className="border-t px-2 py-2 font-mono tabular-nums">
                  {formatEur(score.tco.totalCostEur)}
                </td>
              ))}
            </tr>
            <tr>
              <th scope="row" className="sticky left-0 border-t bg-card py-2 pr-3 font-normal">
                Pe lună
              </th>
              {report.scores.map(score => (
                <td key={score.variantId} className="border-t px-2 py-2 font-mono tabular-nums">
                  {formatEur(score.tco.costPerMonthEur)}
                </td>
              ))}
            </tr>
          </tbody>
        </table>
      </div>

      <div className="flex flex-col gap-3">
        <h2 className="text-sm font-semibold">Din ce se compune costul total</h2>
        {report.scores.map(score => (
          <TcoBreakdown key={score.variantId} tco={score.tco} label={nameOf(score.variantId)} />
        ))}
      </div>
    </section>
  );
}
