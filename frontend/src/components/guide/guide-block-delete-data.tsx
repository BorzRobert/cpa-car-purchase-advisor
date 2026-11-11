"use client";

/**
 * „Șterge-mi datele”: șterge sesiunea anonimă de pe server (profil, conversații, listă scurtă), apoi pornește una nouă.
 *
 * DE CE confirmare: ștergerea nu se poate anula. DE CE `forgetSession` după apel: tokenul vechi nu mai funcționează, deci
 * aplicația îl uită și își cere o sesiune nouă la prima interogare, ca utilizatorul să poată continua fără să reîncarce pagina.
 */
import { useState } from "react";
import { Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api/endpoints";
import { describeError } from "@/lib/api/errors";
import { forgetSession } from "@/lib/api/session";
import type { GuideBlock } from "@/lib/guide";
import { useAppStore } from "@/store/useAppStore";

export function GuideBlockDeleteData({ block }: { block: GuideBlock }) {
  const [pending, setPending] = useState(false);
  const setGuideOpen = useAppStore(state => state.setGuideOpen);

  async function handleDelete() {
    if (!window.confirm("Ștergi definitiv profilul, conversațiile și lista scurtă? Nu se poate anula.")) return;
    setPending(true);
    try {
      await api.session.deleteCurrent();
      await forgetSession();
      setGuideOpen(false);
      toast.success("Datele tale au fost șterse. Începi cu o sesiune nouă.");
    } catch (error) {
      toast.error(describeError(error).title);
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="flex flex-col items-start gap-2 rounded-md border border-destructive/30 p-3">
      <h3 className="text-sm font-semibold">{block.title}</h3>
      <p className="text-sm text-muted-foreground">{block.body}</p>
      <Button variant="destructive" size="sm" onClick={() => void handleDelete()} disabled={pending}>
        <Trash2 />
        Șterge-mi datele
      </Button>
    </div>
  );
}
