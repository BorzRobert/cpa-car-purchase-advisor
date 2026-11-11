/**
 * Calculează costurile într-un singur loc, ca mesajul și totalul conversației să rămână
 * identice când se schimbă tarifele sau forma usage-ului raportat de provider.
 */
import type { ModelPricing } from "@/lib/types";

export type TokenUsage = {
  inputTokens: number | undefined;
  outputTokens: number | undefined;
};

export type CostBreakdown = {
  input: number | undefined;
  output: number | undefined;
  total: number | undefined;
};

function tokenCost(tokens: number | undefined, pricePerMillion: number): number | undefined {
  return tokens === undefined ? undefined : (tokens * pricePerMillion) / 1_000_000;
}

export function calculateCost(usage: TokenUsage, pricing: ModelPricing | undefined): CostBreakdown {
  if (!pricing) return { input: undefined, output: undefined, total: undefined };

  const input = tokenCost(usage.inputTokens, pricing.inputPerMillion);
  const output = tokenCost(usage.outputTokens, pricing.outputPerMillion);

  return {
    input,
    output,
    total: input === undefined || output === undefined ? undefined : input + output
  };
}

export function addCosts(costs: CostBreakdown[]): CostBreakdown {
  const input = costs.every(cost => cost.input !== undefined)
    ? costs.reduce((total, cost) => total + (cost.input ?? 0), 0)
    : undefined;
  const output = costs.every(cost => cost.output !== undefined)
    ? costs.reduce((total, cost) => total + (cost.output ?? 0), 0)
    : undefined;

  return {
    input,
    output,
    total: input === undefined || output === undefined ? undefined : input + output
  };
}
