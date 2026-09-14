"use client";

/**
 * Traduce preferința de temă a utilizatorului în clasa `.dark` pe `<html>`.
 *
 * DE CE există componenta asta, în loc să pună fiecare componentă clase de culoare:
 * în Tailwind v4 + shadcn, tema e un set de variabile CSS redefinite sub `.dark`
 * (vezi `globals.css`). Nimeni nu are nevoie să știe ce temă e activă — clasele `bg-background`
 * și `text-foreground` iau valoarea potrivită singure. Un singur loc atinge documentul: acesta.
 *
 * DE CE „sistem" nu e o a treia culoare, ci o regulă:
 * „sistem" înseamnă „urmează sistemul de operare". Trebuie ASCULTAT, nu doar citit o dată:
 * dacă utilizatorul trece macOS-ul pe dark la apus, aplicația trebuie să se schimbe fără
 * refresh. De aici listener-ul pe `matchMedia("(prefers-color-scheme: dark)")`.
 *
 * DE CE tema se schimbă DOAR din preferințe (nu există buton în header):
 * e o setare, nu o acțiune frecventă. Un buton permanent în header ar ocupa spațiu în zona în
 * care vrem doar conversația — iar cerința de produs e explicită: centrul rămâne curat.
 */

import { createContext, useContext, useEffect, useState } from "react";
import type { ResolvedTheme, ThemePreference } from "@/lib/types";

/** Interogarea sistemului de operare. Constantă, ca să nu apară scrisă greșit în două locuri. */
const DARK_QUERY = "(prefers-color-scheme: dark)";
const THEME_STORAGE_KEY = "skillforge-theme";
const LEGACY_STORAGE_KEY = "skillforge-app";

type ThemeContextValue = {
  preference: ThemePreference;
  resolvedTheme: ResolvedTheme;
  setTheme: (preference: ThemePreference) => void;
};

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

/**
 * Din preferință + starea sistemului rezultă tema efectivă.
 * DE CE o funcție pură, exportată: e singura regulă de decizie a temei, deci trebuie să fie
 * citibilă separat de efectele React.
 */
export function resolveTheme(preference: ThemePreference, systemPrefersDark: boolean): ResolvedTheme {
  if (preference === "sistem") return systemPrefersDark ? "dark" : "light";
  return preference;
}

function isThemePreference(value: unknown): value is ThemePreference {
  return value === "sistem" || value === "light" || value === "dark";
}

function readStoredTheme(): ThemePreference {
  try {
    const storedTheme = localStorage.getItem(THEME_STORAGE_KEY);
    if (isThemePreference(storedTheme)) return storedTheme;

    // Punte temporară: păstrăm alegerea veche când utilizatorul deschide prima dată versiunea nouă.
    const legacyState = localStorage.getItem(LEGACY_STORAGE_KEY);
    const legacyTheme = legacyState ? JSON.parse(legacyState).state?.theme : undefined;
    return isThemePreference(legacyTheme) ? legacyTheme : "sistem";
  } catch {
    // Stocarea poate fi blocată de browser; tema sistemului este un fallback valid și sigur.
    return "sistem";
  }
}

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [preference, setPreference] = useState<ThemePreference>(() =>
    typeof window === "undefined" ? "sistem" : readStoredTheme()
  );
  const [resolvedTheme, setResolvedTheme] = useState<ResolvedTheme>("light");

  useEffect(() => {
    const media = window.matchMedia(DARK_QUERY);

    const apply = () => {
      const resolved = resolveTheme(preference, media.matches);
      setResolvedTheme(resolved);
      // `toggle` cu al doilea argument: adaugă clasa când e dark, o scoate când e light.
      document.documentElement.classList.toggle("dark", resolved === "dark");
      // `color-scheme` spune browserului cum să deseneze ce NU controlăm noi:
      // scrollbar-uri, câmpuri native, meniul de select.
      document.documentElement.style.colorScheme = resolved;
    };

    apply();

    /**
     * DE CE ne abonăm doar pe „sistem":
     * la „light" sau „dark" alegerea utilizatorului bate sistemul de operare, deci un eveniment
     * de la el nu trebuie să schimbe nimic. Abonamentul e și curățat la demontare — altfel, la
     * fiecare schimbare de temă am lăsa în urmă un listener care mai ascultă degeaba.
     */
    if (preference !== "sistem") return;

    media.addEventListener("change", apply);
    return () => media.removeEventListener("change", apply);
  }, [preference]);

  useEffect(() => {
    try {
      localStorage.setItem(THEME_STORAGE_KEY, preference);
    } catch {
      // Tema rămâne funcțională și fără persistență; blocarea storage-ului nu trebuie să crape UI-ul.
    }
  }, [preference]);

  const setTheme = (nextPreference: ThemePreference) => setPreference(nextPreference);

  return <ThemeContext.Provider value={{ preference, resolvedTheme, setTheme }}>{children}</ThemeContext.Provider>;
}

export function useTheme(): ThemeContextValue {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error("useTheme must be used inside ThemeProvider");
  }
  return context;
}
