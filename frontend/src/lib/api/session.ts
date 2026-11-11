/**
 * Sesiunea anonimă: obținerea, păstrarea și înlocuirea tokenului.
 *
 * DE CE tokenul se creează „leneș”, la prima cerere: nu are rost să cerem o sesiune cuiva care doar deschide pagina,
 * iar un backend care doarme (cold start) ar bloca încărcarea. Prima cerere reală (lista de conversații) o declanșează.
 *
 * DE CE `pending`: mai multe interogări pornesc simultan la încărcare; fără el, fiecare ar crea propria sesiune și
 * ar epuiza limita de sesiuni noi per IP.
 *
 * DE CE la 401 se resetează tot cache-ul: o sesiune nouă nu vede nimic din ce a creat cea veche, deci datele din cache
 * ar minți (interogările active se reiau cu tokenul nou). Dacă mai multe cereri primesc 401 deodată, doar prima
 * înlocuiește tokenul (compară cu cel respins).
 */
import { API_BASE_URL } from "@/lib/api/config";
import { errorFromResponse } from "@/lib/api/errors";
import type { components } from "@/lib/api/schema";
import { queryClient } from "@/lib/query-client";
import { useAppStore } from "@/store/useAppStore";

let pending: Promise<string> | null = null;

function createSession(): Promise<string> {
  if (pending) return pending;
  pending = (async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/v1/sessions`, {
        method: "POST",
        headers: { Accept: "application/json" }
      });
      if (!response.ok) throw await errorFromResponse(response);
      const { token } = (await response.json()) as components["schemas"]["IssuedSession"];
      if (!token) throw new Error("Răspunsul pentru sesiune nu conține token.");
      useAppStore.getState().setSessionToken(token);
      return token;
    } finally {
      pending = null;
    }
  })();
  return pending;
}

/** Tokenul curent; creează o sesiune dacă nu există. Așteaptă citirea din `localStorage` înainte să decidă. */
export async function ensureSessionToken(): Promise<string> {
  if (!useAppStore.persist.hasHydrated()) await useAppStore.persist.rehydrate();
  return useAppStore.getState().sessionToken ?? createSession();
}

/** Cheamă-l după un 401: înlocuiește tokenul respins cu unul nou și întoarce tokenul de folosit la reîncercare. */
export async function renewSession(rejectedToken: string): Promise<string> {
  const store = useAppStore.getState();
  if (store.sessionToken === rejectedToken) {
    store.setSessionToken(null);
    store.setActiveConversation(null);
    void queryClient.resetQueries();
  }
  return ensureSessionToken();
}
