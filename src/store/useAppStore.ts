"use client";

/**
 * Starea aplicației: profil, lista conversațiilor, provider, temă.
 *
 * DE CE un store global și nu `useState` în componente:
 * aceleași date sunt citite din trei locuri care nu se conțin unul pe altul — sidebar-ul
 * (lista de conversații), zona de chat și dialogul de preferințe (profilul, tema).
 * Cu `useState` ar trebui ridicate în layout și pasate prin toate nivelurile ca props. Numele
 * din profil e cazul limpede: apare în rândul de utilizator, în salutul de pe conversația nouă
 * și în formularul de editare.
 *
 * DE CE `persist` cu localStorage, deși mai târziu vine Supabase:
 * cerința e ca lista de conversații și profilul să supraviețuiască unui refresh chiar și fără
 * bază de date. `persist` face exact atât, iar la Faza 9 se înlocuiește stratul de storage —
 * componentele nu se ating, pentru că ele văd doar acest store.
 *
 * DE CE mesajele nu sunt aici:
 * `useChat` deține conversația deschisă și fluxul ei SSE. Store-ul păstrează numai lista pentru
 * sidebar; două surse pentru aceleași mesaje s-ar desincroniza inevitabil.
 */
import { useEffect, useState } from "react";
import { create } from "zustand";
import { persist, createJSONStorage, type StateStorage } from "zustand/middleware";
import type { Conversation, Profile, ProviderId, ThemePreference } from "@/lib/types";
import { DEFAULT_MODEL_ID, DEFAULT_PROVIDER_ID, PROVIDERS } from "@/lib/providers";
import { mockProfile } from "@/lib/mock/profile";
import { mockConversations } from "@/lib/mock/conversations";

/** Secțiunile dialogului de preferințe. Ținute aici pentru ca orice buton din aplicație să poată deschide direct secțiunea potrivită. */
export type SettingsSection = "general" | "profil" | "providere" | "despre";

type AppState = {
  // --- date persistate ---
  profile: Profile;
  conversations: Conversation[];
  activeConversationId: string | null;
  providerId: ProviderId;
  modelId: string;
  theme: ThemePreference;

  // --- stare efemeră (NU se salvează: vezi `partialize`) ---
  settingsOpen: boolean;
  settingsSection: SettingsSection;

  // --- acțiuni ---
  setProfile: (profile: Profile) => void;
  setTheme: (theme: ThemePreference) => void;
  selectProvider: (providerId: ProviderId) => void;
  setModel: (modelId: string) => void;
  startNewConversation: () => void;
  createConversation: (firstMessage: string) => void;
  selectConversation: (id: string) => void;
  renameConversation: (id: string, title: string) => void;
  deleteConversation: (id: string) => void;
  openSettings: (section?: SettingsSection) => void;
  setSettingsOpen: (open: boolean) => void;
  setSettingsSection: (section: SettingsSection) => void;
};

/**
 * Storage inert, folosit doar pe server (vezi `storage` mai jos).
 * DE CE nu aruncă erori: pe server nu e nimic de salvat și nimic de citit — trebuie doar să
 * nu se plângă nimeni. `getItem` întoarce `null`, adică „nu există stare salvată".
 */
const noopStorage: StateStorage = {
  getItem: () => null,
  setItem: () => undefined,
  removeItem: () => undefined
};

/** Titlul unei conversații noi se derivă din primul mesaj — la fel ca în aplicațiile reale de chat. */
function buildTitle(firstMessage: string): string {
  const clean = firstMessage.trim().replace(/\s+/g, " ");
  return clean.length > 48 ? `${clean.slice(0, 48)}…` : clean || "Conversație nouă";
}

