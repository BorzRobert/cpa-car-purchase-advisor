import type { NextConfig } from "next";

/**
 * Configurația Next.
 *
 * DE CE e (aproape) goală și de ce e bine așa:
 * tot ce ne trebuie acum vine din convenții — rutele din structura de foldere, bundler-ul
 * (Turbopack, implicit în Next 16) și TypeScript-ul din `tsconfig.json`. Regula proiectului
 * e „un concept nou pe pas", deci nu adăugăm opțiuni de care nu avem încă nevoie.
 *
 * Ce va ajunge aici la pașii următori, ca să știi la ce e locul ăsta:
 * domeniile permise pentru imagini externe (`images.remotePatterns`) și, dacă apare nevoia,
 * headere de securitate. NU intră aici chei sau valori secrete: fișierul e citit și în
 * contextul de build al clientului, iar orice valoare pusă în el poate ajunge în bundle.
 * Secretele stau în `.env.local` și se citesc doar în cod de server.
 */
/**
 * `agentRules: false`: `next dev` ar genera singur `AGENTS.md` și `CLAUDE.md` în `frontend/`; instrucțiunile pentru
 * agenți au o singură sursă, `AGENTS.md` din rădăcina repo-ului (vezi secțiunea 9 din el).
 */
const nextConfig: NextConfig = { agentRules: false };

export default nextConfig;
