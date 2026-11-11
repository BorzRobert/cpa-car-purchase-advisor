/**
 * Erorile API-ului și textul lor pentru utilizator.
 *
 * DE CE există: backend-ul răspunde cu `problem+json` (RFC 9457) și `code` este identificatorul stabil; `detail` e în
 * engleză, pentru jurnale, deci nu se afișează. Aici `code` devine text românesc, într-un registru (nu într-un lanț de
 * `if`), ca o eroare nouă să fie o linie de date.
 */

export type ProblemPayload = {
  code?: string;
  status?: number;
  detail?: string;
  requestId?: string;
};

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    readonly detail?: string,
    readonly requestId?: string,
    readonly retryAfterSeconds?: number
  ) {
    super(detail ?? code);
    this.name = "ApiError";
  }
}

/** Codul special pentru un flux care s-a oprit fără `done` sau `error`; nu vine niciodată de la server. */
export const INCOMPLETE_STREAM = "incomplete-stream";
/** Codul special pentru o cerere care nu a ajuns la server (offline, backend care se trezește). */
export const NETWORK_ERROR = "network";

export function problemToError(problem: ProblemPayload, fallbackStatus: number, retryAfterSeconds?: number): ApiError {
  return new ApiError(
    problem.status ?? fallbackStatus,
    problem.code ?? "internal-error",
    problem.detail,
    problem.requestId,
    retryAfterSeconds
  );
}

/** Citește un răspuns de eroare; un corp care nu e JSON (pagina de „se pornește” a gazdei) devine tot o eroare tipată. */
export async function errorFromResponse(response: Response): Promise<ApiError> {
  const retryAfter = Number(response.headers.get("Retry-After"));
  const retryAfterSeconds = Number.isFinite(retryAfter) && retryAfter > 0 ? retryAfter : undefined;
  try {
    const problem = (await response.json()) as ProblemPayload;
    return problemToError(problem, response.status, retryAfterSeconds);
  } catch {
    return new ApiError(response.status, "internal-error", undefined, undefined, retryAfterSeconds);
  }
}

export type ErrorView = {
  title: string;
  message: string;
  /** „soft” = situație așteptată (limită, serviciu neactivat), nu o defecțiune; se afișează fără roșu. */
  tone: "soft" | "destructive";
};

function waitText(error: ApiError): string {
  const seconds = error.retryAfterSeconds;
  if (!seconds) return "Încearcă din nou puțin mai târziu.";
  // Abrevieri (s, min, h) ca să nu depindem de acordul „de” din română.
  if (seconds < 90) return `Încearcă din nou peste ${seconds} s.`;
  if (seconds < 5400) return `Încearcă din nou peste aproximativ ${Math.ceil(seconds / 60)} min.`;
  return `Încearcă din nou peste aproximativ ${Math.ceil(seconds / 3600)} h.`;
}

const VIEWS: Record<string, (error: ApiError) => ErrorView> = {
  "advisor-not-configured": () => ({
    title: "Consilierul nu este activat încă",
    message: "Serverul funcționează, dar nu are încă o cheie pentru modelul de limbaj. Încearcă din nou mai târziu.",
    tone: "soft"
  }),
  "budget-exceeded": error => ({
    title: "Limita zilnică de răspunsuri a fost atinsă",
    message: `Bugetul de răspunsuri se resetează la miezul nopții (UTC). ${waitText(error)}`,
    tone: "soft"
  }),
  "rate-limited": error => ({
    title: "Prea multe cereri",
    message: waitText(error),
    tone: "soft"
  }),
  "limit-reached": () => ({
    title: "Ai atins o limită",
    message: "Șterge o conversație veche sau un element din lista scurtă și încearcă din nou.",
    tone: "soft"
  }),
  "already-in-shortlist": () => ({
    title: "Este deja în lista scurtă",
    message: "Varianta aceasta este salvată deja.",
    tone: "soft"
  }),
  unauthorized: () => ({
    title: "Sesiunea a expirat",
    message: "Am creat o sesiune nouă; conversațiile vechi nu mai sunt disponibile. Încearcă din nou.",
    tone: "soft"
  }),
  "not-found": () => ({
    title: "Nu am găsit conversația",
    message: "Probabil a fost ștearsă. Începe o conversație nouă.",
    tone: "destructive"
  }),
  "validation-error": () => ({
    title: "Datele nu au fost acceptate",
    message: "Verifică valorile introduse (mesajul poate avea cel mult 4000 de caractere).",
    tone: "destructive"
  }),
  "invalid-input": () => ({
    title: "Datele nu au fost acceptate",
    message: "Verifică valorile introduse (de exemplu bugetul are nevoie și de tipul lui).",
    tone: "destructive"
  }),
  "advisor-unavailable": () => ({
    title: "Modelul nu a putut răspunde",
    message: "A apărut o problemă la generarea răspunsului. Încearcă din nou.",
    tone: "destructive"
  }),
  [INCOMPLETE_STREAM]: () => ({
    title: "Răspunsul s-a întrerupt",
    message: "Conexiunea s-a oprit înainte ca răspunsul să fie complet. Poți relua întrebarea.",
    tone: "destructive"
  }),
  [NETWORK_ERROR]: () => ({
    title: "Nu pot contacta serverul",
    message:
      "Dacă serverul tocmai se trezește, mai durează puțin (până la un minut). Încearcă din nou în câteva secunde.",
    tone: "destructive"
  })
};

export function describeError(error: unknown): ErrorView {
  if (error instanceof ApiError) {
    const view = VIEWS[error.code];
    if (view) return view(error);
  }
  return {
    title: "Ceva nu a mers",
    message: "A apărut o eroare neașteptată. Încearcă din nou.",
    tone: "destructive"
  };
}
