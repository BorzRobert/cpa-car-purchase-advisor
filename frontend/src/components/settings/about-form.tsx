"use client";

/**
 * Preferințe → Despre aplicație: citește manual fluxul SSE de la server.
 *
 * DE CE fără SDK: înainte de primul model de limbaj vedem protocolul care stă sub abstracții
 * precum `useChat`. Când serverul va trimite tokeni reali, cititorul de mai jos rămâne valid.
 */

import { useEffect, useState } from "react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";

type AboutStatus = "streaming" | "done" | "error";

export function AboutForm() {
  const [text, setText] = useState("");
  const [status, setStatus] = useState<AboutStatus>("streaming");
  const [runId, setRunId] = useState(0);

  useEffect(() => {
    const abortController = new AbortController();

    async function readStream() {
      try {
        const response = await fetch("/api/about", { signal: abortController.signal });
        if (!response.ok || !response.body) throw new Error("Răspunsul de pe server nu poate fi citit.");

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";
        let finished = false;

        while (!finished) {
          const { done, value } = await reader.read();
          // O diacritică UTF-8 poate fi tăiată între pachete; `stream: true` reține octeții incompleți.
          buffer += done ? decoder.decode() : decoder.decode(value, { stream: true });

          // Un pachet de rețea nu este neapărat un eveniment SSE: păstrăm ultimul fragment pentru citirea următoare.
          const events = buffer.split("\n\n");
          buffer = events.pop() ?? "";

          for (const event of events) {
            if (!event.startsWith("data: ")) continue;

            const payload = event.slice("data: ".length);
            if (payload === "[DONE]") {
              finished = true;
              break;
            }

            setText(current => current + JSON.parse(payload));
          }

          if (done) break;
        }

        if (!abortController.signal.aborted) setStatus("done");
      } catch {
        // Închiderea dialogului anulează normal cererea; numai o problemă reală ajunge în UI.
        if (!abortController.signal.aborted) {
          setStatus("error");
        }
      }
    }

    void readStream();
    // Nu mai scriem în starea unei secțiuni demontate când dialogul se închide înainte de finalul fluxului.
    return () => abortController.abort();
  }, [runId]);

  function handleRetry() {
    setText("");
    setStatus("streaming");
    setRunId(current => current + 1);
  }

  return (
    <section className="flex min-h-0 flex-1 flex-col" aria-live="polite">
      <div className="min-h-0 flex-1 overflow-y-auto rounded-md border bg-muted/30 p-4 text-sm leading-6 whitespace-pre-wrap">
        {text}
        {status === "streaming" && (
          <span className="animate-pulse" aria-label="Răspunsul se scrie">
            |
          </span>
        )}
      </div>

      {status === "error" && (
        <Alert variant="destructive" className="mt-4">
          <AlertTitle>Descrierea nu a putut fi încărcată</AlertTitle>
          <AlertDescription>Serverul nu a trimis răspunsul așteptat. Încearcă din nou.</AlertDescription>
        </Alert>
      )}

      <footer className="mt-auto flex items-center justify-between gap-3 pt-4">
        <p className="text-xs text-muted-foreground">Sursă: serverul SkillForge, printr-un flux SSE.</p>
        <Button onClick={handleRetry} disabled={status === "streaming"}>
          Reia
        </Button>
      </footer>
    </section>
  );
}
