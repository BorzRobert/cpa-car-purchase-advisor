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
import { toast } from "sonner";
import { Alert, AlertAction, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { ChatInput } from "@/components/chat/chat-input";
import { EmptyState } from "@/components/chat/empty-state";
import { MessageList } from "@/components/chat/message-list";
import { AppHeader } from "@/components/layout/app-header";
import { serializeConversationJson, serializeConversationMarkdown } from "@/lib/message-utils";
import { useAppStore, useStoreHydration } from "@/store/useAppStore";
import type { AppUIMessage, ProviderAvailability, ProviderId } from "@/lib/types";

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
    if (response && typeof response === "object" && "error" in response) {
      const message = (response as { error?: unknown }).error;
      if (typeof message === "string") return message;
    }
  } catch {
    // Un stream întrerupt nu trimite neapărat JSON; detaliile lui nu sunt pentru utilizator.
  }

  return "Modelul nu poate răspunde acum. Verifică cheia, modelul și limita de utilizare.";
}

export function Chat() {
  const hydrated = useStoreHydration();
  const activeConversationId = useAppStore(state => state.activeConversationId);
  const conversations = useAppStore(state => state.conversations);
  const createConversation = useAppStore(state => state.createConversation);
  const updateConversationMessages = useAppStore(state => state.updateConversationMessages);
  const [pendingMessage, setPendingMessage] = useState<string | null>(null);
  const activeConversation = conversations.find(conversation => conversation.id === activeConversationId);

  if (!hydrated) {
    return <ChatSession initialMessages={[]} activeConversationId={null} pendingMessage={null} hydrated={false} />;
  }

  return (
    <ChatSession
      key={activeConversationId ?? "new"}
      initialMessages={activeConversation?.messages ?? []}
      activeConversationId={activeConversationId}
      pendingMessage={pendingMessage}
      hydrated={hydrated}
      onCreateConversation={message => {
        setPendingMessage(message);
        createConversation(message);
      }}
      onPendingMessageSent={() => setPendingMessage(null)}
      onMessagesFinished={(id, messages) => updateConversationMessages(id, messages)}
    />
  );
}

type ChatSessionProps = {
  initialMessages: AppUIMessage[];
  activeConversationId: string | null;
  pendingMessage: string | null;
  hydrated: boolean;
  onCreateConversation?: (message: string) => void;
  onPendingMessageSent?: () => void;
  onMessagesFinished?: (id: string, messages: AppUIMessage[]) => void;
};

