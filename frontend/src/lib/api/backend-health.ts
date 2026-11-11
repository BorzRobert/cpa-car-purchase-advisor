/**
 * Apelurile către backend pentru starea serviciului (ping de trezire și `GET /api/v1/health`).
 *
 * DE CE nu trec prin `client.ts`: aceste apeluri sunt publice și trebuie să funcționeze înainte să existe o sesiune —
 * tocmai ele trezesc backend-ul, iar o sesiune se creează abia după ce acesta răspunde.
 */
import { API_BASE_URL } from "@/lib/api/config";
import type { components } from "@/lib/api/schema";

export { API_BASE_URL };

/** Forma răspunsului `GET /api/v1/health`, din tipurile generate. */
export type BackendHealth = components["schemas"]["HealthResponse"];

/** Cât așteptăm un singur răspuns: un backend care se trezește poate lăsa conexiunea agățată. */
const ATTEMPT_TIMEOUT_MS = 10_000;

async function getJson<T>(path: string, signal: AbortSignal): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: { Accept: "application/json" },
    signal: AbortSignal.any([signal, AbortSignal.timeout(ATTEMPT_TIMEOUT_MS)])
  });
  if (!response.ok) {
    // Pagina de „se pornește" a gazdei vine ca 502/503, nu ca eroare de rețea: tratăm la fel, ca „încă nu e gata".
    throw new Error(`HTTP ${response.status} la ${path}`);
  }
  return (await response.json()) as T;
}

/**
 * Ping-ul de trezire: `/actuator/health` nu atinge baza de date, deci pornește doar serviciul Render, nu și Neon.
 * Rezolvă doar dacă backend-ul răspunde cu `UP`.
 */
export async function pingBackend(signal: AbortSignal): Promise<void> {
  const { status } = await getJson<{ status: string }>("/actuator/health", signal);
  if (status !== "UP") throw new Error(`Backend în starea ${status}`);
}

/** Starea aplicației (versiune, consilier configurat sau nu); apelabilă abia după un ping reușit. */
export function fetchBackendHealth(signal: AbortSignal): Promise<BackendHealth> {
  return getJson<BackendHealth>("/api/v1/health", signal);
}
