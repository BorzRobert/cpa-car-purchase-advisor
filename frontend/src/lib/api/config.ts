/**
 * Adresa backend-ului, citită într-un singur loc.
 *
 * DE CE `process.env.NEXT_PUBLIC_API_BASE_URL` apare literal: Next.js înlocuiește la `next build` doar referințele
 * scrise exact așa (nu și `process.env[nume]`), deci după ce schimbi valoarea în Vercel e nevoie de Redeploy.
 */
export const API_BASE_URL = (process.env.NEXT_PUBLIC_API_BASE_URL ?? "").trim().replace(/\/+$/, "");
