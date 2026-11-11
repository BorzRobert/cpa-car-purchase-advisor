"use client";

/**
 * Furnizorul TanStack Query al aplicației.
 *
 * DE CE o componentă client separată: `layout.tsx` rămâne Server Component, iar `QueryClientProvider` cere context
 * React, deci trebuie să fie client. Clientul propriu-zis este un modul (`lib/query-client.ts`), ca modulul de sesiune
 * să-l poată reseta.
 */
import { QueryClientProvider } from "@tanstack/react-query";
import { queryClient } from "@/lib/query-client";

export function AppProviders({ children }: { children: React.ReactNode }) {
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
