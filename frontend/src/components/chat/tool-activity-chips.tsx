/**
 * Chip-urile de activitate ale agentului („Caut mașini potrivite…”, „Am găsit 5 variante”).
 *
 * DE CE există: o unealtă poate dura câteva secunde, iar fără un semn vizibil utilizatorul crede că aplicația a înghețat.
 * `summary` vine deja în română de la backend, gata de afișat.
 */
import { CircleCheck, Loader2 } from "lucide-react";
import type { ToolActivity } from "@/lib/message-utils";

export function ToolActivityChips({ tools }: { tools: ToolActivity[] }) {
  if (tools.length === 0) return null;

  return (
    <ul className="flex flex-wrap gap-1.5" aria-label="Activitatea consilierului">
      {tools.map((tool, index) => (
        <li
          key={`${tool.name}-${index}`}
          className="flex items-center gap-1.5 rounded-full border bg-background px-2.5 py-1 text-xs text-muted-foreground"
        >
          {tool.status === "started" ? (
            <Loader2 className="size-3 animate-spin" aria-hidden="true" />
          ) : (
            <CircleCheck className="size-3 text-emerald-500" aria-hidden="true" />
          )}
          {tool.summary}
        </li>
      ))}
    </ul>
  );
}
