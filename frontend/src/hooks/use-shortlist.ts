"use client";

/** Lista scurtă din backend: citire, adăugare (de pe un card de vehicul) și scoatere. */
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api/endpoints";

export const shortlistKey = ["shortlist"] as const;

export function useShortlist() {
  return useQuery({ queryKey: shortlistKey, queryFn: api.shortlist.list });
}

export function useAddToShortlist() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (variantId: string) => api.shortlist.add(variantId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: shortlistKey })
  });
}

export function useRemoveFromShortlist() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.shortlist.remove(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: shortlistKey })
  });
}
