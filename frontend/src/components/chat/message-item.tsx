"use client";

/**
 * Un singur mesaj din conversație: bula de text plus, pentru consilier, activitatea uneltelor, cardurile și clasamentul.
 *
 * DE CE răspunsul consilierului nu e doar text: agentul cheamă unelte deterministe, iar rezultatul lor (carduri,
 * clasament) ajunge pe ecran direct, nu prin textul modelului. Textul modelului trece prin singura componentă Markdown.
 *
 * DE CE user la dreapta și assistant la stânga: e convenția pe care o citește oricine fără explicații.
 */

import { Copy } from "lucide-react";
import { toast } from "sonner";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { ComparisonSummary } from "@/components/chat/comparison-summary";
import { Markdown } from "@/components/chat/markdown";
import { ToolActivityChips } from "@/components/chat/tool-activity-chips";
import { VehicleCards } from "@/components/chat/vehicle-cards";
import type { ChatMessage } from "@/lib/message-utils";
import { cn } from "@/lib/utils";

export function MessageItem({ message }: { message: ChatMessage }) {
  const isUser = message.role === "user";
  const hasExtras = message.tools.length > 0 || message.cards.length > 0 || message.comparison !== null;

  // Răspunsul abia început nu are încă nimic de arătat; indicatorul „scrie…” din listă ocupă locul lui.
  if (!isUser && !message.content && !hasExtras) return null;

  /**
   * DE CE copierea are nevoie de `try/catch`: `navigator.clipboard` cere context securizat (https sau localhost) și
   * poate fi refuzată de browser; fără tratare, butonul ar părea stricat, fără nicio explicație.
   */
  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(message.content);
      toast.success("Mesaj copiat");
    } catch {
      toast.error("Copierea nu este disponibilă (doar pe localhost sau HTTPS).");
    }
  }

  return (
    <div className={cn("flex gap-3", isUser ? "flex-row-reverse" : "flex-row")}>
      <Avatar className="mt-0.5 size-7 shrink-0">
        <AvatarFallback className={cn("text-xs", isUser ? "bg-primary text-primary-foreground" : "bg-muted")}>
          {isUser ? "TU" : "CPA"}
        </AvatarFallback>
      </Avatar>

      <div className={cn("flex max-w-[85%] min-w-0 flex-col gap-2", isUser ? "items-end" : "items-start")}>
        <span className="px-1 text-xs text-muted-foreground">{isUser ? "Tu" : "Consilier CPA"}</span>

        {!isUser && <ToolActivityChips tools={message.tools} />}
        {!isUser && <VehicleCards cards={message.cards} />}
        {!isUser && message.comparison && <ComparisonSummary report={message.comparison} />}

        {message.content && (
          // `group` face butonul de copiere să apară la hover; stă în interiorul bulei ca poziția lui să fie stabilă.
          <div
            className={cn(
              "group relative rounded-2xl px-4 py-3 text-sm leading-relaxed whitespace-pre-wrap",
              isUser ? "rounded-br-sm bg-primary text-primary-foreground" : "rounded-bl-sm bg-muted"
            )}
          >
            <Markdown>{message.content}</Markdown>

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
          </div>
        )}
      </div>
    </div>
  );
}
