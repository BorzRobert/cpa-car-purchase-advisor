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

import { useEffect, useMemo, useRef, useState } from "react";
import { ChatErrorAlert } from "@/components/chat/chat-error-alert";
import { ChatInput } from "@/components/chat/chat-input";
import { EmptyState } from "@/components/chat/empty-state";
import { MessageList } from "@/components/chat/message-list";
import { AppHeader } from "@/components/layout/app-header";
import { SidePanel } from "@/components/panel/side-panel";
import { useAdvisorChat } from "@/hooks/use-advisor-chat";
import { useAppStore } from "@/store/useAppStore";

export function Chat() {
  const activeConversationId = useAppStore(state => state.activeConversationId);
  const { messages, streaming, loading, error, historyError, send, stop, dismissError } =
    useAdvisorChat(activeConversationId);
  const [draft, setDraft] = useState("");
  const composerWrapperRef = useRef<HTMLDivElement>(null);

  // Panoul lateral arată ultima comparație și dacă s-au găsit variante; se derivă din mesaje, nu se ține a doua copie.
  const comparison = useMemo(() => messages.findLast(message => message.comparison)?.comparison ?? null, [messages]);
  const hasVehicles = useMemo(() => messages.some(message => message.cards.length > 0), [messages]);

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

  /**
   * Exemplele din ghid vin prin store (ghidul e un dialog, nu are acces la ciorna de aici). Abonarea la store, nu un
   * efect pe valoare: preia textul o singură dată și îl golește. Focusul se mută după închiderea dialogului, care îl
   * returnează altfel butonului care l-a deschis.
   */
  useEffect(
    () =>
      useAppStore.subscribe((state, previous) => {
        if (state.pendingPrompt === null || state.pendingPrompt === previous.pendingPrompt) return;
        const prompt = state.pendingPrompt;
        state.setPendingPrompt(null);
        setDraft(prompt);
        window.setTimeout(() => composerWrapperRef.current?.querySelector("textarea")?.focus(), 250);
      }),
    []
  );

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
    <div className="flex min-h-0 flex-1">
      <div className="flex min-h-0 min-w-0 flex-1 flex-col">
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

      <SidePanel comparison={comparison} hasVehicles={hasVehicles} />
    </div>
  );
}
