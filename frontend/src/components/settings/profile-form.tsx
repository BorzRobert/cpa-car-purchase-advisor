"use client";

/**
 * Preferințe → Nevoile tale: încarcă profilul din backend și arată formularul când datele sunt gata.
 *
 * DE CE încărcarea e separată de formular: formularul își pornește ciorna din profil la montare (nu sincronizează cu un
 * efect, ca să nu suprascrie ce tocmai a tastat utilizatorul), deci trebuie montat abia după ce profilul există.
 */
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { BuyerNeedsForm } from "@/components/settings/buyer-needs-form";
import { useProfile } from "@/hooks/use-profile";

export function ProfileForm() {
  const profile = useProfile();

  if (profile.isError) {
    return (
      <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
        <p>Nu am putut încărca profilul.</p>
        <Button variant="outline" size="sm" onClick={() => void profile.refetch()}>
          Reîncearcă
        </Button>
      </div>
    );
  }

  if (!profile.data) {
    return (
      <div className="flex flex-col gap-4" aria-hidden>
        <Skeleton className="h-9 w-64" />
        <Skeleton className="h-9 w-full" />
        <Skeleton className="h-9 w-full" />
        <Skeleton className="h-24 w-full" />
      </div>
    );
  }

  return <BuyerNeedsForm profile={profile.data} />;
}
