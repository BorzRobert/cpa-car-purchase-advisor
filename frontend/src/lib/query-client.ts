/**
 * Clientul TanStack Query, un singur exemplar pentru toată aplicația.
 *
 * DE CE modul și nu `useState(() => new QueryClient())` în provider: modulul de sesiune trebuie să poată goli cache-ul
 * când se creează o sesiune nouă (datele vechi aparțineau altei sesiuni), iar el nu e o componentă React. Pe server nu
 * rulează nicio interogare (hook-urile `useQuery` nu pornesc la randarea pe server), deci un exemplar partajat nu
 * amestecă date între utilizatori.
 */
import { QueryClient } from "@tanstack/react-query";
import { ApiError } from "@/lib/api/errors";

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: false,
      // Reîncercăm doar erorile de rețea (backend care se trezește); un 4xx nu se repară prin repetare.
      retry: (failureCount, error) => failureCount < 2 && (!(error instanceof ApiError) || error.status === 0)
    }
  }
});
