/**
 * Descrierea aplicației, trimisă gradual de pe server la ruta `/api/about`.
 *
 * DE CE textul stă aici și nu în componenta client: acesta e primul exemplu în care UI-ul nu
 * decide conținutul răspunsului. La pasul următor, array-ul se înlocuiește cu un LLM, iar
 * protocolul și cititorul din browser rămân aproape identice.
 */

// Streaming-ul funcționează și în Edge; alegem explicit Node.js aici ca primul exemplu să aibă
// runtime-ul declarat, înainte să discutăm când constrângerile unui provider ar cere Edge.
export const runtime = "nodejs";

const ABOUT_CHUNKS = [
  "SkillForge este un copilot personal pentru dezvoltarea abilităților și a carierei. ",
  "Pornește de la profilul tău real - stack, niveluri și obiectiv - ca să nu-ți ofere recomandări generice. ",
  "Te ajută să vezi ce merită învățat mai departe și să transformi obiectivele mari în pași concreți. ",
  "Acest răspuns este demonstrat în streaming: serverul trimite textul treptat, iar interfața îl afișează imediat."
];

const STREAM_DELAY_MS = 120;

export async function GET() {
  const encoder = new TextEncoder();

  const stream = new ReadableStream<Uint8Array>({
    async start(controller) {
      for (const chunk of ABOUT_CHUNKS) {
        // JSON păstrează liniile noi în payload; `\n\n` rămâne exclusiv separatorul dintre evenimente SSE.
        controller.enqueue(encoder.encode(`data: ${JSON.stringify(chunk)}\n\n`));
        // Pauza e intenționat vizibilă, ca mecanismul să poată fi observat fără un LLM sau o cheie API.
        await new Promise(resolve => setTimeout(resolve, STREAM_DELAY_MS));
      }

      controller.enqueue(encoder.encode("data: [DONE]\n\n"));
      controller.close();
    }
  });

  return new Response(stream, {
    headers: {
      "Content-Type": "text/event-stream; charset=utf-8",
      // Fără aceste directive, un proxy poate reține evenimentele și anula efectul de streaming.
      "Cache-Control": "no-cache, no-transform"
    }
  });
}
