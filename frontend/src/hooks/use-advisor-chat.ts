"use client";

/**
 * Hook-ul conversației: istoricul din backend plus răspunsul care curge acum prin SSE.
 *
 * DE CE două surse: cât timp răspunsul curge, hook-ul e sursa de adevăr (mesajul se construiește eveniment cu
 * eveniment); după ce fluxul s-a încheiat, istoricul se reia din backend, care este arhiva persistentă, iar mesajul
 * „în curs” dispare. Așa nu avem în browser o a doua copie a conversației care să poată diverge de cea din baza de date.
 *
 * DE CE `send` întoarce „rejected” înainte de orice flux: cererile respinse (limită, buget, consilier neactivat) nu
 * salvează mesajul, deci apelantul poate pune textul înapoi în câmpul de scris.
 *
 * DE CE un flux care nu se încheie cu `done` sau `error` e „incomplet”: conexiunea s-a rupt (rețea, proxy, backend
 * oprit); răspunsul nu trebuie prezentat ca și cum ar fi complet.
 */
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { parseAdvisorEvent } from "@/lib/advisor-events";
import { ApiError, INCOMPLETE_STREAM, problemToError } from "@/lib/api/errors";
import { api, openMessageStream } from "@/lib/api/endpoints";
import { applyAdvisorEvent, newMessage, type ChatMessage } from "@/lib/message-utils";
import { readSse } from "@/lib/sse";
import { conversationKeys, useMessages } from "@/hooks/use-conversations";
import { profileKey } from "@/hooks/use-profile";
import { shortlistKey } from "@/hooks/use-shortlist";
import { useAppStore } from "@/store/useAppStore";

export type SendResult = "sent" | "rejected";

type Live = { conversationId: string; user: ChatMessage; assistant: ChatMessage };
/** Ce a arătat un răspuns în afară de text; backend-ul păstrează doar textul, deci acestea trăiesc cât timp stă pagina deschisă. */
type Extras = Pick<ChatMessage, "tools" | "cards" | "comparison">;
type ScopedError = { conversationId: string | null; error: ApiError };

function isAbort(error: unknown): boolean {
  return error instanceof DOMException && error.name === "AbortError";
}

export function useAdvisorChat(conversationId: string | null) {
  const queryClient = useQueryClient();
  const history = useMessages(conversationId);
  const [live, setLive] = useState<Live | null>(null);
  const [scopedError, setScopedError] = useState<ScopedError | null>(null);
  const [streaming, setStreaming] = useState(false);
  const [extras, setExtras] = useState<Record<string, Extras>>({});
  const controller = useRef<AbortController | null>(null);

  // O eroare aparține conversației în care a apărut; la schimbarea conversației nu rămâne agățată de alta.
  const error = scopedError && scopedError.conversationId === conversationId ? scopedError.error : null;

  const messages = useMemo(() => {
    const saved = (history.data ?? []).map(message =>
      extras[message.id] ? { ...message, ...extras[message.id] } : message
    );
    if (!live || live.conversationId !== conversationId) return saved;
    // Răspunsul a ajuns în istoric (reîncărcarea de după `done`): mesajul „în curs” nu mai trebuie arătat.
    if (saved.some(message => message.id === live.assistant.id)) return saved;
    // Backend-ul salvează mesajul utilizatorului înainte de flux; o reîncărcare care îl prinde nu trebuie să-l dubleze.
    const last = saved.at(-1);
    const base = last?.role === "user" && last.content === live.user.content ? saved.slice(0, -1) : saved;
    return [...base, live.user, live.assistant];
  }, [conversationId, extras, history.data, live]);

  useEffect(() => () => controller.current?.abort(), []);

  const send = useCallback(
    async (text: string): Promise<SendResult> => {
      if (controller.current) return "rejected";
      const abort = new AbortController();
      controller.current = abort;
      setScopedError(null);
      setStreaming(true);

      const store = useAppStore.getState();
      let id = conversationId;
      let created = false;
      let accepted = false;
      let finished = false;
      let assistant = newMessage(`pending-assistant-${crypto.randomUUID()}`, "assistant");

      try {
        if (id === null) {
          id = (await api.conversations.create()).id ?? null;
          if (id === null) throw new ApiError(0, INCOMPLETE_STREAM);
          created = true;
          store.setActiveConversation(id);
        }
        setLive({
          conversationId: id,
          user: newMessage(`pending-user-${crypto.randomUUID()}`, "user", text),
          assistant
        });

        const tokenBefore = useAppStore.getState().sessionToken;
        let response: Response;
        try {
          response = await openMessageStream(id, text, abort.signal);
        } catch (failure) {
          // Sesiunea a fost înlocuită între timp (401): conversația veche aparținea celei vechi, iar „nu există” ar deruta.
          if (useAppStore.getState().sessionToken !== tokenBefore) throw new ApiError(401, "unauthorized");
          throw failure;
        }
        if (!response.body) throw new ApiError(0, INCOMPLETE_STREAM);
        accepted = true;

        for await (const frame of readSse(response.body)) {
          const event = parseAdvisorEvent(frame);
          if (!event) continue;
          if (event.type === "done") finished = true;
          if (event.type === "error") {
            finished = true;
            setScopedError({ conversationId: id, error: problemToError(event.problem, 502) });
          }
          assistant = applyAdvisorEvent(assistant, event);
          const snapshot = assistant;
          setLive(current => (current ? { ...current, assistant: snapshot } : current));
        }
        if (!finished) setScopedError({ conversationId: id, error: new ApiError(0, INCOMPLETE_STREAM) });
        return "sent";
      } catch (failure) {
        if (isAbort(failure)) return accepted ? "sent" : "rejected";
        const apiError = failure instanceof ApiError ? failure : new ApiError(0, INCOMPLETE_STREAM);
        if (accepted) {
          setScopedError({ conversationId: id, error: apiError });
          return "sent";
        }
        // Conversația creată doar pentru acest mesaj nu are rost să rămână goală în listă.
        if (created && id) {
          store.setActiveConversation(null);
          await api.conversations.remove(id).catch(() => undefined);
        }
        setScopedError({ conversationId: created ? null : id, error: apiError });
        return "rejected";
      } finally {
        controller.current = null;
        // Arhiva (backend-ul) devine sursa de adevăr; profilul și lista scurtă pot fi schimbate de uneltele agentului.
        const refresh = [
          conversationKeys.list,
          profileKey,
          shortlistKey,
          ...(id ? [conversationKeys.messages(id)] : [])
        ];
        await Promise.all(refresh.map(queryKey => queryClient.invalidateQueries({ queryKey })));
        if (accepted && (assistant.tools.length > 0 || assistant.cards.length > 0 || assistant.comparison)) {
          const { tools, cards, comparison } = assistant;
          setExtras(current => ({ ...current, [assistant.id]: { tools, cards, comparison } }));
        }
        setLive(null);
        setStreaming(false);
      }
    },
    [conversationId, queryClient]
  );

  const stop = useCallback(() => controller.current?.abort(), []);
  const dismissError = useCallback(() => setScopedError(null), []);

  return {
    messages,
    streaming,
    error,
    historyError: history.error,
    loading: conversationId !== null && history.isPending && !live,
    send,
    stop,
    dismissError
  };
}
