"use client";

/** Glosarul din ghid: aceleași definiții ca tooltip-urile din interfață (`lib/glossary.ts`). */
import { GLOSSARY } from "@/lib/glossary";
import type { GuideBlock } from "@/lib/guide";

export function GuideBlockGlossary({ block }: { block: GuideBlock }) {
  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-col gap-1">
        <h3 className="text-sm font-semibold">{block.title}</h3>
        <p className="text-sm text-muted-foreground">{block.body}</p>
      </div>
      <dl className="flex flex-col gap-3">
        {GLOSSARY.map(entry => (
          <div key={entry.id} className="flex flex-col gap-0.5">
            <dt className="text-sm font-medium">{entry.term}</dt>
            <dd className="text-sm text-muted-foreground">{entry.long}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
}
