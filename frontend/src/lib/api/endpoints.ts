/**
 * Apelurile către backend, câte unul pe endpoint, cu tipurile generate din OpenAPI.
 *
 * DE CE obiect și nu funcții împrăștiate în hook-uri: contractul se vede într-un singur loc, iar când se schimbă
 * `openapi.json` și se regenerează `schema.d.ts`, erorile de tip apar aici, nu în producție.
 */
import { apiJson, apiRequest } from "@/lib/api/client";
import type { components } from "@/lib/api/schema";

type Schemas = components["schemas"];

export type Conversation = Schemas["Conversation"];
export type ApiMessage = Schemas["Message"];
export type BuyerProfile = Schemas["BuyerProfile"];
export type ProfileRequest = Schemas["ProfileRequest"];
export type ShortlistItem = Schemas["ShortlistItem"];

export const api = {
  conversations: {
    list: () => apiJson<Conversation[]>("/api/v1/conversations"),
    create: (title?: string) => apiJson<Conversation>("/api/v1/conversations", { method: "POST", body: { title } }),
    rename: (id: string, title: string) =>
      apiJson<Conversation>(`/api/v1/conversations/${id}`, { method: "PATCH", body: { title } }),
    remove: (id: string) => apiJson<void>(`/api/v1/conversations/${id}`, { method: "DELETE" }),
    messages: (id: string) => apiJson<ApiMessage[]>(`/api/v1/conversations/${id}/messages`)
  },
  profile: {
    get: () => apiJson<BuyerProfile>("/api/v1/profile"),
    replace: (profile: ProfileRequest) => apiJson<BuyerProfile>("/api/v1/profile", { method: "PUT", body: profile }),
    clear: () => apiJson<void>("/api/v1/profile", { method: "DELETE" })
  },
  shortlist: {
    list: () => apiJson<ShortlistItem[]>("/api/v1/shortlist"),
    add: (variantId: string) => apiJson<ShortlistItem>("/api/v1/shortlist", { method: "POST", body: { variantId } }),
    remove: (id: string) => apiJson<void>(`/api/v1/shortlist/${id}`, { method: "DELETE" })
  }
};

/** Trimite întrebarea și întoarce răspunsul brut `text/event-stream`, de citit cu `readSse`. */
export function openMessageStream(conversationId: string, content: string, signal: AbortSignal): Promise<Response> {
  return apiRequest(`/api/v1/conversations/${conversationId}/messages`, {
    method: "POST",
    body: { content },
    signal,
    accept: "text/event-stream, application/problem+json"
  });
}
