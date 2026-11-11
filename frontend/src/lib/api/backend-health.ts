/**
 * Apelurile către backend pentru starea serviciului (Faza 3, „walking skeleton").
 *
 * DE CE există fișierul: URL-ul backend-ului vine dintr-o singură variabilă publică, iar toate apelurile îl
 * citesc de aici, nu din `process.env` împrăștiat prin componente. Clientul complet (`lib/api`, tipuri generate
 * din OpenAPI) apare în Faza 6 și înlocuiește tipul scris de mână de mai jos.
 *
 * DE CE `process.env.NEXT_PUBLIC_API_BASE_URL` apare literal: Next.js înlocuiește la `next build` doar referințele
 * scrise exact așa (nu și `process.env[nume]`), deci după ce schimbi valoarea în Vercel e nevoie de Redeploy.
 */

export const API_BASE_URL = (process.env.NEXT_PUBLIC_API_BASE_URL ?? "").trim().replace(/\/+$/, "");

/** Forma răspunsului `GET /api/v1/health`; temporară, până la tipurile generate din OpenAPI (Faza 6). */
export type BackendHealth = {
  status: string;
  service: string;
  version: string;
  advisorConfigured: boolean;
};

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
