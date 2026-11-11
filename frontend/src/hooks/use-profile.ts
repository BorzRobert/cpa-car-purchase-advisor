"use client";

/**
 * Profilul cumpărătorului din backend (formularul de nevoi și panoul care îl arată).
 *
 * DE CE `setQueryData` după salvare: `PUT` întoarce profilul salvat (curățat de backend); îl punem direct în cache în
 * loc să mai cerem o dată același lucru.
 */
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, type ProfileRequest } from "@/lib/api/endpoints";

export const profileKey = ["profile"] as const;

export function useProfile() {
  return useQuery({ queryKey: profileKey, queryFn: api.profile.get });
}

export function useSaveProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (profile: ProfileRequest) => api.profile.replace(profile),
    onSuccess: saved => queryClient.setQueryData(profileKey, saved)
  });
}

export function useClearProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => api.profile.clear(),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: profileKey })
  });
}
