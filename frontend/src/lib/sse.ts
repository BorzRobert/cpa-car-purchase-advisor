/**
 * Parser pentru Server-Sent Events peste `fetch` + `ReadableStream`.
 *
 * DE CE nu `EventSource`: nu poate trimite POST și nici antetul `Authorization`, iar răspunsul la mesaj e un POST.
 *
 * DE CE parserul e o clasă cu stare separată de citirea fluxului: octeții sosesc în bucăți care se pot opri oriunde
 * (în mijlocul unui rând, între `\r` și `\n`, în mijlocul unui caracter UTF-8), așa că logica de încadrare trebuie să
 * se poată testa fără rețea.
 *
 * Reguli din specificația SSE respectate: rândurile care încep cu `:` sunt comentarii (heartbeat-ul serverului
 * `: keep-alive`) și se ignoră; mai multe rânduri `data:` ale aceluiași eveniment se unesc cu `\n`; un eveniment
 * neterminat la închiderea fluxului se aruncă — apelantul îl tratează ca flux incomplet.
 */

export type SseFrame = { event: string; data: string };

export class SseParser {
  private buffer = "";
  private event = "";
  private data: string[] = [];

  /** Primește o bucată de text și întoarce evenimentele care s-au încheiat în ea. */
  push(chunk: string): SseFrame[] {
    this.buffer += chunk;
    const frames: SseFrame[] = [];
    let start = 0;

    for (let i = 0; i < this.buffer.length; i++) {
      const char = this.buffer[i];
      if (char !== "\n" && char !== "\r") continue;
      // `\r` la capătul bufferului: nu știm încă dacă urmează `\n`; așteptăm următoarea bucată.
      if (char === "\r" && i + 1 === this.buffer.length) break;

      const line = this.buffer.slice(start, i);
      if (char === "\r" && this.buffer[i + 1] === "\n") i++;
      start = i + 1;
      this.onLine(line, frames);
    }

    this.buffer = this.buffer.slice(start);
    return frames;
  }

  private onLine(line: string, frames: SseFrame[]) {
    if (line === "") {
      if (this.data.length > 0) frames.push({ event: this.event || "message", data: this.data.join("\n") });
      this.event = "";
      this.data = [];
      return;
    }
    if (line.startsWith(":")) return;

    const colon = line.indexOf(":");
    const field = colon === -1 ? line : line.slice(0, colon);
    let value = colon === -1 ? "" : line.slice(colon + 1);
    if (value.startsWith(" ")) value = value.slice(1);

    if (field === "event") this.event = value;
    else if (field === "data") this.data.push(value);
  }
}

/** Citește corpul răspunsului și produce evenimentele pe măsură ce sosesc; o oprire prin `AbortController` aruncă `AbortError`. */
export async function* readSse(body: ReadableStream<Uint8Array>): AsyncGenerator<SseFrame> {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  const parser = new SseParser();
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) return;
      yield* parser.push(decoder.decode(value, { stream: true }));
    }
  } finally {
    // Dacă apelantul se oprește devreme (return/throw), închidem conexiunea în loc să o lăsăm deschisă.
    await reader.cancel().catch(() => undefined);
  }
}
