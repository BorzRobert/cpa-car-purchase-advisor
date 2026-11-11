/**
 * Clientul HTTP al backend-ului: adaugă tokenul de sesiune, `X-Request-Id` și transformă erorile în `ApiError`.
 *
 * DE CE un singur loc: tokenul, reîncercarea după 401 și tratarea erorilor de rețea nu trebuie repetate în fiecare
 * apel. Tipurile cererilor și răspunsurilor vin din `schema.d.ts` (generat din OpenAPI), nu sunt scrise de mână.
 *
 * DE CE 401 se reîncearcă o singură dată: tokenul stocat poate fi invalid (sesiune ștearsă, bază resetată); o sesiune
 * nouă rezolvă cazul, dar un al doilea 401 ar însemna o problemă reală, nu un token vechi.
 */
import { API_BASE_URL } from "@/lib/api/config";
import { ApiError, NETWORK_ERROR, errorFromResponse } from "@/lib/api/errors";
import { ensureSessionToken, renewSession } from "@/lib/api/session";

type RequestOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  signal?: AbortSignal;
  accept?: string;
};

async function send(path: string, options: RequestOptions, token: string): Promise<Response> {
  const headers: Record<string, string> = {
    Accept: options.accept ?? "application/json",
    Authorization: `Bearer ${token}`,
    "X-Request-Id": crypto.randomUUID()
  };
  if (options.body !== undefined) headers["Content-Type"] = "application/json";

  try {
    return await fetch(`${API_BASE_URL}${path}`, {
      method: options.method ?? "GET",
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: options.signal
    });
  } catch (error) {
    // O oprire cerută de utilizator trebuie să rămână oprire, nu „eroare de rețea”.
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw new ApiError(0, NETWORK_ERROR);
  }
}

/** Răspunsul brut (pentru fluxul SSE); aruncă `ApiError` pentru orice status în afara 2xx. */
export async function apiRequest(path: string, options: RequestOptions = {}): Promise<Response> {
  let token = await ensureSessionToken();
  let response = await send(path, options, token);
  if (response.status === 401) {
    token = await renewSession(token);
    response = await send(path, options, token);
  }
  if (!response.ok) throw await errorFromResponse(response);
  return response;
}

export async function apiJson<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const response = await apiRequest(path, options);
  return response.status === 204 ? (undefined as T) : ((await response.json()) as T);
}
