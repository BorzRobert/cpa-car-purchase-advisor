"use client";

/**
 * Lista de mesaje a conversației active, plus stările ei (încărcare, „scrie…”).
 *
 * DE CE lista e separată de `chat.tsx`: `chat.tsx` decide CE se afișează (ecran de start sau conversație); aici e
 * strict CUM arată o conversație și cum se comportă la derulare.
 */

import { useEffect, useRef } from "react";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Skeleton } from "@/components/ui/skeleton";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { MessageItem } from "@/components/chat/message-item";
import type { ChatMessage } from "@/lib/message-utils";

type MessageListProps = {
  messages: ChatMessage[];
  streaming: boolean;
  isLoading: boolean;
};

export function MessageList({ messages, streaming, isLoading }: MessageListProps) {
  const bottomRef = useRef<HTMLDivElement>(null);

  // Indicatorul „scrie…” apare doar cât răspunsul nu are încă nimic de arătat (nici text, nici activitate de unelte).
  const last = messages.at(-1);
  const waiting =
    streaming && (last?.role !== "assistant" || (!last.content && last.tools.length === 0 && last.cards.length === 0));

  /**
   * Derularea automată la ultimul mesaj. `block: "end"` derulează doar containerul, nu toată pagina; depinde și de
   * `streaming` ca indicatorul să nu rămână sub marginea vizibilă.
   */
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages, streaming]);

  return (
    <ScrollArea className="min-h-0 flex-1 bg-background/40">
      <div className="mx-auto flex w-full max-w-3xl flex-col gap-6 px-4 py-6">
        {isLoading ? <MessagesSkeleton /> : messages.map(message => <MessageItem key={message.id} message={message} />)}

        {waiting && <TypingIndicator />}

        {/* Ancoră invizibilă pentru derulare. Mai fiabilă decât calculul manual al lui scrollTop. */}
        <div ref={bottomRef} />
      </div>
    </ScrollArea>
  );
}

/** Forme, nu un spinner: utilizatorul înțelege că urmează o listă de mesaje, iar saltul de layout e mai mic. */
function MessagesSkeleton() {
  return (
    <div className="flex flex-col gap-6" aria-hidden>
      {[0, 1].map(index => (
        <div key={index} className="flex flex-col gap-6">
          <div className="flex flex-row-reverse gap-3">
            <Skeleton className="size-7 rounded-full" />
            <Skeleton className="h-12 w-52 rounded-2xl" />
          </div>
          <div className="flex gap-3">
            <Skeleton className="size-7 rounded-full" />
            <div className="flex flex-col gap-2">
              <Skeleton className="h-4 w-[min(28rem,70vw)]" />
              <Skeleton className="h-4 w-[min(22rem,60vw)]" />
              <Skeleton className="h-4 w-40" />
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

/** `aria-live="polite"`: cititorul de ecran află că a început răspunsul, fără să fie întrerupt în mijlocul altei citiri. */
function TypingIndicator() {
  return (
    <div className="flex gap-3" aria-live="polite">
      <Avatar className="mt-0.5 size-7 shrink-0">
        <AvatarFallback className="bg-muted text-xs">CPA</AvatarFallback>
      </Avatar>
      <div className="flex items-center gap-1.5 rounded-2xl rounded-bl-sm bg-muted px-4 py-4">
        <span className="sr-only">Consilierul scrie…</span>
        {[0, 150, 300].map(delay => (
          <span
            key={delay}
            className="size-1.5 animate-bounce rounded-full bg-muted-foreground/60"
            style={{ animationDelay: `${delay}ms` }}
          />
        ))}
      </div>
    </div>
  );
}
