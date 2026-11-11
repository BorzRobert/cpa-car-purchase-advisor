"use client";

/**
 * Rândul de stare a backend-ului din sidebar.
 *
 * DE CE există: este dovada vizibilă că frontend-ul vorbește cu backend-ul public (CORS + HTTPS cap-coadă) și locul
 * unde utilizatorul vede „Se trezește consilierul…" în timpul cold start-ului, în loc de o cerere eșuată.
 *
 * DE CE și un toast cât timp se trezește: pe telefon sidebar-ul e închis la încărcare, deci rândul de aici nu s-ar
 * vedea tocmai când așteptarea e cea mai lungă.
 *
 * DE CE registru și nu lanț de `if`: fiecare stare e o intrare de date (text, icon, culoare); o stare nouă nu atinge JSX-ul.
 */

import { useEffect, useRef } from "react";
import { CircleAlert, CircleCheck, CircleDashed, Loader2, PlugZap, type LucideIcon } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { SidebarMenu, SidebarMenuItem } from "@/components/ui/sidebar";
import { useBackendStatus, type BackendStatus as Status } from "@/hooks/use-backend-status";

type View = {
  icon: LucideIcon;
  iconClassName: string;
  label: (status: Status) => string;
  hint?: (status: Status) => string | null;
};

const VIEWS: Record<Status["phase"], View> = {
  "not-configured": {
    icon: PlugZap,
    iconClassName: "text-muted-foreground",
    label: () => "Backend neconfigurat",
    hint: () => "Setează NEXT_PUBLIC_API_BASE_URL."
  },
  checking: {
    icon: CircleDashed,
    iconClassName: "text-muted-foreground",
    label: () => "Se verifică backend-ul…"
  },
  waking: {
    icon: Loader2,
    iconClassName: "animate-spin text-amber-500",
    label: () => "Se trezește consilierul…",
    hint: () => "Prima pornire poate dura până la un minut."
  },
  ready: {
    icon: CircleCheck,
    iconClassName: "text-emerald-500",
    label: status => (status.phase === "ready" ? `Backend conectat · v${status.health.version}` : ""),
    hint: status =>
      status.phase === "ready" && !status.health.advisorConfigured
        ? "Consilierul nu e activat încă pe acest server (fără cheie)."
        : null
  },
  unreachable: {
    icon: CircleAlert,
    iconClassName: "text-destructive",
    label: () => "Consilierul nu răspunde",
    hint: () => "Serverul nu a pornit la timp sau adresa e greșită."
  }
};

const WAKING_TOAST_ID = "backend-waking";

export function BackendStatus() {
  const { status, retry } = useBackendStatus();
  const view = VIEWS[status.phase];
  const wasWaking = useRef(false);

  useEffect(() => {
    if (status.phase === "waking") {
      wasWaking.current = true;
      toast.loading("Se trezește consilierul…", { id: WAKING_TOAST_ID });
      return;
    }
    toast.dismiss(WAKING_TOAST_ID);
    if (status.phase === "ready" && wasWaking.current) toast.success("Consilierul este gata.");
    wasWaking.current = false;
  }, [status.phase]);

  const hint = view.hint?.(status);

  return (
    <SidebarMenu>
      <SidebarMenuItem>
        <div role="status" aria-live="polite" className="flex items-start gap-2 px-2 py-1.5 text-xs">
          <view.icon className={`mt-0.5 size-3.5 shrink-0 ${view.iconClassName}`} aria-hidden="true" />
          <div className="flex min-w-0 flex-col gap-0.5">
            <span className="truncate font-medium">{view.label(status)}</span>
            {hint && <span className="text-muted-foreground">{hint}</span>}
            {status.phase === "unreachable" && (
              <Button variant="outline" size="xs" className="mt-1 w-fit" onClick={retry}>
                Reîncearcă
              </Button>
            )}
          </div>
        </div>
      </SidebarMenuItem>
    </SidebarMenu>
  );
}
