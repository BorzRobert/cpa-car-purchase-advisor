/**
 * Profilul cumpărătorului ca listă de chip-uri scurte („Buget 20.000 €”, „15.000 km/an”) pentru panoul lateral.
 *
 * DE CE un registru: fiecare câmp știe singur cum se scrie; un câmp nou din profil e o intrare de date, nu un `if` nou.
 * Câmpurile necompletate nu produc chip, deci panoul arată exact ce știe consilierul până acum.
 */
import type { BuyerProfile } from "@/lib/api/endpoints";
import {
  BODY_TYPE_OPTIONS,
  BUDGET_KIND_OPTIONS,
  CONDITION_OPTIONS,
  FINANCING_OPTIONS,
  POWERTRAIN_OPTIONS,
  formatEur,
  labelOf,
  type Option
} from "@/lib/profile-options";

export type ProfileChip = { id: string; label: string };

type ChipRule = { id: string; chip: (profile: BuyerProfile) => string | null };

const joined = <T extends string>(values: T[] | undefined, options: Option<T>[]) =>
  values && values.length > 0 ? values.map(value => labelOf(options, value)).join(", ") : null;

const CHIP_RULES: ChipRule[] = [
  {
    id: "budget",
    chip: ({ budgetAmount, budgetKind }) =>
      budgetAmount === undefined || budgetAmount === null
        ? null
        : `Buget ${formatEur(budgetAmount)}${budgetKind ? ` ${labelOf(BUDGET_KIND_OPTIONS, budgetKind).toLowerCase()}` : ""}`
  },
  {
    id: "financing",
    chip: ({ financingMode }) => (financingMode ? labelOf(FINANCING_OPTIONS, financingMode) : null)
  },
  {
    id: "yearlyKm",
    chip: ({ yearlyKm }) =>
      yearlyKm === undefined || yearlyKm === null ? null : `${yearlyKm.toLocaleString("ro-RO")} km/an`
  },
  {
    id: "cityPercent",
    chip: ({ cityPercent }) => (cityPercent === undefined || cityPercent === null ? null : `${cityPercent}% oraș`)
  },
  {
    id: "passengers",
    chip: ({ passengers }) => (passengers === undefined || passengers === null ? null : `${passengers} pasageri`)
  },
  { id: "bodyTypes", chip: ({ bodyTypes }) => joined(bodyTypes, BODY_TYPE_OPTIONS) },
  { id: "powertrains", chip: ({ powertrains }) => joined(powertrains, POWERTRAIN_OPTIONS) },
  { id: "condition", chip: ({ condition }) => (condition ? labelOf(CONDITION_OPTIONS, condition) : null) },
  {
    id: "homeCharging",
    chip: ({ homeCharging }) =>
      homeCharging === undefined || homeCharging === null
        ? null
        : homeCharging
          ? "Încărcare acasă"
          : "Fără încărcare acasă"
  },
  {
    id: "mustHaves",
    chip: ({ mustHaves }) => (mustHaves && mustHaves.length > 0 ? `Obligatoriu: ${mustHaves.join(", ")}` : null)
  },
  {
    id: "dealBreakers",
    chip: ({ dealBreakers }) => (dealBreakers && dealBreakers.length > 0 ? `Exclus: ${dealBreakers.join(", ")}` : null)
  }
];

export function profileChips(profile: BuyerProfile): ProfileChip[] {
  return CHIP_RULES.flatMap(rule => {
    const label = rule.chip(profile);
    return label ? [{ id: rule.id, label }] : [];
  });
}
