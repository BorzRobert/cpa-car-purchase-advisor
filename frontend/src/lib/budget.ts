/**
 * Intervalele cursorului de buget, pentru bugetul total și pentru cel lunar.
 *
 * DE CE intervale separate: 800 € ca buget total și 800 € pe lună sunt ordine de mărime diferite; același cursor ar fi
 * inutilizabil pe unul dintre ele. La comutare, suma se aduce în intervalul noului tip (nu se convertește: nu știm
 * durata finanțării), ca valoarea afișată să nu iasă niciodată din cursor.
 */
import type { BuyerProfile } from "@/lib/api/endpoints";

export type BudgetKind = NonNullable<BuyerProfile["budgetKind"]>;

export type BudgetRange = { min: number; max: number; step: number };

export const BUDGET_RANGES: Record<BudgetKind, BudgetRange> = {
  TOTAL: { min: 2_000, max: 80_000, step: 500 },
  MONTHLY: { min: 100, max: 1_500, step: 25 }
};

export function clampBudget(amount: number, kind: BudgetKind): number {
  const { min, max, step } = BUDGET_RANGES[kind];
  const snapped = Math.round(amount / step) * step;
  return Math.min(max, Math.max(min, snapped));
}
