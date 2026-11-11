/**
 * Tipurile temei, într-un singur loc.
 *
 * DE CE „sistem” e o valoare separată de light/dark: e o *regulă* („urmează sistemul de operare”), nu o culoare. Tema
 * efectiv aplicată se calculează din ea — vezi `theme-provider`. Restul tipurilor aplicației vin din alte locuri:
 * cele ale API-ului din `lib/api/schema.d.ts` (generat), cele ale fluxului din `lib/advisor-events.ts`.
 */

/** Preferința de temă, așa cum o alege utilizatorul. */
export type ThemePreference = "sistem" | "light" | "dark";

/** Tema efectiv aplicată pe document. „sistem” nu poate apărea aici — a fost deja rezolvat. */
export type ResolvedTheme = "light" | "dark";
