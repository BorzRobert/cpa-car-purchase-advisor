"use client";

/**
 * Zona centrală: decide dacă vezi ecranul de start sau o conversație, și ține ciorna de input.
 *
 * DE CE decizia stă aici și nu în pagină:
 * pagina compune zonele (sidebar / header / centru) și nu trebuie să știe nimic despre starea
 * chat-ului. Aici e singurul loc care întreabă „există o conversație activă cu mesaje?".
 *
 * DE CE zona centrală NU are bară de acțiuni:
 * cerința de produs e explicită — în centru doar strictul necesar. Orice buton adăugat aici
 * (regenerează, exportă, șterge) fură atenția de la singurul lucru care contează: conversația.
 * Acțiunile pe conversație stau în sidebar, pe fiecare rând.
 */

import { useEffect, useRef, useState } from "react";
import { AlertCircle } from "lucide-react";
import { useChat } from "@ai-sdk/react";
import { DefaultChatTransport } from "ai";
import { Alert, AlertAction, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { ChatInput } from "@/components/chat/chat-input";
import { EmptyState } from "@/components/chat/empty-state";
import { MessageList } from "@/components/chat/message-list";
import { useAppStore, useStoreHydration } from "@/store/useAppStore";

/** Inițialele pentru avatar. Extrase o dată, ca să nu se recalculeze la fiecare mesaj din listă. */
function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "EU";
  return parts
    .slice(0, 2)
    .map(part => part[0]?.toUpperCase() ?? "")
    .join("");
}

function getChatErrorMessage(error: Error): string {
  try {
    const response: unknown = JSON.parse(error.message);
    if (
      response &&
      typeof response === "object" &&
      "error" in response &&
      (response as { error?: unknown }).error === "Cheia nu e configurată."
    ) {
      return "Cheia nu e configurată.";
    }
  } catch {
    // Un stream întrerupt nu trimite neapărat JSON; detaliile lui nu sunt pentru utilizator.
  }

  return "Modelul nu poate răspunde acum. Verifică cheia, modelul și limita de utilizare.";
}

export function Chat() {
  const [draft, setDraft] = useState("");
  const composerWrapperRef = useRef<HTMLDivElement>(null);
  const creatingConversationRef = useRef(false);

  const hydrated = useStoreHydration();
  const profile = useAppStore(state => state.profile);
  const activeConversationId = useAppStore(state => state.activeConversationId);
  const createConversation = useAppStore(state => state.createConversation);

  const { messages, status, sendMessage, stop, error, setMessages, clearError } = useChat({
    transport: new DefaultChatTransport({
      api: "/api/chat",
      prepareSendMessagesRequest: ({ messages, body }) => ({
        body: { ...body, messages, profile: useAppStore.getState().profile }
      })
    })
  });

  const initials = getInitials(profile.name);

  useEffect(() => {
    if (creatingConversationRef.current) {
      creatingConversationRef.current = false;
      return;
    }
    setMessages([]);
    clearError();
  }, [activeConversationId, clearError, setMessages]);

  function handleSubmit() {
    if (!draft.trim()) return;
    const text = draft.trim();
    if (activeConversationId === null) {
      // Schimbarea id-ului e doar crearea conversației curente, nu navigare spre alta.
      creatingConversationRef.current = true;
      createConversation(text);
    }
    sendMessage({ text });
    setDraft("");
    requestAnimationFrame(() => composerWrapperRef.current?.querySelector("textarea")?.focus());
  }

  /**
   * DE CE sugestia mută focusul în textarea:
   * după click, cursorul trebuie să fie deja în text ca să poți ajusta întrebarea. Fără asta,
   * ai un input completat pe care trebuie să-l mai și clichezi — un pas inutil.
   */
  function handlePickSuggestion(prompt: string) {
    setDraft(prompt);
    composerWrapperRef.current?.querySelector("textarea")?.focus();
  }

  const composer = (
    <div ref={composerWrapperRef}>
      <ChatInput
        value={draft}
        onValueChange={setDraft}
        onSubmit={handleSubmit}
        status={status}
        onStop={() => void stop()}
      />
    </div>
  );

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      {/*
        DE CE alerta stă deasupra conversației, nu în locul ei:
        o eroare de provider nu trebuie să șteargă de pe ecran ce ai discutat până atunci.
        Vezi problema ȘI contextul, deci poți reîncerca fără să pierzi nimic.
      */}
      {status === "error" && error && (
        <div className="mx-auto w-full max-w-3xl px-4 pt-4">
          <Alert variant="destructive">
            <AlertCircle />
            <AlertTitle>Răspunsul nu a putut fi generat</AlertTitle>
            <AlertDescription>{getChatErrorMessage(error)}</AlertDescription>
            <AlertAction>
              <Button variant="ghost" size="sm" onClick={clearError}>
                Am înțeles
              </Button>
            </AlertAction>
          </Alert>
        </div>
      )}

      {activeConversationId !== null && messages.length > 0 ? (
        <>
          <MessageList messages={messages} status={status} userInitials={initials} isLoading={!hydrated} />
          {/* Composer-ul lipit jos: zona de mesaje se derulează, cutia rămâne mereu la îndemână. */}
          <div className="border-t bg-background/80 supports-backdrop-filter:backdrop-blur">
            <div className="mx-auto w-full max-w-3xl px-4 py-4">{composer}</div>
          </div>
        </>
      ) : (
        <EmptyState
          userName={profile.name}
          profileGoal={profile.goal}
          onPickSuggestion={handlePickSuggestion}
          composer={composer}
        />
      )}
    </div>
  );
}
