"use client";

/**
 * Un singur mesaj din conversație.
 *
 * DE CE mesajul e o componentă proprie, nu doar un `<div>` în listă:
 * aici se va complica cel mai mult la pașii următori — Markdown, blocuri de cod, citări din
 * notițe (RAG), apeluri de unelte afișate în bulă. Dacă toată logica ar sta în `message-list`,
 * fișierul acela ar deveni de necitit exact când devine interesant.
 *
 * DE CE user la dreapta și assistant la stânga:
 * e convenția pe care o citește oricine fără explicații — „ce am spus eu" vs. „ce mi s-a
 * răspuns". Alinierea singură transmite rolul, fără să fie nevoie de etichete mari.
 */

import { Copy, RotateCcw } from "lucide-react";
import { toast } from "sonner";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { getMessageText } from "@/lib/message-utils";
import { cn } from "@/lib/utils";
import type { UIMessage } from "ai";

type MessageItemProps = {
  message: UIMessage;
  userInitials: string;
  onRegenerate: (messageId: string) => void;
  canRegenerate: boolean;
};

export function MessageItem({ message, userInitials, onRegenerate, canRegenerate }: MessageItemProps) {
  const isUser = message.role === "user";
  const text = getMessageText(message);

  /**
   * DE CE copierea are nevoie de `try/catch`:
   * `navigator.clipboard` cere context securizat (https sau localhost) și poate fi refuzată de
   * browser. Fără tratare, butonul ar părea pur și simplu stricat, fără nicio explicație.
   */
  async function handleCopy() {
    if (!navigator.clipboard) {
      toast.error("Copierea este disponibilă doar pe localhost sau HTTPS.");
      return;
    }

    try {
      await navigator.clipboard.writeText(text);
      toast.success("Mesaj copiat");
    } catch {
      toast.error("Browserul nu a permis copierea în clipboard.");
    }
  }

  return (
    <div className={cn("flex gap-3", isUser ? "flex-row-reverse" : "flex-row")}>
      <Avatar className="mt-0.5 size-7 shrink-0">
        <AvatarFallback className={cn("text-xs", isUser ? "bg-primary text-primary-foreground" : "bg-muted")}>
          {isUser ? userInitials : "SF"}
        </AvatarFallback>
      </Avatar>

      <div className={cn("flex max-w-[85%] flex-col gap-1", isUser ? "items-end" : "items-start")}>
        <span className="px-1 text-xs text-muted-foreground">{isUser ? "Tu" : "SkillForge"}</span>

        {/*
          `group` de aici e ce face butonul de copiere să apară la hover peste bulă.
          DE CE butonul stă ÎN interiorul bulei și nu lângă ea: lângă bulă ar muta textul la
          apariție și ar fi greu de nimerit pe telefon. În bulă, poziția lui e stabilă.
        */}
        <div
          className={cn(
            "group relative rounded-2xl px-4 py-3 text-sm leading-relaxed whitespace-pre-wrap",
            isUser ? "rounded-br-sm bg-primary text-primary-foreground" : "rounded-bl-sm bg-muted"
          )}
        >
          {text}

          {typeof navigator !== "undefined" && navigator.clipboard && (
            <Tooltip>
              <TooltipTrigger
                render={
                  <Button
                    variant="ghost"
                    size="icon-xs"
                    onClick={() => void handleCopy()}
                    aria-label="Copiază mesajul"
                    className={cn(
                      "absolute -top-1 opacity-0 transition-opacity group-hover:opacity-100 focus-visible:opacity-100",
                      isUser ? "-left-9" : "-right-9"
                    )}
                  />
                }
              >
                <Copy />
              </TooltipTrigger>
              <TooltipContent>Copiază</TooltipContent>
            </Tooltip>
          )}

          {!isUser && canRegenerate && (
            <Tooltip>
              <TooltipTrigger
                render={
                  <Button
                    variant="ghost"
                    size="icon-xs"
                    onClick={() => onRegenerate(message.id)}
                    aria-label="Încearcă din nou răspunsul"
                    className="absolute -right-9 -bottom-1 opacity-0 transition-opacity group-hover:opacity-100 focus-visible:opacity-100"
                  />
                }
              >
                <RotateCcw />
              </TooltipTrigger>
              <TooltipContent>Mai încearcă</TooltipContent>
            </Tooltip>
          )}
        </div>
      </div>
    </div>
  );
}
