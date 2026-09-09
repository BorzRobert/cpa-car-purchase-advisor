/**
 * Endpointul care izolează cheia Anthropic de browser și transformă răspunsul modelului în SSE.
 * Clientul primește numai evenimente UI; secretul rămâne în procesul Node care face apelul plătit.
 */
import { createAnthropic } from "@ai-sdk/anthropic";
import { convertToModelMessages, streamText, type UIMessage } from "ai";
import { DEFAULT_MODEL_ID, DEFAULT_PROVIDER_ID } from "@/lib/providers";
import { buildSystemPrompt, normalizeProfile } from "@/lib/system-prompt";

export const runtime = "nodejs";

function isUIMessageArray(value: unknown): value is UIMessage[] {
  return Array.isArray(value);
}

export async function POST(request: Request) {
  const apiKey = process.env.ANTHROPIC_API_KEY;

  if (!apiKey) {
    return Response.json({ error: "Cheia nu e configurată." }, { status: 400 });
  }

  let messages: UIMessage[];
  let profile: ReturnType<typeof normalizeProfile>;

  try {
    const body: unknown = await request.json();
    if (!body || typeof body !== "object" || !isUIMessageArray((body as { messages?: unknown }).messages)) {
      return Response.json({ error: "Mesajele trimise nu sunt valide." }, { status: 400 });
    }
    const requestBody = body as { messages: UIMessage[]; profile?: unknown };
    messages = requestBody.messages;
    profile = normalizeProfile(requestBody.profile);
  } catch {
    return Response.json({ error: "Cererea nu conține JSON valid." }, { status: 400 });
  }

  try {
    const anthropic = createAnthropic({ apiKey });
    const result = streamText({
      model: anthropic(DEFAULT_MODEL_ID),
      system: buildSystemPrompt(profile),
      messages: await convertToModelMessages(messages)
    });

    return result.toUIMessageStreamResponse({
      onError: () => "Modelul nu poate răspunde acum. Verifică cheia, modelul și limita de utilizare."
    });
  } catch {
    // Erorile de configurare și de conversie nu trebuie să dezvăluie detalii despre cont sau provider.
    return Response.json(
      { error: `Providerul implicit (${DEFAULT_PROVIDER_ID}) nu poate porni răspunsul.` },
      { status: 400 }
    );
  }
}
