"use client";

/**
 * Header-ul zonei centrale. Intenționat aproape gol.
 *
 * DE CE există totuși:
 * pe ecran mic sidebar-ul e ascuns, deci trebuie să existe un loc fix din care se deschide
 * (`SidebarTrigger`). Iar când derulezi o conversație lungă, titlul de sus e singurul indiciu
 * despre unde te afli.
 *
 * DE CE NU are bară de acțiuni și, în special, NU are buton de temă:
 * cerința de produs e ca centrul să conțină doar strictul necesar. Tema se schimbă din
 * preferințe → General → Appearance, într-un singur loc. Un comutator de temă în header e
 * tentant („e la un click") dar ocupă permanent spațiu pentru o acțiune făcută o dată la
 * câteva luni.
 */

import { Download, Eraser, MoreHorizontal } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger
} from "@/components/ui/dropdown-menu";
import { SidebarTrigger } from "@/components/ui/sidebar";
import { useAppStore } from "@/store/useAppStore";

type AppHeaderProps = {
  onExport: (format: "json" | "markdown") => void;
  onClear: () => void;
  canClear: boolean;
};

export function AppHeader({ onExport, onClear, canClear }: AppHeaderProps) {
  const conversations = useAppStore(state => state.conversations);
  const activeConversationId = useAppStore(state => state.activeConversationId);

  const activeTitle = conversations.find(conversation => conversation.id === activeConversationId)?.title;

  return (
    <header className="flex h-12 shrink-0 items-center gap-2 border-b px-2">
      <SidebarTrigger aria-label="Comută sidebar-ul" />
      {/* `truncate` fiindcă titlul vine din primul mesaj al utilizatorului: poate fi oricât de lung. */}
      <span className="truncate text-sm text-muted-foreground">{activeTitle ?? "Conversație nouă"}</span>
      <DropdownMenu>
        <DropdownMenuTrigger
          render={<Button variant="ghost" size="icon" className="ml-auto" aria-label="Acțiuni conversație" />}
        >
          <MoreHorizontal />
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-48">
          <DropdownMenuItem onClick={() => onExport("markdown")}>
            <Download />
            Exportă Markdown
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => onExport("json")}>
            <Download />
            Exportă JSON
          </DropdownMenuItem>
          <DropdownMenuItem variant="destructive" disabled={!canClear} onClick={onClear}>
            <Eraser />
            Golește conversația
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </header>
  );
}
