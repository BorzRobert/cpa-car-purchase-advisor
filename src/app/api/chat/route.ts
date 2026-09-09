/**
 * Endpointul care izolează cheia Anthropic de browser și transformă răspunsul modelului în SSE.
 * Clientul primește numai evenimente UI; secretul rămâne în procesul Node care face apelul plătit.
 */
import { convertToModelMessages, streamText, type UIMessage } from "ai";
import { DEFAULT_MODEL_ID, DEFAULT_PROVIDER_ID } from "@/lib/providers";
import { getModel, isProviderConfigured, isValidProviderId } from "@/lib/providers.server";
import { buildSystemPrompt, normalizeProfile } from "@/lib/system-prompt";

export const runtime = "nodejs";

function isUIMessageArray(value: unknown): value is UIMessage[] {
  return Array.isArray(value);
}

export async function POST(request: Request) {
  let messages: UIMessage[];
  let profile: ReturnType<typeof normalizeProfile>;
  let providerId = DEFAULT_PROVIDER_ID;
  let modelId = DEFAULT_MODEL_ID;

  try {
    const body: unknown = await request.json();
    if (!body || typeof body !== "object" || !isUIMessageArray((body as { messages?: unknown }).messages)) {
      return Response.json({ error: "Mesajele trimise nu sunt valide." }, { status: 400 });
    }
    const requestBody = body as { messages: UIMessage[]; profile?: unknown; providerId?: unknown; modelId?: unknown };
    messages = requestBody.messages;
    profile = normalizeProfile(requestBody.profile);
    if (isValidProviderId(requestBody.providerId)) providerId = requestBody.providerId;
    if (typeof requestBody.modelId === "string") modelId = requestBody.modelId;
  } catch {
    return Response.json({ error: "Cererea nu conține JSON valid." }, { status: 400 });
  }

  if (!isProviderConfigured(providerId)) {
    return Response.json({ error: "Providerul ales nu are cheia configurată pe server." }, { status: 400 });
  }

  try {
    const result = streamText({
      model: getModel(providerId, modelId),
      system: buildSystemPrompt(profile),
      messages: await convertToModelMessages(messages)
    });

    return result.toUIMessageStreamResponse({
      onError: () => "Modelul nu poate răspunde acum. Verifică cheia, modelul și limita de utilizare."
    });
  } catch {
    // Erorile de configurare și de conversie nu trebuie să dezvăluie detalii despre cont sau provider.
    return Response.json(
      { error: "Providerul ales nu poate porni răspunsul. Verifică cheia, modelul și limita de utilizare." },
      { status: 400 }
    );
  }
}
