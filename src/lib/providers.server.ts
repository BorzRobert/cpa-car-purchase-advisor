/**
 * Fabrica server-only pentru modelele LLM.
 *
 * DE CE e separat de registru: registrul ajunge în browser, dar cheile și SDK-urile trebuie să
 * rămână în procesul serverului. Aici este singurul loc care traduce providerul într-un SDK.
 */
import { createAnthropic } from "@ai-sdk/anthropic";
import { createOpenAI } from "@ai-sdk/openai";
import { DEFAULT_MODEL_ID, DEFAULT_PROVIDER_ID, PROVIDERS } from "@/lib/providers";
import type { ModelPricing, ProviderId } from "@/lib/types";

const PROVIDER_ENV_KEYS: Record<ProviderId, string> = {
  anthropic: "ANTHROPIC_API_KEY",
  openai: "OPENAI_API_KEY"
};

export function isProviderConfigured(providerId: ProviderId): boolean {
  return Boolean(process.env[PROVIDER_ENV_KEYS[providerId]]);
}

export function getModel(providerId: ProviderId, modelId: string) {
  const { providerId: resolvedProviderId, modelId: resolvedModelId } = resolveModelSelection(providerId, modelId);

  if (resolvedProviderId === "anthropic") {
    return createAnthropic({ apiKey: process.env.ANTHROPIC_API_KEY })(resolvedModelId);
  }

  return createOpenAI({ apiKey: process.env.OPENAI_API_KEY })(resolvedModelId);
}

export function resolveModelSelection(
  providerId: ProviderId,
  modelId: string
): {
  providerId: ProviderId;
  modelId: string;
  pricing: ModelPricing;
} {
  const provider = PROVIDERS[providerId];
  const model = provider?.models.find(candidate => candidate.id === modelId);
  const fallbackProvider = PROVIDERS[DEFAULT_PROVIDER_ID];
  const fallbackModel = fallbackProvider.models.find(candidate => candidate.id === DEFAULT_MODEL_ID);
  const resolvedModel = model ?? fallbackModel;

  if (!resolvedModel) {
    throw new Error("Registrul nu are un model implicit valid.");
  }

  return {
    providerId: model ? providerId : DEFAULT_PROVIDER_ID,
    modelId: resolvedModel.id,
    pricing: resolvedModel.pricing
  };
}

export function isValidProviderId(value: unknown): value is ProviderId {
  return value === "anthropic" || value === "openai";
}
