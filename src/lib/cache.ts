/**
 * Cache-ul local protejează retrimiterile identice, nu încearcă să fie infrastructură de producție.
 * Cheia este construită de ruta de chat, unde există toate datele care influențează răspunsul.
 */
export type CacheStore = {
  get: <T>(key: string) => T | undefined;
  set: <T>(key: string, value: T, ttlMs?: number) => void;
};

type CacheEntry = {
  value: unknown;
  expiresAt: number;
};

const MAX_ENTRIES = 100;
const DEFAULT_TTL_MS = 5 * 60 * 1000;
const entries = new Map<string, CacheEntry>();

export const cache: CacheStore = {
  get<T>(key: string): T | undefined {
    const entry = entries.get(key);
    if (!entry) return undefined;
    if (entry.expiresAt <= Date.now()) {
      entries.delete(key);
      return undefined;
    }
    return entry.value as T;
  },

  set<T>(key: string, value: T, ttlMs = DEFAULT_TTL_MS): void {
    if (entries.size >= MAX_ENTRIES && !entries.has(key)) {
      const oldestKey = entries.keys().next().value;
      if (oldestKey) entries.delete(oldestKey);
    }
    entries.set(key, { value, expiresAt: Date.now() + ttlMs });
  }
};
