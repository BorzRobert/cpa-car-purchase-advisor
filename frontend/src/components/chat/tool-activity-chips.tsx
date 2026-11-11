/**
 * Chip-urile de activitate ale agentului („Caut mașini potrivite…”, „Am găsit 5 variante”).
 *
 * DE CE există: o unealtă poate dura câteva secunde, iar fără un semn vizibil utilizatorul crede că aplicația a înghețat.
 * `summary` vine deja în română de la backend, gata de afișat. Iconița depinde de unealtă (registru), ca să se distingă
 * dintr-o privire „caut” de „calculez”.
 */
import {
  Calculator,
  CircleCheck,
  Landmark,
  ListChecks,
  Loader2,
  Scale,
  Search,
  UserRound,
  Zap,
  type LucideIcon
} from "lucide-react";
import type { ToolActivity } from "@/lib/message-utils";

/** Numele uneltelor sunt cele din `communication.md`; o unealtă necunoscută primește iconița de stare. */
const TOOL_ICONS: Record<string, LucideIcon> = {
  updateBuyerProfile: UserRound,
  searchVehicles: Search,
  compareVehicles: Scale,
  rankVehicles: Scale,
  calculateTco: Calculator,
  calculateFinancing: Landmark,
  evVsIceBreakEven: Zap,
  addToShortlist: ListChecks,
  getShortlist: ListChecks
};

export function ToolActivityChips({ tools }: { tools: ToolActivity[] }) {
  if (tools.length === 0) return null;

  return (
    <ul className="flex flex-wrap gap-1.5" aria-label="Activitatea consilierului">
      {tools.map((tool, index) => {
        const running = tool.status === "started";
        const Icon = running ? Loader2 : (TOOL_ICONS[tool.name] ?? CircleCheck);
        return (
          <li
            key={`${tool.name}-${index}`}
            className="flex items-center gap-1.5 rounded-full border bg-card px-2.5 py-1 text-xs text-muted-foreground"
          >
            <Icon
              className={running ? "size-3 animate-spin text-primary motion-reduce:animate-none" : "size-3 text-signal"}
              aria-hidden="true"
            />
            {tool.summary}
          </li>
        );
      })}
    </ul>
  );
}
