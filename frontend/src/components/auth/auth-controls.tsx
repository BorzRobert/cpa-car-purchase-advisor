"use client";

import { useState } from "react";
import { useSession, signIn, signOut } from "next-auth/react";
import { LogIn, LogOut, ShieldCheck } from "lucide-react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { AuthConfiguredOnly } from "@/components/auth/auth-provider";

function GitHubMark() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className="size-5 fill-current">
      <path d="M12 .7a11.3 11.3 0 0 0-3.58 22.02c.57.1.78-.25.78-.55v-2.16c-3.18.69-3.85-1.34-3.85-1.34-.52-1.32-1.27-1.67-1.27-1.67-1.04-.71.08-.7.08-.7 1.15.08 1.76 1.18 1.76 1.18 1.02 1.75 2.67 1.24 3.32.95.1-.74.4-1.24.73-1.52-2.54-.29-5.2-1.27-5.2-5.66 0-1.25.45-2.27 1.18-3.07-.12-.29-.51-1.45.11-3.03 0 0 .96-.31 3.13 1.17a10.9 10.9 0 0 1 5.7 0c2.17-1.48 3.13-1.17 3.13-1.17.62 1.58.23 2.74.11 3.03.73.8 1.18 1.82 1.18 3.07 0 4.4-2.67 5.36-5.21 5.65.41.36.78 1.08.78 2.18v3.23c0 .3.2.66.79.55A11.3 11.3 0 0 0 12 .7Z" />
    </svg>
  );
}

function GoogleMark() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className="size-5">
      <path
        fill="#4285F4"
        d="M21.35 12.27c0-.72-.06-1.25-.2-1.82H12v3.45h5.37a4.58 4.58 0 0 1-1.99 3.01v2.5h3.22c1.89-1.74 2.75-4.3 2.75-7.14Z"
      />
      <path
        fill="#34A853"
        d="M12 21.7c2.7 0 4.96-.89 6.61-2.42l-3.22-2.5c-.89.6-2.02.96-3.39.96-2.6 0-4.8-1.76-5.59-4.12H3.08v2.58A9.99 9.99 0 0 0 12 21.7Z"
      />
      <path
        fill="#FBBC05"
        d="M6.41 13.62A6 6 0 0 1 6.1 12c0-.56.1-1.1.3-1.62V7.8H3.08A9.99 9.99 0 0 0 2 12c0 1.61.39 3.13 1.08 4.2l3.33-2.58Z"
      />
      <path
        fill="#EA4335"
        d="M12 6.26c1.47 0 2.78.5 3.82 1.49l2.87-2.87C16.95 3.27 14.7 2.3 12 2.3a9.99 9.99 0 0 0-8.92 5.5l3.33 2.58C7.2 8.02 9.4 6.26 12 6.26Z"
      />
    </svg>
  );
}

const AUTH_PROVIDERS = [
  { id: "github", label: "GitHub", active: true, Icon: GitHubMark },
  { id: "google", label: "Google", active: false, Icon: GoogleMark }
] as const;

export function LoginDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>Alege cum te conectezi</DialogTitle>
          <DialogDescription>Folosește un cont existent. Nu creăm și nu salvăm parole.</DialogDescription>
        </DialogHeader>
        <div className="grid gap-2">
          {AUTH_PROVIDERS.map(provider => (
            <Button
              key={provider.id}
              variant="outline"
              className="h-12 justify-start gap-3"
              disabled={!provider.active}
              onClick={() => void signIn(provider.id, { callbackUrl: "/" })}
            >
              <provider.Icon />
              <span>{provider.label}</span>
              {!provider.active && <span className="ml-auto text-xs text-muted-foreground">Urmează</span>}
            </Button>
          ))}
        </div>
      </DialogContent>
    </Dialog>
  );
}

export function AuthSidebarUser({ onSettings }: { onSettings: () => void }) {
  return <ConfiguredSidebarUser onSettings={onSettings} />;
}

function ConfiguredSidebarUser({ onSettings }: { onSettings: () => void }) {
  const { data: session, status } = useSession();
  const [loginOpen, setLoginOpen] = useState(false);

  if (status === "loading") return <span className="px-2 text-xs text-muted-foreground">Se verifică sesiunea...</span>;
  if (!session?.user) {
    return (
      <>
        <Button variant="outline" className="w-full justify-start gap-2" onClick={() => setLoginOpen(true)}>
          <LogIn />
          Conectează-te
        </Button>
        <LoginDialog open={loginOpen} onOpenChange={setLoginOpen} />
      </>
    );
  }

  const name = session.user.name || session.user.email || "Cont conectat";
  return (
    <div className="flex items-center gap-2">
      <button className="flex min-w-0 flex-1 items-center gap-2 text-left" onClick={onSettings}>
        <Avatar className="size-7">
          {session.user.image && <AvatarImage src={session.user.image} alt="" />}
          <AvatarFallback className="text-xs">{name.slice(0, 2).toUpperCase()}</AvatarFallback>
        </Avatar>
        <span className="truncate text-sm">{name}</span>
      </button>
      <Button variant="ghost" size="icon" onClick={() => void signOut({ callbackUrl: "/" })} aria-label="Ieși din cont">
        <LogOut />
      </Button>
    </div>
  );
}

export function AuthAccountField() {
  return (
    <AuthConfiguredOnly>
      <ConfiguredAccountField />
    </AuthConfiguredOnly>
  );
}

function ConfiguredAccountField() {
  const { data: session, status } = useSession();
  const email = session?.user?.email;
  const value =
    status === "loading"
      ? "Se verifică sesiunea..."
      : !session?.user
        ? "Nu ești conectat."
        : email || "Furnizorul nu ne-a trimis emailul.";

  return (
    <div className="flex flex-col gap-2">
      <label htmlFor="account-email" className="text-sm font-medium">
        Emailul contului
      </label>
      <input
        id="account-email"
        value={value}
        readOnly
        className="flex h-9 w-full rounded-md border border-input bg-muted/40 px-3 py-1 text-sm text-muted-foreground"
      />
      <p className="text-xs text-muted-foreground">
        Vine de la furnizor și nu poate fi schimbat aici. Nu face parte din profil, nu se salvează local și nu este
        trimis în system prompt.
      </p>
    </div>
  );
}

export function AuthStatusIcon() {
  return <ShieldCheck className="size-4" aria-hidden="true" />;
}
