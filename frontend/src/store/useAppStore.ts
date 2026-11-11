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
 * DE CE `skipHydration` și rehidratare manuală (în `ensureSessionToken`): pagina e randată întâi pe server, unde
 * `localStorage` nu există; citirea lui în prima randare din browser ar produce o eroare de hidratare React. Rehidratarea
 * se face înainte de prima cerere către backend, adică exact când tokenul devine necesar.
 */
import { create } from "zustand";
import { persist, createJSONStorage, type StateStorage } from "zustand/middleware";

/** Secțiunile dialogului de preferințe, ținute aici ca orice buton să poată deschide direct secțiunea potrivită. */
export type SettingsSection = "general" | "profil" | "lista";

type AppState = {
  // --- date persistate ---
  sessionToken: string | null;
  /** A văzut sau a sărit peste dialogul de bun venit; o dată văzut, nu mai apare. */
  onboardingSeen: boolean;

  // --- stare efemeră (NU se salvează: vezi `partialize`) ---
  activeConversationId: string | null;
  settingsOpen: boolean;
  settingsSection: SettingsSection;
  /** Panoul lateral ca foaie de jos (pe ecrane înguste; pe cele late panoul e mereu vizibil). */
  panelOpen: boolean;
  guideOpen: boolean;
  /** Text de pus în composer (din ghid sau din sugestii); `Chat` îl preia și îl golește. */
  pendingPrompt: string | null;

  // --- acțiuni ---
  setSessionToken: (token: string | null) => void;
  setOnboardingSeen: (seen: boolean) => void;
  setActiveConversation: (id: string | null) => void;
  openSettings: (section?: SettingsSection) => void;
  setSettingsOpen: (open: boolean) => void;
  setSettingsSection: (section: SettingsSection) => void;
  setPanelOpen: (open: boolean) => void;
  setGuideOpen: (open: boolean) => void;
  setPendingPrompt: (prompt: string | null) => void;
};

/** Pe server nu e nimic de citit sau de salvat; `getItem` întoarce `null`, adică „nu există stare salvată”. */
const noopStorage: StateStorage = {
  getItem: () => null,
  setItem: () => undefined,
  removeItem: () => undefined
};

/** Migrarea formei salvate; exportată ca să se poată testa fără browser. Din v8 se păstrează doar tokenul. */
export function migrateAppState(
  persisted: unknown,
  version: number
): { sessionToken: string | null; onboardingSeen: boolean } {
  const token = (persisted as { sessionToken?: unknown } | null)?.sessionToken;
  return { sessionToken: version === 8 && typeof token === "string" ? token : null, onboardingSeen: false };
}

export const useAppStore = create<AppState>()(
  persist(
    (set, get) => ({
      sessionToken: null,
      onboardingSeen: false,
      activeConversationId: null,
      settingsOpen: false,
      settingsSection: "general",
      panelOpen: false,
      guideOpen: false,
      pendingPrompt: null,

      setSessionToken: token => set({ sessionToken: token }),
      setOnboardingSeen: seen => set({ onboardingSeen: seen }),
      setActiveConversation: id => set({ activeConversationId: id }),
      openSettings: section => set({ settingsOpen: true, settingsSection: section ?? get().settingsSection }),
      setSettingsOpen: open => set({ settingsOpen: open }),
      setSettingsSection: section => set({ settingsSection: section }),
      setPanelOpen: open => set({ panelOpen: open }),
      setGuideOpen: open => set({ guideOpen: open }),
      setPendingPrompt: prompt => set({ pendingPrompt: prompt })
    }),
    {
      name: "cpa-app",
      skipHydration: true,
      storage: createJSONStorage(() => (typeof window === "undefined" ? noopStorage : window.localStorage)),
      partialize: state => ({ sessionToken: state.sessionToken, onboardingSeen: state.onboardingSeen }),

      /**
       * Forma salvată s-a schimbat (v9: apare `onboardingSeen`). Din v8 se păstrează tokenul, ca cine a folosit deja
       * aplicația să nu-și piardă sesiunea, iar dialogul de bun venit se arată o dată (nu l-a văzut niciodată). Până la
       * v7 `cpa-app` ținea profilul, conversațiile și providerul din aplicația veche: acelea nu se migrează.
       */
      version: 9,
      migrate: migrateAppState
    }
  )
);
