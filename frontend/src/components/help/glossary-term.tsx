"use client";

/**
 * Un termen financiar cu definiția într-un tooltip (TCO, DAE, depreciere).
 *
 * DE CE buton și nu text subliniat: un tooltip se deschide și de la tastatură (focus) doar dacă ținta e focusabilă.
 * Definiția vine din registrul `lib/glossary.ts`, aceeași sursă ca tab-ul „Glosar” din ghid.
 */
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { glossaryEntry, type GlossaryId } from "@/lib/glossary";

export function GlossaryTerm({ id, children }: { id: GlossaryId; children: React.ReactNode }) {
  const entry = glossaryEntry(id);

  return (
    <Tooltip>
      <TooltipTrigger
        render={
          <button
            type="button"
            className="cursor-help underline decoration-dotted underline-offset-4 focus-visible:rounded-sm focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
          />
        }
      >
        {children}
      </TooltipTrigger>
      <TooltipContent className="max-w-64 text-balance">{entry.short}</TooltipContent>
    </Tooltip>
  );
}
