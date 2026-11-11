/**
 * Conversia dintre profilul din backend și ciorna formularului de nevoi, ca funcții pure.
 *
 * DE CE separat de componentă: câmpurile numerice se tastează ca text (un câmp gol e „nespecificat”, nu 0), iar limitele
 * din OpenAPI (cel mult 10 elemente de câte 80 de caractere, procente 0–100 etc.) trebuie verificate înainte de trimitere,
 * ca utilizatorul să primească un mesaj clar în loc de un `400` generic. Backend-ul rămâne totuși autoritatea: validează
 * și curăță din nou.
 */
import type { BodyType, Powertrain } from "@/lib/advisor-events";
import type { BuyerProfile, ProfileRequest } from "@/lib/api/endpoints";

type Single<T> = T[];

export type ProfileDraft = {
  budgetAmount: string;
  budgetKind: Single<NonNullable<BuyerProfile["budgetKind"]>>;
  financingMode: Single<NonNullable<BuyerProfile["financingMode"]>>;
  yearlyKm: string;
  cityPercent: string;
  passengers: string;
  bodyTypes: BodyType[];
  powertrains: Powertrain[];
  condition: Single<NonNullable<BuyerProfile["condition"]>>;
  homeCharging: Single<"yes" | "no">;
  mustHaves: string;
  dealBreakers: string;
};

export const MAX_LIST_ITEMS = 10;
export const MAX_ITEM_LENGTH = 80;

const text = (value: number | undefined) => (value === undefined || value === null ? "" : String(value));
const single = <T>(value: T | undefined | null): T[] => (value ? [value] : []);

export function toDraft(profile: BuyerProfile): ProfileDraft {
  return {
    budgetAmount: text(profile.budgetAmount),
    budgetKind: single(profile.budgetKind),
    financingMode: single(profile.financingMode),
    yearlyKm: text(profile.yearlyKm),
    cityPercent: text(profile.cityPercent),
    passengers: text(profile.passengers),
    bodyTypes: profile.bodyTypes ?? [],
    powertrains: profile.powertrains ?? [],
    condition: single(profile.condition),
    homeCharging:
      profile.homeCharging === undefined || profile.homeCharging === null ? [] : [profile.homeCharging ? "yes" : "no"],
    mustHaves: (profile.mustHaves ?? []).join("\n"),
    dealBreakers: (profile.dealBreakers ?? []).join("\n")
  };
}

function parseNumber(value: string, label: string, min: number, max: number, errors: string[]): number | undefined {
  const trimmed = value.trim();
  if (!trimmed) return undefined;
  const parsed = Number(trimmed.replace(",", "."));
  if (!Number.isFinite(parsed) || parsed < min || parsed > max) {
    errors.push(`${label}: introdu un număr între ${min} și ${max}.`);
    return undefined;
  }
  return parsed;
}

function parseLines(value: string, label: string, errors: string[]): string[] {
  const lines = value
    .split("\n")
    .map(line => line.trim())
    .filter(Boolean);
  if (lines.length > MAX_LIST_ITEMS) errors.push(`${label}: cel mult ${MAX_LIST_ITEMS} rânduri.`);
  if (lines.some(line => line.length > MAX_ITEM_LENGTH)) {
    errors.push(`${label}: fiecare rând poate avea cel mult ${MAX_ITEM_LENGTH} de caractere.`);
  }
  return lines;
}

/** Ciorna → cererea `PUT /profile`. `PUT` înlocuiește tot profilul, deci ce lipsește aici se șterge în backend. */
export function toRequest(draft: ProfileDraft): { request: ProfileRequest; errors: string[] } {
  const errors: string[] = [];
  const budgetAmount = parseNumber(draft.budgetAmount, "Buget", 0, 10_000_000, errors);
  const budgetKind = draft.budgetKind[0];
  if (budgetAmount !== undefined && !budgetKind) errors.push("Buget: alege dacă suma este totală sau pe lună.");

  const request: ProfileRequest = {
    budgetAmount,
    budgetKind: budgetAmount === undefined ? undefined : budgetKind,
    financingMode: draft.financingMode[0],
    yearlyKm: parseNumber(draft.yearlyKm, "Kilometri pe an", 0, 200_000, errors),
    cityPercent: parseNumber(draft.cityPercent, "Condus în oraș", 0, 100, errors),
    passengers: parseNumber(draft.passengers, "Pasageri", 1, 9, errors),
    bodyTypes: draft.bodyTypes,
    powertrains: draft.powertrains,
    condition: draft.condition[0],
    homeCharging: draft.homeCharging.length === 0 ? undefined : draft.homeCharging[0] === "yes",
    mustHaves: parseLines(draft.mustHaves, "Obligatoriu", errors),
    dealBreakers: parseLines(draft.dealBreakers, "Eliminatoriu", errors)
  };
  return { request, errors };
}
