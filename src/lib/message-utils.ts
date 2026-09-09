/**
 * Transformările exportului de conversație, fără dependențe de browser sau store.
 *
 * DE CE există o structură intermediară comună: JSON-ul și Markdown-ul trebuie să descrie
 * exact aceleași date. Dacă fiecare format ar extrage separat mesajele, o modificare viitoare
 * ar putea ajunge într-un export și să lipsească din celălalt.
 */
import type { UIMessage } from "ai";
import type { Profile } from "@/lib/types";

export type ExportSnapshot = {
  exportedAt: string;
  profile: Profile;
  messages: Array<{
    id: string;
    role: UIMessage["role"];
    text: string;
  }>;
};

/** Singurul loc care transformă părțile text ale unui mesaj în text exportabil. */
export function getMessageText(message: UIMessage): string {
  return message.parts
    .filter(part => part.type === "text")
    .map(part => part.text)
    .join("");
}

export function buildExportSnapshot(messages: UIMessage[], profile: Profile, exportedAt: string): ExportSnapshot {
  return {
    exportedAt,
    profile,
    messages: messages.map(message => ({
      id: message.id,
      role: message.role,
      text: getMessageText(message)
    }))
  };
}

export function serializeConversationJson(messages: UIMessage[], profile: Profile, exportedAt: string): string {
  return JSON.stringify(buildExportSnapshot(messages, profile, exportedAt), null, 2);
}

export function serializeConversationMarkdown(messages: UIMessage[], profile: Profile, exportedAt: string): string {
  const snapshot = buildExportSnapshot(messages, profile, exportedAt);
  const skills = snapshot.profile.skills.map(skill => `- ${skill.name}: ${skill.level}`).join("\n") || "- Niciun skill";
  const conversation = snapshot.messages
    .map(message => `### ${message.role === "user" ? "Tu" : "SkillForge"}\n\n${message.text}`)
    .join("\n\n");

  return `# Export SkillForge

- Data exportului: ${snapshot.exportedAt}

## Profil

- Nume: ${snapshot.profile.name}
- Stack actual: ${snapshot.profile.stack}
- Obiectiv: ${snapshot.profile.goal}

### Skill-uri

${skills}

## Conversație

${conversation || "Nu există mesaje în această conversație."}
`;
}
