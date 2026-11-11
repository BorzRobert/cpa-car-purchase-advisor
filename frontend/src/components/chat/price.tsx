/**
 * Prețul într-un font cu cifre de aceeași lățime, urmat mereu de „date la”.
 *
 * DE CE o componentă: contractul cere „date la” lângă ORICE preț; dacă fiecare loc ar scrie singur eticheta, unul ar
 * uita-o. Aici nu poți afișa un preț fără data cifrelor.
 */
import { formatEur } from "@/lib/profile-options";
import { cn } from "@/lib/utils";

type PriceProps = {
  valueEur: number;
  /** Data cifrelor din catalog; când lipsește, se afișează doar „orientativ”. */
  dataAsOf?: string | null;
  className?: string;
};

export function Price({ valueEur, dataAsOf, className }: PriceProps) {
  return (
    <span className={cn("inline-flex flex-wrap items-baseline gap-x-1.5", className)}>
      <span className="font-mono font-semibold tabular-nums">{formatEur(valueEur)}</span>
      <span className="text-xs text-muted-foreground">orientativ{dataAsOf ? ` · date la ${dataAsOf}` : ""}</span>
    </span>
  );
}
