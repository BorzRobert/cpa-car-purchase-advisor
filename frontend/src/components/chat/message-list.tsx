"use client";

/**
 * Lista de mesaje a conversației active, plus stările ei.
 *
 * DE CE lista e separată de `chat.tsx`:
 * `chat.tsx` decide CE se afișează (ecran de start sau conversație). Aici e strict CUM arată o
 * conversație și cum se comportă la derulare. Sunt două motive diferite de schimbare, deci
 * două fișiere.
 *
 * DE CE conține și `Skeleton` și indicatorul „scrie…":
 * sunt stări ale aceleiași liste. Ținute aici, nu pot fi uitate la Faza 3 — când mesajele vor
 * veni prin streaming, componenta rămâne aceeași, doar sursa datelor se schimbă.
 */

import { useEffect, useRef } from "react";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Skeleton } from "@/components/ui/skeleton";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { MessageItem } from "@/components/chat/message-item";
import type { ChatStatus } from "ai";
import { addCosts } from "@/lib/cost";
import type { AppUIMessage } from "@/lib/types";

type MessageListProps = {
  messages: AppUIMessage[];
  status: ChatStatus;
  userInitials: string;
  isLoading: boolean;
  onRegenerate: (messageId: string) => void;
  onEdit: (messageId: string, text: string) => void;
};

export function MessageList({ messages, status, userInitials, isLoading, onRegenerate, onEdit }: MessageListProps) {
  const bottomRef = useRef<HTMLDivElement>(null);
  const assistantCosts = messages.flatMap(message =>
    message.role === "assistant" && message.metadata ? [message.metadata.cost] : []
  );
  const conversationCost = addCosts(assistantCosts);

  function formatCost(value: number | undefined): string {
    return value === undefined ? "—" : `$${value.toFixed(6)}`;
  }

  /**
   * Derularea automată la ultimul mesaj.
   * DE CE depinde și de `status`: când apare indicatorul „scrie…", lista crește în înălțime;
   * fără dependența asta, indicatorul ar rămâne sub marginea vizibilă și ai crede că aplicația
   * n-a făcut nimic. `block: "end"` derulează doar containerul, nu toată pagina.
   */
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages, status]);

  return (
    <ScrollArea className="min-h-0 flex-1 bg-background/40">
      <div className="mx-auto flex w-full max-w-3xl flex-col gap-6 px-4 py-6">
        {assistantCosts.length > 0 && (
          <div className="self-end text-right text-xs text-muted-foreground" aria-label="Costul conversației">
            <div>Conversație: {formatCost(conversationCost.total)}</div>
            <div>
              {formatCost(conversationCost.input)} intrare (istoric + întrebări) · {formatCost(conversationCost.output)}{" "}
              răspunsuri
            </div>
          </div>
        )}
        {isLoading ? <MessagesSkeleton /> : null}

        {!isLoading &&
          messages.map((message, index) => (
            <MessageItem
              key={message.id}
              message={message}
              userInitials={userInitials}
              onRegenerate={onRegenerate}
              onEdit={onEdit}
              canRegenerate={message.role === "assistant" && index === messages.length - 1 && status === "ready"}
              canEdit={message.role === "user" && status === "ready"}
            />
          ))}

        {(status === "submitted" || status === "streaming") && <TypingIndicator />}

        {/* Ancoră invizibilă pentru derulare. Mai fiabilă decât calculul manual al lui scrollTop. */}
        <div ref={bottomRef} />
      </div>
    </ScrollArea>
  );
}

/**
 * Starea de încărcare.
 * DE CE forme, nu un spinner: un spinner spune „așteaptă", formele spun „vine o listă de
 * mesaje". Utilizatorul înțelege ce urmează, iar saltul de layout la apariția datelor e mai mic.
 */
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

/**
 * Indicatorul „scrie…".
 * DE CE trei puncte animate și nu textul „se încarcă": ocupă exact locul unde va apărea bula de
 * răspuns, deci nu mai există un salt când textul începe să curgă.
 * DE CE `aria-live="polite"`: cine folosește cititor de ecran trebuie să afle că a început
 * răspunsul, fără să fie întrerupt în mijlocul altei citiri.
 */
function TypingIndicator() {
  return (
    <div className="flex gap-3" aria-live="polite">
      <Avatar className="mt-0.5 size-7 shrink-0">
        <AvatarFallback className="bg-muted text-xs">SF</AvatarFallback>
      </Avatar>
      <div className="flex items-center gap-1.5 rounded-2xl rounded-bl-sm bg-muted px-4 py-4">
        <span className="sr-only">SkillForge scrie…</span>
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
