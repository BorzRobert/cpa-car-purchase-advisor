"use client";

/**
 * Zona centrală: decide dacă vezi ecranul de start sau o conversație, și ține ciorna de input.
 *
 * DE CE decizia stă aici și nu în pagină: pagina compune zonele (sidebar / header / centru) și nu trebuie să știe nimic
 * despre starea chat-ului. Aici e singurul loc care întreabă „există o conversație deschisă?”.
 *
 * DE CE zona centrală NU are bară de acțiuni: cerința de produs e ca în centru să fie doar strictul necesar. Acțiunile
 * pe conversație (redenumire, ștergere) stau în sidebar, pe fiecare rând.
 */

import { useRef, useState } from "react";
import { ChatErrorAlert } from "@/components/chat/chat-error-alert";
import { ChatInput } from "@/components/chat/chat-input";
import { EmptyState } from "@/components/chat/empty-state";
import { MessageList } from "@/components/chat/message-list";
import { AppHeader } from "@/components/layout/app-header";
import { useAdvisorChat } from "@/hooks/use-advisor-chat";
import { useAppStore } from "@/store/useAppStore";

export function Chat() {
  const activeConversationId = useAppStore(state => state.activeConversationId);
  const { messages, streaming, loading, error, historyError, send, stop, dismissError } =
    useAdvisorChat(activeConversationId);
  const [draft, setDraft] = useState("");
  const composerWrapperRef = useRef<HTMLDivElement>(null);

  async function handleSubmit() {
    const text = draft.trim();
    if (!text || streaming) return;
    setDraft("");
    const result = await send(text);
    // O cerere respinsă (limită, consilier neactivat) nu a salvat mesajul: îl punem înapoi, ca să nu-l scrie din nou.
    if (result === "rejected") setDraft(current => current || text);
    requestAnimationFrame(() => composerWrapperRef.current?.querySelector("textarea")?.focus());
  }

  /** După click pe o sugestie, cursorul trebuie să fie deja în text ca să poți ajusta întrebarea. */
  function handlePickSuggestion(prompt: string) {
    setDraft(prompt);
    composerWrapperRef.current?.querySelector("textarea")?.focus();
  }

  const composer = (
    <div ref={composerWrapperRef}>
      <ChatInput
        value={draft}
        onValueChange={setDraft}
        onSubmit={() => void handleSubmit()}
        streaming={streaming}
        onStop={stop}
      />
    </div>
  );

  const shownError = error ?? historyError;

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <AppHeader />
      {shownError && <ChatErrorAlert error={shownError} onDismiss={error ? dismissError : undefined} />}

      {activeConversationId !== null ? (
        <>
          <MessageList messages={messages} streaming={streaming} isLoading={loading} />
          {/* Composer-ul lipit jos: zona de mesaje se derulează, cutia rămâne mereu la îndemână. */}
          <div className="border-t bg-background/80 supports-backdrop-filter:backdrop-blur">
            <div className="mx-auto w-full max-w-3xl px-4 py-4">{composer}</div>
          </div>
        </>
      ) : (
        <EmptyState onPickSuggestion={handlePickSuggestion} composer={composer} />
      )}
    </div>
  );
}
