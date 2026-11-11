"use client";

/**
 * Header-ul zonei centrale. Intenționat aproape gol.
 *
 * DE CE există totuși: pe ecran mic sidebar-ul e ascuns, deci trebuie să existe un loc fix din care se deschide
 * (`SidebarTrigger`). Iar când derulezi o conversație lungă, titlul de sus e singurul indiciu despre unde te afli.
 *
 * DE CE NU are bară de acțiuni și NU are buton de temă: cerința de produs e ca centrul să conțină doar strictul
 * necesar. Tema se schimbă din Preferințe → General → Appearance, într-un singur loc.
 */

import { SidebarTrigger } from "@/components/ui/sidebar";
import { PanelTrigger } from "@/components/panel/panel-trigger";
import { useConversations } from "@/hooks/use-conversations";
import { useAppStore } from "@/store/useAppStore";

export function AppHeader() {
  const activeConversationId = useAppStore(state => state.activeConversationId);
  const { data: conversations } = useConversations();

  const activeTitle = conversations?.find(conversation => conversation.id === activeConversationId)?.title;

  return (
    <header className="flex h-12 shrink-0 items-center gap-2 border-b px-2">
      <SidebarTrigger aria-label="Comută sidebar-ul" />
      {/* Titlul conversației e și `h1`-ul paginii: ecranul de start și cel de conversație au astfel același antet principal. */}
      <h1 className="truncate font-sans text-sm font-normal text-muted-foreground">
        {activeTitle ?? "Conversație nouă"}
      </h1>
      <PanelTrigger />
    </header>
  );
}
