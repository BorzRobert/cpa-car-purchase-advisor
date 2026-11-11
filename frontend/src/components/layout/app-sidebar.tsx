"use client";

/**
 * Sidebar-ul: „+ Nouă” sus, lista de conversații la mijloc, starea backend-ului și preferințele jos.
 *
 * DE CE sus e UN SINGUR buton: CPA face un lucru — te ajută să alegi o mașină. Un meniu cu funcții care nu există ar
 * dilua mesajul. Restul (nevoi, listă scurtă, temă) sunt setări, deci stau în preferințe.
 *
 * DE CE lista de conversații vine din backend: memoria între sesiuni e o cerință de bază, iar backend-ul e arhiva ei.
 * Lista este dovada vizibilă că aplicația ține minte, de aceea ocupă tot spațiul dintre buton și subsol.
 *
 * DE CE folosim componenta `sidebar` de la shadcn: pe ecran mic devine un `Sheet`, are stare deschis/închis, scurtătură
 * de tastatură și gestionează focusul.
 */

import { useState } from "react";
import { ChevronUp, MoreHorizontal, Pencil, Plus, Settings, Trash2 } from "lucide-react";
import { toast } from "sonner";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuAction,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar
} from "@/components/ui/sidebar";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger
} from "@/components/ui/dropdown-menu";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { BackendStatus } from "@/components/layout/backend-status";
import { useConversations, useDeleteConversation, useRenameConversation } from "@/hooks/use-conversations";
import { describeError } from "@/lib/api/errors";
import type { Conversation } from "@/lib/api/endpoints";
import { useAppStore } from "@/store/useAppStore";

