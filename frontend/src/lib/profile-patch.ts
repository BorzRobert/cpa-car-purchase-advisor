/**
 * Aplică pe profil patch-ul din evenimentul SSE `profile`, ca funcție pură.
 *
 * DE CE există: panoul lateral trebuie să se actualizeze în timp ce agentul lucrează, fără o cerere în plus. Backend-ul
 * trimite doar câmpurile schimbate; `null` înseamnă „câmp șters”, iar după încheierea fluxului profilul se reîncarcă
 * oricum din backend (sursa de adevăr), deci o greșeală de aici nu poate dăinui.
 */
import type { BuyerProfile } from "@/lib/api/endpoints";

const LIST_FIELDS = new Set<string>(["bodyTypes", "powertrains", "mustHaves", "dealBreakers"]);

export function applyProfilePatch(profile: BuyerProfile, patch: Record<string, unknown>): BuyerProfile {
  const next: Record<string, unknown> = { ...profile };
  for (const [key, value] of Object.entries(patch)) {
    if (value !== null) next[key] = value;
    // O listă ștearsă devine listă goală (ca în profilul gol din backend), nu `undefined`.
    else if (LIST_FIELDS.has(key)) next[key] = [];
    else delete next[key];
  }
  return next as BuyerProfile;
}
