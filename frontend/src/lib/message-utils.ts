/**
 * Transformările mesajelor, ca funcții pure, fără dependențe de browser sau de React.
 *
 * DE CE aici: mesajul din interfață (`ChatMessage`) are două surse — istoricul din backend (doar text) și răspunsul
 * aflat în curs de streaming (text + unelte + carduri). Ambele ajung la aceeași formă prin funcțiile de mai jos, iar
 * faptul că sunt pure le face testabile fără rețea.
 */
import type { AdvisorEvent, ComparisonReport, VehicleCard } from "@/lib/advisor-events";
import type { ApiMessage } from "@/lib/api/endpoints";

export type ToolActivity = { name: string; status: "started" | "finished"; summary: string };

export type ChatMessage = {
  id: string;
  role: "user" | "assistant";
  content: string;
  tools: ToolActivity[];
  cards: VehicleCard[];
  comparison: ComparisonReport | null;
};

export function newMessage(id: string, role: ChatMessage["role"], content = ""): ChatMessage {
  return { id, role, content, tools: [], cards: [], comparison: null };
}

export function fromApiMessage(message: ApiMessage): ChatMessage {
  return newMessage(message.id ?? crypto.randomUUID(), message.role === "USER" ? "user" : "assistant", message.content);
}

/** Aplică un eveniment al fluxului pe răspunsul consilierului aflat în curs. Evenimentele fără efect vizual îl lasă neschimbat. */
export function applyAdvisorEvent(message: ChatMessage, event: AdvisorEvent): ChatMessage {
  switch (event.type) {
    case "meta":
      return { ...message, id: event.messageId };
    case "token":
      // Textul vine în bucăți; se concatenează, nu se înlocuiește.
      return { ...message, content: message.content + event.delta };
    case "tool":
      return { ...message, tools: updateTools(message.tools, event) };
    case "vehicles": {
      const known = new Set(message.cards.map(card => card.variantId));
      return { ...message, cards: [...message.cards, ...event.cards.filter(card => !known.has(card.variantId))] };
    }
    case "compare":
      return { ...message, comparison: event.report };
    default:
      return message;
  }
}

function updateTools(tools: ToolActivity[], event: Extract<AdvisorEvent, { type: "tool" }>): ToolActivity[] {
  const activity: ToolActivity = { name: event.name, status: event.status, summary: event.summary };
  if (event.status === "started") return [...tools, activity];

  // `finished` încheie ultimul apel pornit cu același nume (aceeași unealtă poate fi chemată de mai multe ori).
  const index = tools.findLastIndex(tool => tool.name === event.name && tool.status === "started");
  if (index === -1) return [...tools, activity];
  return tools.map((tool, i) => (i === index ? activity : tool));
}
