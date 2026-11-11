"use client";

/**
 * Conversațiile și mesajele lor, din backend, prin TanStack Query.
 *
 * DE CE cheile sunt exportate: hook-ul de chat invalidează aceleași chei după ce un răspuns s-a terminat, ca lista și
 * istoricul să se reia din backend (arhiva persistentă) în loc să fie întreținute de mână în două locuri.
 */
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api/endpoints";
import { fromApiMessage } from "@/lib/message-utils";
import { useAppStore } from "@/store/useAppStore";

export const conversationKeys = {
  list: ["conversations"] as const,
  messages: (id: string) => ["conversations", id, "messages"] as const
};

export function useConversations() {
  return useQuery({ queryKey: conversationKeys.list, queryFn: api.conversations.list });
}

export function useMessages(conversationId: string | null) {
  return useQuery({
    queryKey: conversationKeys.messages(conversationId ?? ""),
    queryFn: async () => (await api.conversations.messages(conversationId!)).map(fromApiMessage),
    enabled: conversationId !== null
  });
}

export function useRenameConversation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, title }: { id: string; title: string }) => api.conversations.rename(id, title),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: conversationKeys.list })
  });
}

export function useDeleteConversation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.conversations.remove(id),
    onSuccess: (_result, id) => {
      // Dacă ștergi conversația deschisă, zona de chat trebuie să revină la ecranul de start, nu să arate o conversație inexistentă.
      if (useAppStore.getState().activeConversationId === id) useAppStore.getState().setActiveConversation(null);
      queryClient.removeQueries({ queryKey: conversationKeys.messages(id) });
      return queryClient.invalidateQueries({ queryKey: conversationKeys.list });
    }
  });
}
