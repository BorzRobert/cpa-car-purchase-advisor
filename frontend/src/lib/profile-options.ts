/**
 * Etichetele în română ale valorilor enum din API, ca date.
 *
 * DE CE registru: formularul de nevoi, cardurile de vehicule și lista scurtă arată aceleași enum-uri; o etichetă
 * schimbată aici se schimbă peste tot, iar o valoare nouă din backend e o linie de date, nu un `if` nou.
 * Valorile vin din tipurile generate din OpenAPI, deci o valoare uitată aici nu compilează.
 */
import type { BodyType, Powertrain } from "@/lib/advisor-events";
import type { BuyerProfile } from "@/lib/api/endpoints";

export type Option<T extends string> = { value: T; label: string };

export const BODY_TYPE_OPTIONS: Option<BodyType>[] = [
  { value: "HATCHBACK", label: "Hatchback" },
  { value: "SEDAN", label: "Berlină" },
  { value: "ESTATE", label: "Break" },
  { value: "SUV", label: "SUV" },
  { value: "MPV", label: "Monovolum" }
];

export const POWERTRAIN_OPTIONS: Option<Powertrain>[] = [
  { value: "PETROL", label: "Benzină" },
  { value: "DIESEL", label: "Diesel" },
  { value: "HYBRID", label: "Hibrid" },
  { value: "PLUG_IN_HYBRID", label: "Hibrid plug-in" },
  { value: "ELECTRIC", label: "Electric" }
];

export const CONDITION_OPTIONS: Option<NonNullable<BuyerProfile["condition"]>>[] = [
  { value: "NEW", label: "Nouă" },
  { value: "USED", label: "Second-hand" },
  { value: "ANY", label: "Oricare" }
];

export const BUDGET_KIND_OPTIONS: Option<NonNullable<BuyerProfile["budgetKind"]>>[] = [
  { value: "TOTAL", label: "Total" },
  { value: "MONTHLY", label: "Pe lună" }
];

export const FINANCING_OPTIONS: Option<NonNullable<BuyerProfile["financingMode"]>>[] = [
  { value: "CASH", label: "Cash" },
  { value: "FINANCE", label: "Finanțare" }
];

export const HOME_CHARGING_OPTIONS: Option<"yes" | "no">[] = [
  { value: "yes", label: "Da" },
  { value: "no", label: "Nu" }
];

export function labelOf<T extends string>(options: Option<T>[], value: T | undefined): string {
  return options.find(option => option.value === value)?.label ?? value ?? "";
}

export function formatEur(value: number): string {
  return `${value.toLocaleString("ro-RO", { maximumFractionDigits: 0 })} €`;
}
