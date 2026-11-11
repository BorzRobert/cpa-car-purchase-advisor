"use client";

/**
 * Grup de opțiuni (selecție simplă sau multiplă) construit dintr-un registru de etichete.
 *
 * DE CE o componentă generică: formularul de nevoi are cinci câmpuri de acest tip (caroserie, motorizare, stare, tip
 * de buget, finanțare); fiecare ar repeta aceeași conversie între `string[]` (ce știe `ToggleGroup`) și tipul enum.
 * Conversia o face un filtru de tip, nu un `as`, ca o valoare străină să nu poată intra în profil.
 *
 * Selecție simplă: a doua apăsare pe opțiunea activă o deselectează (rămâne „nespecificat”), fiindcă toate câmpurile
 * profilului sunt opționale.
 */
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import type { Option } from "@/lib/profile-options";

type OptionToggleProps<T extends string> = {
  label: string;
  options: Option<T>[];
  value: T[];
  onChange: (value: T[]) => void;
  multiple?: boolean;
};

export function OptionToggle<T extends string>({ label, options, value, onChange, multiple }: OptionToggleProps<T>) {
  return (
    <ToggleGroup
      multiple={multiple}
      value={value}
      onValueChange={next => onChange(next.filter((item): item is T => options.some(option => option.value === item)))}
      variant="outline"
      size="sm"
      className="flex-wrap"
      aria-label={label}
    >
      {options.map(option => (
        <ToggleGroupItem key={option.value} value={option.value}>
          {option.label}
        </ToggleGroupItem>
      ))}
    </ToggleGroup>
  );
}