export function AppSidebar() {
  const { isMobile, setOpenMobile } = useSidebar();

  const conversations = useConversations();
  const rename = useRenameConversation();
  const remove = useDeleteConversation();
  const activeConversationId = useAppStore(state => state.activeConversationId);
  const setActiveConversation = useAppStore(state => state.setActiveConversation);
  const openSettings = useAppStore(state => state.openSettings);

  /** Conversația aflată în redenumire. `null` = dialogul e închis. */
  const [renaming, setRenaming] = useState<Conversation | null>(null);
  const [renameValue, setRenameValue] = useState("");

  /** Pe telefon sidebar-ul acoperă conversația; dacă ar rămâne deschis după selecție, n-ai vedea rezultatul. */
  function closeOnMobile() {
    if (isMobile) setOpenMobile(false);
  }

  function handleNew() {
    setActiveConversation(null);
    closeOnMobile();
  }

  function handleSelect(id: string) {
    setActiveConversation(id);
    closeOnMobile();
  }

  function handleDelete(conversation: Conversation) {
    if (!conversation.id) return;
    remove.mutate(conversation.id, {
      // Confirmarea e necesară: rândul dispare din listă, iar fără mesaj n-ai cum să știi dacă ai șters ce voiai.
      onSuccess: () => toast.success(`Conversație ștearsă: „${conversation.title}"`),
      onError: error => toast.error(describeError(error).title)
    });
  }

  function submitRename() {
    if (!renaming?.id) return;
    rename.mutate(
      { id: renaming.id, title: renameValue.trim() },
      {
        onSuccess: () => toast.success("Titlu actualizat"),
        onError: error => toast.error(describeError(error).title)
      }
    );
    setRenaming(null);
  }

  return (
    <>
      <Sidebar>
        <SidebarHeader className="gap-3 p-3">
          <div className="flex items-center gap-2 px-1">
            {/* Marca, desenată din tokenii temei: se adaptează singură la temă și nu adaugă o cerere de rețea. */}
            <span className="grid size-6 place-items-center rounded-md bg-primary text-[11px] font-semibold text-primary-foreground">
              C
            </span>
            <span className="text-base font-semibold tracking-tight">CPA</span>
          </div>

          <Button onClick={handleNew} className="w-full justify-start gap-2">
            <Plus />
            Conversație nouă
          </Button>
        </SidebarHeader>

        {/*
          `overflow-hidden` pe container, derulare doar în `ScrollArea`: `SidebarContent` are implicit `overflow-auto`,
          iar cu o listă lungă am fi avut două zone derulabile una în alta.
        */}
        <SidebarContent className="overflow-hidden">
          <SidebarGroup className="min-h-0 flex-1">
            <SidebarGroupLabel>Conversații</SidebarGroupLabel>

            <ScrollArea className="min-h-0 flex-1">
              <SidebarMenu>
                {conversations.isPending &&
                  [0, 1, 2, 3, 4].map(index => (
                    <SidebarMenuItem key={index} className="px-2 py-1.5">
                      <Skeleton className="h-4 w-full" />
                    </SidebarMenuItem>
                  ))}

                {conversations.data?.map(conversation => (
                  <SidebarMenuItem key={conversation.id}>
                    <SidebarMenuButton
                      isActive={conversation.id === activeConversationId}
                      onClick={() => conversation.id && handleSelect(conversation.id)}
                      className="pr-8"
                    >
                      <span className="truncate">{conversation.title ?? "Conversație nouă"}</span>
                    </SidebarMenuButton>

                    {/* Meniul pe rând apare la hover: cinci rânduri cu câte un buton mereu vizibil ar face lista o bară de unelte. */}
                    <DropdownMenu>
                      <DropdownMenuTrigger
                        render={
                          <SidebarMenuAction
                            showOnHover
                            aria-label={`Acțiuni pentru ${conversation.title ?? "conversație"}`}
                          />
                        }
                      >
                        <MoreHorizontal />
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="start" side="right" className="w-44">
                        <DropdownMenuItem
                          onClick={() => {
                            setRenaming(conversation);
                            setRenameValue(conversation.title ?? "");
                          }}
                        >
                          <Pencil />
                          Redenumește
                        </DropdownMenuItem>
                        <DropdownMenuItem variant="destructive" onClick={() => handleDelete(conversation)}>
                          <Trash2 />
                          Șterge
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </SidebarMenuItem>
                ))}

                {conversations.isError && (
                  <div className="flex flex-col items-start gap-2 px-2 py-4 text-sm text-muted-foreground">
                    <p>Nu am putut încărca conversațiile.</p>
                    <Button variant="outline" size="xs" onClick={() => void conversations.refetch()}>
                      Reîncearcă
                    </Button>
                  </div>
                )}

                {conversations.data?.length === 0 && (
                  // Starea goală: fără ea, un utilizator care și-a șters tot ar vedea un panou pustiu.
                  <p className="px-2 py-6 text-sm text-muted-foreground">
                    Nicio conversație încă. Apasă <span className="font-medium text-foreground">Conversație nouă</span>{" "}
                    ca să începi.
                  </p>
                )}
              </SidebarMenu>
            </ScrollArea>
          </SidebarGroup>
        </SidebarContent>

        <SidebarFooter>
          {/* Starea backend-ului deasupra butonului de preferințe: informație de fundal, nu o acțiune. */}
          <BackendStatus />
          <SidebarMenu>
            <SidebarMenuItem>
              <SidebarMenuButton
                onClick={() => {
                  openSettings();
                  closeOnMobile();
                }}
                aria-label="Deschide preferințele"
              >
                <Settings />
                <span className="flex-1 truncate text-left">Preferințe</span>
                <ChevronUp className="text-muted-foreground" />
              </SidebarMenuButton>
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarFooter>
      </Sidebar>

      {/* Dialog și nu editare direct în listă: e explicit, are buton de anulare și se comportă previzibil pe mobil. */}
      <Dialog open={renaming !== null} onOpenChange={open => !open && setRenaming(null)}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Redenumește conversația</DialogTitle>
          </DialogHeader>
          <div className="flex flex-col gap-2">
            <Label htmlFor="rename-conversation">Titlu</Label>
            <Input
              id="rename-conversation"
              value={renameValue}
              maxLength={120}
              onChange={event => setRenameValue(event.target.value)}
              onKeyDown={event => {
                if (event.key === "Enter" && renameValue.trim()) submitRename();
              }}
              autoFocus
            />
          </div>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setRenaming(null)}>
              Anulează
            </Button>
            <Button onClick={submitRename} disabled={!renameValue.trim()}>
              Salvează
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
