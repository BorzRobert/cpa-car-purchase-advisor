"use client";

/**
 * Preferințe → Lista scurtă: variantele salvate de cumpărător (sau de agent, prin unealta `addToShortlist`).
 *
 * DE CE aici și nu o pagină: panoul lateral cu lista scurtă și TCO-ul curent vine în Faza 7; până atunci lista trebuie
 * totuși să fie vizibilă și editabilă, ca fluxul „adaugă din card → vezi → scoate” să funcționeze cap-coadă.
 */
import { Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useRemoveFromShortlist, useShortlist } from "@/hooks/use-shortlist";
import { describeError } from "@/lib/api/errors";
import { POWERTRAIN_OPTIONS, formatEur, labelOf } from "@/lib/profile-options";

export function ShortlistPanel() {
  const shortlist = useShortlist();
  const remove = useRemoveFromShortlist();

  if (shortlist.isError) {
    return (
      <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
        <p>Nu am putut încărca lista scurtă.</p>
        <Button variant="outline" size="sm" onClick={() => void shortlist.refetch()}>
          Reîncearcă
        </Button>
      </div>
    );
  }

  if (!shortlist.data) {
    return <Skeleton className="h-16 w-full" aria-hidden />;
  }

  if (shortlist.data.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        Lista scurtă e goală. Cere consilierului să caute mașini și apasă „Lista scurtă” pe cardul unei variante.
      </p>
    );
  }

  return (
    <ul className="flex flex-col gap-2">
      {shortlist.data.map(item => {
        const vehicle = item.vehicle;
        const variant = vehicle?.variants?.[0];
        return (
          <li key={item.id} className="flex items-center gap-3 rounded-lg border p-3">
            <div className="flex min-w-0 flex-1 flex-col gap-1">
              <span className="truncate text-sm font-medium">
                {vehicle?.make} {vehicle?.model}
                {variant?.name ? ` · ${variant.name}` : ""}
              </span>
              <div className="flex flex-wrap items-center gap-1.5 text-xs text-muted-foreground">
                {variant?.powertrain && (
                  <Badge variant="secondary">{labelOf(POWERTRAIN_OPTIONS, variant.powertrain)}</Badge>
                )}
                {variant?.priceEur !== undefined && (
                  <span>
                    {formatEur(variant.priceEur)} (orientativ{variant.dataAsOf ? `, date la ${variant.dataAsOf}` : ""})
                  </span>
                )}
              </div>
            </div>
            <Button
              variant="ghost"
              size="icon-sm"
              aria-label="Scoate din lista scurtă"
              disabled={remove.isPending}
              onClick={() =>
                item.id &&
                remove.mutate(item.id, {
                  onSuccess: () => toast.success("Scos din lista scurtă"),
                  onError: error => toast.error(describeError(error).title)
                })
              }
            >
              <Trash2 />
            </Button>
          </li>
        );
      })}
    </ul>
  );
}
