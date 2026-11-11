"use client";

/**
 * Composer-ul: cutia din care utilizatorul trimite un mesaj.
 *
 * DE CE e o componentă separată, folosită în două locuri: apare centrat pe ecranul de conversație nouă și lipit jos
 * într-o conversație existentă. Aceeași cutie, două poziții — comportamentul tastaturii nu trebuie să difere.
 *
 * DE CE e „controlată” (primește `value` din afară): sugestiile de pe ecranul de start pre-completează inputul, iar
 * textul e o ciornă de moment, nu date ale utilizatorului — nu stă în store (ar ajunge în `localStorage`).
 */

import { useEffect, useRef } from "react";
import { Send, Square } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";

/** Înălțimea maximă a cutiei de text. Peste ea, textul se derulează în loc să împingă ecranul. */
const MAX_HEIGHT_PX = 200;
/** Limita backend-ului pentru un mesaj (`PostMessageRequest.content`); un text mai lung se refuză, nu se taie. */
const MAX_MESSAGE_LENGTH = 4000;

type ChatInputProps = {
  value: string;
  onValueChange: (value: string) => void;
  onSubmit: () => void;
  streaming: boolean;
  onStop: () => void;
};

export function ChatInput({ value, onValueChange, onSubmit, streaming, onStop }: ChatInputProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  /**
   * Creșterea în înălțime odată cu textul. Se face din JavaScript fiindcă un `<textarea>` nu se poate dimensiona după
   * conținut din CSS: resetăm la `auto` ca `scrollHeight` să reflecte conținutul real, apoi fixăm înălțimea.
   */
  useEffect(() => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = "auto";
    el.style.height = `${Math.min(el.scrollHeight, MAX_HEIGHT_PX)}px`;
  }, [value]);

  /**
   * Enter trimite, Shift+Enter face rând nou. `isComposing` protejează scrierea cu diacritice sau IME: acolo Enter
   * confirmă caracterul, iar dacă am trimite mesajul, textul s-ar rupe la jumătate.
   */
  function handleKeyDown(event: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key !== "Enter" || event.shiftKey || event.nativeEvent.isComposing) return;
    event.preventDefault();
    if (streaming || !value.trim()) return;
    onSubmit();
  }

  return (
    <div className="rounded-2xl border bg-card shadow-sm transition-colors focus-within:border-ring/60">
      <Textarea
        ref={textareaRef}
        value={value}
        onChange={event => onValueChange(event.target.value)}
        onKeyDown={handleKeyDown}
        rows={1}
        maxLength={MAX_MESSAGE_LENGTH}
        placeholder="Descrie ce mașină cauți sau întreabă-mă orice despre cumpărare…"
        aria-label="Mesaj pentru consilier"
        // `resize-none` fiindcă înălțimea o gestionăm noi; fără bordură proprie, pentru că bordura o desenează cutia din jur.
        className="max-h-[200px] min-h-0 resize-none border-0 bg-transparent px-4 py-3 text-base shadow-none focus-visible:ring-0 md:text-sm dark:bg-transparent"
      />

      <div className="flex items-center justify-end px-2 pb-2">
        {/* Un singur buton care comută între „trimite” și „stop”: nu pot exista simultan. */}
        {streaming ? (
          <Button size="icon-sm" variant="secondary" onClick={onStop} aria-label="Oprește răspunsul">
            <Square />
          </Button>
        ) : (
          <Button size="icon-sm" onClick={onSubmit} disabled={!value.trim()} aria-label="Trimite mesajul">
            <Send />
          </Button>
        )}
      </div>
    </div>
  );
}
