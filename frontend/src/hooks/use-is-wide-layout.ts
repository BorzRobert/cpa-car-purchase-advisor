import * as React from "react";

/**
 * Spune dacă ecranul e destul de lat ca panoul lateral să stea lângă conversație (≥ 1280 px).
 *
 * DE CE `useSyncExternalStore`, ca în `use-mobile.ts`: `matchMedia` e o stare din afara lui React. Pe server presupunem
 * ecran îngust (panoul e o foaie, deci nu se randează nimic în plus), iar prima randare din browser corectează.
 * DE CE o singură instanță a panoului la un moment dat: două copii montate (una ascunsă) ar dubla interogările și id-urile.
 */
const WIDE_QUERY = "(min-width: 1280px)";

function subscribe(onStoreChange: () => void) {
  const mql = window.matchMedia(WIDE_QUERY);
  mql.addEventListener("change", onStoreChange);
  return () => mql.removeEventListener("change", onStoreChange);
}

export function useIsWideLayout() {
  return React.useSyncExternalStore(
    subscribe,
    () => window.matchMedia(WIDE_QUERY).matches,
    () => false
  );
}
