/**
 * Endpointul care izolează cheia Anthropic de browser și transformă răspunsul modelului în SSE.
 * Clientul primește numai evenimente UI; secretul rămâne în procesul Node care face apelul plătit.
 */
import { createUIMessageStream, createUIMessageStreamResponse, convertToModelMessages, streamText } from "ai";
import { createHash, randomUUID } from "node:crypto";
import type { UIMessage } from "ai";
import { authConfigured, getServerSession } from "@/lib/auth";
import { cache } from "@/lib/cache";
import { calculateCost } from "@/lib/cost";
import { DEFAULT_MODEL_ID, DEFAULT_PROVIDER_ID } from "@/lib/providers";
import { getModel, isProviderConfigured, isValidProviderId, resolveModelSelection } from "@/lib/providers.server";
import { buildSystemPrompt, normalizeProfile } from "@/lib/system-prompt";
import type { AppUIMessage, MessageMetadata } from "@/lib/types";

export const runtime = "nodejs";

function isUIMessageArray(value: unknown): value is UIMessage[] {
  return Array.isArray(value);
}

type CachedResponse = {
  text: string;
  metadata: MessageMetadata;
};

const RATE_LIMIT = 10;
const RATE_WINDOW_MS = 60_000;
// Protecție de bun-simț pentru o instanță; nu este măsură de securitate, fiindcă instanțele nu împart memoria.
const requestWindows = new Map<string, number[]>();

function getClientId(request: Request): string {
  return request.headers.get("x-forwarded-for")?.split(",")[0]?.trim() || "local-client";
}

function getRateLimitRetryAt(clientId: string): number | undefined {
  const now = Date.now();
  const timestamps = (requestWindows.get(clientId) ?? []).filter(timestamp => timestamp > now - RATE_WINDOW_MS);
  if (timestamps.length >= RATE_LIMIT) {
    requestWindows.set(clientId, timestamps);
    return timestamps[0] + RATE_WINDOW_MS;
  }
  timestamps.push(now);
  requestWindows.set(clientId, timestamps);
  return undefined;
}

function getCacheKey(providerId: string, modelId: string, systemPrompt: string, messages: UIMessage[]): string {
  const messagesHash = createHash("sha256").update(JSON.stringify(messages)).digest("hex");
  return createHash("sha256").update(`${providerId}:${modelId}:${systemPrompt}:${messagesHash}`).digest("hex");
}

function createCachedResponse(response: CachedResponse): Response {
  const messageId = randomUUID();
  const stream = createUIMessageStream<AppUIMessage>({
    execute: async ({ writer }) => {
      writer.write({ type: "start", messageId });
      writer.write({ type: "text-start", id: messageId });

      for (const chunk of response.text.match(/[\s\S]{1,80}/g) ?? [response.text]) {
        writer.write({ type: "text-delta", id: messageId, delta: chunk });
        await new Promise(resolve => setTimeout(resolve, 8));
      }

      writer.write({ type: "text-end", id: messageId });
      writer.write({ type: "finish", finishReason: "stop", messageMetadata: response.metadata });
    }
  });

  return createUIMessageStreamResponse({ stream });
}

export async function POST(request: Request) {
  // În producție, lipsa configurației închide ruta plătită; altfel deploy-ul ar deveni public fără să observe nimeni.
  if (authConfigured || process.env.NODE_ENV === "production") {
    const session = await getServerSession();
    if (!session?.user?.id)
      return Response.json({ error: "Trebuie să fii autentificat ca să întrebi agentul." }, { status: 401 });
    console.info(`[chat] cerere autentificată pentru utilizatorul ${session.user.id}`);
  }

  let messages: UIMessage[];
  let profile: ReturnType<typeof normalizeProfile>;
  let providerId = DEFAULT_PROVIDER_ID;
  let modelId = DEFAULT_MODEL_ID;

  try {
    const body: unknown = await request.json();
    if (!body || typeof body !== "object" || !isUIMessageArray((body as { messages?: unknown }).messages)) {
      return Response.json({ error: "Mesajele trimise nu sunt valide." }, { status: 400 });
    }
    const requestBody = body as {
      messages: UIMessage[];
      profile?: unknown;
      providerId?: unknown;
      modelId?: unknown;
      forceRefresh?: unknown;
    };
    messages = requestBody.messages;
    profile = normalizeProfile(requestBody.profile);
    if (isValidProviderId(requestBody.providerId)) providerId = requestBody.providerId;
    if (typeof requestBody.modelId === "string") modelId = requestBody.modelId;

    const retryAt = getRateLimitRetryAt(getClientId(request));
    if (retryAt) {
      return Response.json(
        {
          error: `Ai atins limita de ${RATE_LIMIT} cereri pe minut. Poți reîncerca după ${new Date(retryAt).toLocaleTimeString("ro-RO")}.`
        },
        { status: 429, headers: { "Retry-After": String(Math.ceil((retryAt - Date.now()) / 1000)) } }
      );
    }

    const systemPrompt = buildSystemPrompt(profile);
    const selection = resolveModelSelection(providerId, modelId);
    const cacheKey = getCacheKey(selection.providerId, selection.modelId, systemPrompt, messages);
    if (requestBody.forceRefresh !== true) {
      const cached = cache.get<CachedResponse>(cacheKey);
      if (cached) return createCachedResponse(cached);
    }

    if (!isProviderConfigured(selection.providerId)) {
      return Response.json({ error: "Providerul ales nu are cheia configurată pe server." }, { status: 400 });
    }

    const result = streamText({
      model: getModel(selection.providerId, selection.modelId),
      system: systemPrompt,
      messages: await convertToModelMessages(messages),
      onFinish: ({ text, usage }) => {
        const normalizedUsage = {
          inputTokens: usage.inputTokens,
          outputTokens: usage.outputTokens
        };
        cache.set<CachedResponse>(cacheKey, {
          text,
          metadata: {
            providerId: selection.providerId,
            modelId: selection.modelId,
            usage: normalizedUsage,
            cost: calculateCost(normalizedUsage, selection.pricing),
            cached: false
          }
        });
      }
    });

    return result.toUIMessageStreamResponse<AppUIMessage>({
      originalMessages: messages as AppUIMessage[],
      messageMetadata: ({ part }) => {
        if (part.type !== "finish") return undefined;
        const usage = {
          inputTokens: part.totalUsage.inputTokens,
          outputTokens: part.totalUsage.outputTokens
        };
        return {
          providerId: selection.providerId,
          modelId: selection.modelId,
          usage,
          cost: calculateCost(usage, selection.pricing),
          cached: false
        } satisfies MessageMetadata;
      },
      onError: () => "Modelul nu poate răspunde acum. Verifică cheia, modelul și limita de utilizare."
    });
  } catch {
    return Response.json({ error: "Cererea nu conține JSON valid." }, { status: 400 });
  }
}