export const useAppStore = create<AppState>()(
  persist(
    (set, get) => ({
      profile: mockProfile,
      conversations: mockConversations,
      activeConversationId: null,
      providerId: DEFAULT_PROVIDER_ID,
      modelId: DEFAULT_MODEL_ID,
      theme: "sistem",

      settingsOpen: false,
      settingsSection: "general",

      setProfile: profile => set({ profile }),
      setTheme: theme => set({ theme }),

      /**
       * DE CE schimbarea providerului resetează și modelul:
       * un model aparține unui provider. Dacă am păstra `modelId` la schimbare, ar rămâne un
       * model care nu există în noul provider — exact bug-ul pe care un registru bine folosit
       * trebuie să-l facă imposibil.
       */
      selectProvider: providerId => {
        const firstModel = PROVIDERS[providerId].models[0];
        set({ providerId, modelId: firstModel?.id ?? "" });
      },
      setModel: modelId => set({ modelId }),

      /**
       * DE CE „conversație nouă" nu creează imediat un obiect în listă:
       * altfel fiecare click pe „+ New" ar lăsa în sidebar o conversație goală, fără titlu.
       * Conversația se naște la primul mesaj — atunci avem și din ce să-i facem titlul.
       */
      startNewConversation: () => set({ activeConversationId: null }),

      createConversation: firstMessage =>
        set(state => {
          const conversation: Conversation = {
            id: crypto.randomUUID(),
            title: buildTitle(firstMessage),
            createdAt: new Date().toISOString()
          };

          return { conversations: [conversation, ...state.conversations], activeConversationId: conversation.id };
        }),

      selectConversation: id => set({ activeConversationId: id }),

      renameConversation: (id, title) =>
        set(state => ({
          conversations: state.conversations.map(conversation =>
            conversation.id === id ? { ...conversation, title: title.trim() || conversation.title } : conversation
          )
        })),

      /**
       * DE CE ștergerea trebuie să atingă și `activeConversationId`:
       * dacă ștergi conversația deschisă, id-ul activ ar rămâne să indice ceva inexistent și
       * zona de chat ar randa gol, fără să știe de ce. Revenim explicit la ecranul de start.
       */
      deleteConversation: id =>
        set(state => ({
          conversations: state.conversations.filter(conversation => conversation.id !== id),
          activeConversationId: state.activeConversationId === id ? null : state.activeConversationId
        })),

      openSettings: section => set({ settingsOpen: true, settingsSection: section ?? get().settingsSection }),
      setSettingsOpen: open => set({ settingsOpen: open }),
      setSettingsSection: section => set({ settingsSection: section })
    }),
    {
      name: "skillforge-app",

      /**
       * DE CE `skipHydration` și rehidratare manuală (vezi `useStoreHydration`):
       * pagina e randată mai întâi pe server, unde `localStorage` nu există. Dacă store-ul s-ar
       * citi din localStorage în timpul primei randări din browser, HTML-ul serverului și cel
       * al clientului ar diferi, iar React ar raporta o eroare de hidratare. Așa: prima randare
       * folosește datele de start (identice pe server și în browser), iar starea salvată intră
       * imediat după montare.
       */
      skipHydration: true,

      /**
       * DE CE storage-ul e ales printr-o funcție cu verificare de `window`:
       * modulul ăsta e importat și în timpul randării pe server (și la `next build`), unde
       * `localStorage` nu are ce căuta. Fără verificare, build-ul avertizează că `localStorage`
       * nu e disponibil — iar în alte medii ar fi de-a dreptul o eroare. Pe server dăm un
       * storage inert: nu are ce să citească, fiindcă starea utilizatorului e în browserul lui.
       */
      storage: createJSONStorage(() => (typeof window === "undefined" ? noopStorage : window.localStorage)),

      /**
       * DE CE nu salvăm tot:
       * Starea dialogului este de moment. Salvată, ar redeschide fereastra la următoarea vizită.
       */
      partialize: state => ({
        profile: state.profile,
        conversations: state.conversations.map(({ id, title, createdAt }) => ({ id, title, createdAt })),
        activeConversationId: state.activeConversationId,
        providerId: state.providerId,
        modelId: state.modelId,
        theme: state.theme
      }),

      /**
       * DE CE o versiune: prima migrare vine sigur.
       * Când profilul primește un câmp nou, starea salvată pe calculatorul cuiva va fi veche.
       * Cu `version` + `migrate` avem unde trata cazul, în loc să crape aplicația pe date vechi.
       */
      version: 2
    }
  )
);

/**
 * Citește starea salvată din `localStorage`, după ce componenta s-a montat în browser.
 *
 * DE CE e nevoie de un hook și nu se întâmplă automat:
 * am cerut `skipHydration: true` tocmai ca prima randare să fie identică pe server și în
 * browser. Prețul e că rehidratarea trebuie pornită explicit — aici.
 *
 * DE CE întoarce un boolean:
 * cât timp starea salvată nu a fost citită, lista de conversații e cea de start, nu a
 * utilizatorului. Componentele folosesc valoarea asta ca să afișeze `Skeleton` în loc de date
 * care s-ar schimba sub ochii lui — și așa avem un motiv REAL pentru starea de încărcare,
 * nu una simulată.
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
