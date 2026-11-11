"use client";

/**
 * Starea aplicației din browser: tokenul de sesiune, conversația deschisă și starea dialogului de preferințe.
 *
 * DE CE atât de puțin: conversațiile, mesajele, profilul și lista scurtă trăiesc în baza de date a backend-ului și se
 * citesc prin TanStack Query. Aici rămâne doar ce backend-ul nu are cum să știe — cine sunt eu (tokenul) și ce am
 * deschis acum. Tema stă în `ThemeProvider` (`cpa-theme`), nu aici.
 *
 * DE CE se salvează doar tokenul: conversația activă ar putea indica, după un refresh, o conversație ștearsă între
 * timp; la repornire pornim de la ecranul de start, iar lista din sidebar o arată pe cele existente.
 *
 * DE CE `skipHydration` și rehidratare manuală (`useStoreHydration`): pagina e randată întâi pe server, unde
 * `localStorage` nu există; citirea lui în prima randare din browser ar produce o eroare de hidratare React.
 */
import { useEffect, useState } from "react";
import { create } from "zustand";
import { persist, createJSONStorage, type StateStorage } from "zustand/middleware";

/** Secțiunile dialogului de preferințe, ținute aici ca orice buton să poată deschide direct secțiunea potrivită. */
export type SettingsSection = "general" | "profil" | "lista";

type AppState = {
  // --- date persistate ---
  sessionToken: string | null;

  // --- stare efemeră (NU se salvează: vezi `partialize`) ---
  activeConversationId: string | null;
  settingsOpen: boolean;
  settingsSection: SettingsSection;

  // --- acțiuni ---
  setSessionToken: (token: string | null) => void;
  setActiveConversation: (id: string | null) => void;
  openSettings: (section?: SettingsSection) => void;
  setSettingsOpen: (open: boolean) => void;
  setSettingsSection: (section: SettingsSection) => void;
};

/** Pe server nu e nimic de citit sau de salvat; `getItem` întoarce `null`, adică „nu există stare salvată”. */
const noopStorage: StateStorage = {
  getItem: () => null,
  setItem: () => undefined,
  removeItem: () => undefined
};

export const useAppStore = create<AppState>()(
  persist(
    (set, get) => ({
      sessionToken: null,
      activeConversationId: null,
      settingsOpen: false,
      settingsSection: "general",

      setSessionToken: token => set({ sessionToken: token }),
      setActiveConversation: id => set({ activeConversationId: id }),
      openSettings: section => set({ settingsOpen: true, settingsSection: section ?? get().settingsSection }),
      setSettingsOpen: open => set({ settingsOpen: open }),
      setSettingsSection: section => set({ settingsSection: section })
    }),
    {
      name: "cpa-app",
      skipHydration: true,
      storage: createJSONStorage(() => (typeof window === "undefined" ? noopStorage : window.localStorage)),
      partialize: state => ({ sessionToken: state.sessionToken }),

      /**
       * Forma salvată s-a schimbat (v8): până la v7 `cpa-app` ținea profilul, conversațiile și providerul din
       * aplicația veche, care acum stau în backend sau nu mai există. Nu se migrează nimic din ele — produsul nu are
       * utilizatori de păstrat — se pornește curat, fără token, iar aplicația își cere o sesiune nouă.
       */
      version: 8,
      migrate: () => ({ sessionToken: null })
    }
  )
);

/**
 * Citește starea salvată din `localStorage` după montare și spune când e gata, ca UI-ul să afișeze scheleturi în loc
 * de date care s-ar schimba sub ochii utilizatorului.
 */
export function useStoreHydration(): boolean {
  const [hydrated, setHydrated] = useState(() => useAppStore.persist.hasHydrated());

  useEffect(() => {
    const unsubscribe = useAppStore.persist.onFinishHydration(() => setHydrated(true));
    void useAppStore.persist.rehydrate();
    return unsubscribe;
  }, []);

  return hydrated;
}