function ChatSession({
  initialMessages,
  activeConversationId,
  pendingMessage,
  hydrated,
  onCreateConversation,
  onPendingMessageSent,
  onMessagesFinished
}: ChatSessionProps) {
  const [draft, setDraft] = useState("");
  const [providerAvailability, setProviderAvailability] = useState<Record<ProviderId, ProviderAvailability> | null>(
    null
  );
  const composerWrapperRef = useRef<HTMLDivElement>(null);
  const pendingMessageSentRef = useRef(false);
  const [forceRefresh, setForceRefresh] = useState(false);
  const profile = useAppStore(state => state.profile);
  const providerId = useAppStore(state => state.providerId);
  const modelId = useAppStore(state => state.modelId);

  const { messages, status, sendMessage, stop, regenerate, error, setMessages, clearError } = useChat<AppUIMessage>({
    messages: initialMessages,
    transport: new DefaultChatTransport({
      api: "/api/chat",
      prepareSendMessagesRequest: ({ messages, body }) => {
        setForceRefresh(false);
        return {
          body: {
            ...body,
            messages,
            profile,
            providerId,
            modelId,
            forceRefresh
          }
        };
      }
    })
  });

  const previousStatusRef = useRef(status);

  const initials = getInitials(profile.name);

  useEffect(() => {
    void fetch("/api/providers")
      .then(response => response.json())
      .then(data => setProviderAvailability(data as Record<ProviderId, ProviderAvailability>))
      .catch(() => setProviderAvailability(null));
  }, []);

  useEffect(() => {
    // Hook-ul e sursa de adevăr în timpul fluxului; arhiva primește o singură copie când fluxul s-a încheiat.
    const wasStreaming = previousStatusRef.current === "submitted" || previousStatusRef.current === "streaming";
    if (activeConversationId && wasStreaming && status === "ready") {
      onMessagesFinished?.(activeConversationId, messages);
    }
    previousStatusRef.current = status;
  }, [activeConversationId, messages, onMessagesFinished, status]);

  useEffect(() => {
    if (!pendingMessage || !activeConversationId || pendingMessageSentRef.current) return;
    // Efectele pot fi reluate în Strict Mode; aceeași întrebare trebuie să pornească un singur stream.
    pendingMessageSentRef.current = true;
    sendMessage({ text: pendingMessage });
    onPendingMessageSent?.();
  }, [activeConversationId, onPendingMessageSent, pendingMessage, sendMessage]);

  function handleSubmit() {
    if (!draft.trim()) return;
    const text = draft.trim();
    if (activeConversationId === null) {
      onCreateConversation?.(text);
    } else {
      sendMessage({ text });
    }
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

  function handleRegenerate(messageId: string) {
    const messageIndex = messages.findIndex(message => message.id === messageId);
    if (messageIndex < 1 || messages[messageIndex]?.role !== "assistant") return;

    // Tăiem răspunsul înainte de retrimitere ca `regenerate` să înlocuiască bula, nu să adauge una nouă.
    setMessages(messages.slice(0, messageIndex));
    clearError();
    setForceRefresh(true);
    toast.info("Generez din nou răspunsul");
    void regenerate();
  }

  function handleEdit(messageId: string, text: string) {
    if (status !== "ready") return;
    const messageIndex = messages.findIndex(message => message.id === messageId);
    if (messageIndex < 0 || messages[messageIndex]?.role !== "user") return;

    // Tăiem și răspunsul vechi: altfel modelul ar primi două continuări contradictorii ale aceleiași întrebări.
    setMessages(messages.slice(0, messageIndex));
    clearError();
    sendMessage({ text });
  }

  function handleClear() {
    if (!window.confirm("Golești mesajele acestei conversații? Acțiunea nu poate fi anulată.")) return;
    setMessages([]);
    clearError();
    if (activeConversationId) onMessagesFinished?.(activeConversationId, []);
    toast.success("Conversația a fost golită");
  }

  function handleExport(format: "json" | "markdown") {
    const exportedAt = new Date().toISOString();
    const isMarkdown = format === "markdown";
    const content = isMarkdown
      ? serializeConversationMarkdown(messages, profile, exportedAt)
      : serializeConversationJson(messages, profile, exportedAt);
    const blob = new Blob([content], {
      type: isMarkdown ? "text/markdown;charset=utf-8" : "application/json;charset=utf-8"
    });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `skillforge-${exportedAt.slice(0, 10)}.${isMarkdown ? "md" : "json"}`;
    link.click();
    URL.revokeObjectURL(url);
    toast.success(`Export ${isMarkdown ? "Markdown" : "JSON"} descărcat`);
  }

  const composer = (
    <div ref={composerWrapperRef}>
      <ChatInput
        value={draft}
        onValueChange={setDraft}
        onSubmit={handleSubmit}
        status={status}
        onStop={() => void stop()}
        providerId={providerId}
        modelId={modelId}
        providerAvailability={providerAvailability}
      />
    </div>
  );

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <AppHeader onExport={handleExport} onClear={handleClear} canClear={messages.length > 0} />
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
          <MessageList
            messages={messages}
            status={status}
            userInitials={initials}
            isLoading={!hydrated}
            onRegenerate={handleRegenerate}
            onEdit={handleEdit}
          />
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
