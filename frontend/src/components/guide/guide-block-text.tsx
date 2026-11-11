"use client";

/** Un bloc de text simplu din ghid: titlu și paragraf. */
import type { GuideBlock } from "@/lib/guide";

export function GuideBlockText({ block }: { block: GuideBlock }) {
  return (
    <div className="flex flex-col gap-1">
      <h3 className="text-sm font-semibold">{block.title}</h3>
      <p className="text-sm text-muted-foreground">{block.body}</p>
    </div>
  );
}
