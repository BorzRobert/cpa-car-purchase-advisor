"use client";

/**
 * Starea backend-ului la încărcarea paginii, cu tratarea „cold start"-ului.
 *
 * DE CE există: pe planul gratuit Render serviciul adoarme și se retrezește în ~1 minut + pornirea JVM. O cerere
 * obișnuită ar eșua sau ar rămâne agățată, iar interfața ar părea stricată. Aici pingul repetă încercările și
 * expune o stare explicită „waking", ca UI-ul să spună „Se trezește consilierul…" în loc de o eroare.
 *
 * DE CE fără TanStack Query: nu e instalat până în Faza 6, iar pentru un singur apel la încărcare un `useEffect`
 * e suficient; când apare clientul real, hook-ul se înlocuiește, nu se extinde.
 */

import { useCallback, useEffect, useState } from "react";
import { API_BASE_URL, fetchBackendHealth, pingBackend, type BackendHealth } from "@/lib/api/backend-health";

export type BackendStatus =
  | { phase: "not-configured" }
  | { phase: "checking" }
  | { phase: "waking" }
  | { phase: "ready"; health: BackendHealth }
  | { phase: "unreachable" };

/** După cât timp fără răspuns trecem din „se verifică" în „se trezește" (un backend treaz răspunde mult mai repede). */
const SLOW_AFTER_MS = 2_500;
const RETRY_EVERY_MS = 3_000;
/** Peste cold start-ul așteptat (~1–2 minute); după el afișăm „nu răspunde" cu buton de reîncercare. */
const GIVE_UP_AFTER_MS = 150_000;

function sleep(ms: number, signal: AbortSignal): Promise<void> {
  return new Promise(resolve => {
    const timer = setTimeout(resolve, ms);
    signal.addEventListener(
      "abort",
      () => {
        clearTimeout(timer);
        resolve();
      },
      { once: true }
    );
  });
}

export function useBackendStatus() {
  const [status, setStatus] = useState<BackendStatus>(
    API_BASE_URL ? { phase: "checking" } : { phase: "not-configured" }
  );
  // Crește la „Reîncearcă"; schimbarea ei relansează efectul de mai jos.
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    if (!API_BASE_URL) return;

    const controller = new AbortController();
    const { signal } = controller;
    const startedAt = Date.now();
    const slowTimer = setTimeout(() => {
      setStatus(current => (current.phase === "checking" ? { phase: "waking" } : current));
    }, SLOW_AFTER_MS);

    async function run() {
      while (!signal.aborted) {
        try {
          await pingBackend(signal);
          const health = await fetchBackendHealth(signal);
          if (!signal.aborted) setStatus({ phase: "ready", health });
          return;
        } catch {
          if (signal.aborted) return;
          if (Date.now() - startedAt > GIVE_UP_AFTER_MS) {
            setStatus({ phase: "unreachable" });
            return;
          }
          setStatus({ phase: "waking" });
          await sleep(RETRY_EVERY_MS, signal);
        }
      }
    }

    void run();

    return () => {
      clearTimeout(slowTimer);
      controller.abort();
    };
  }, [attempt]);

  const retry = useCallback(() => {
    setStatus({ phase: "checking" });
    setAttempt(value => value + 1);
  }, []);

  return { status, retry };
}
