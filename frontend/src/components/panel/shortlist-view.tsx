"use client";

/**
 * Lista scurtă: variantele salvate de cumpărător (sau de agent, prin unealta `addToShortlist`).
 *
 * DE CE o componentă folosită în două locuri: stă în panoul lateral și în Preferințe → Lista scurtă; o singură
 * implementare înseamnă că „scoate din listă” și „date la” se comportă la fel peste tot. Titlul îl pune locul în care apare.
 */
import { Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Price } from "@/components/chat/price";
import { useRemoveFromShortlist, useShortlist } from "@/hooks/use-shortlist";
import { describeError } from "@/lib/api/errors";
import { POWERTRAIN_OPTIONS, labelOf } from "@/lib/profile-options";

export function ShortlistView() {
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
    return (
      <div className="flex flex-col gap-2" aria-hidden>
        <Skeleton className="h-14 w-full" />
        <Skeleton className="h-14 w-full" />
      </div>
    );
  }

  if (shortlist.data.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        Lista scurtă e goală. Cere consilierului să caute mașini și apasă „Adaugă în lista scurtă” pe cardul unei
        variante.
      </p>
    );
  }

  return (
    <ul className="flex flex-col gap-2">
      {shortlist.data.map(item => {
        const vehicle = item.vehicle;
        const variant = vehicle?.variants?.[0];
        const name = `${vehicle?.make ?? ""} ${vehicle?.model ?? ""}`.trim();
        return (
          <li key={item.id} className="flex items-center gap-2 rounded-md border bg-card p-2.5">
            <div className="flex min-w-0 flex-1 flex-col gap-1">
              <span className="truncate text-sm font-medium">
                {name}
                {variant?.name ? ` · ${variant.name}` : ""}
              </span>
              <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
                {variant?.powertrain && (
                  <Badge variant="secondary">{labelOf(POWERTRAIN_OPTIONS, variant.powertrain)}</Badge>
                )}
                {variant?.priceEur !== undefined && <Price valueEur={variant.priceEur} dataAsOf={variant.dataAsOf} />}
              </div>
            </div>
            <Button
              variant="ghost"
              size="icon-sm"
              aria-label={`Scoate ${name || "varianta"} din lista scurtă`}
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
