"use client";

/**
 * Alerta de eroare a conversației, cu text prietenos pentru fiecare cod de eroare al backend-ului.
 *
 * DE CE stă deasupra conversației și nu în locul ei: o eroare nu trebuie să șteargă de pe ecran ce ai discutat până
 * atunci. DE CE are două tonuri: „consilier neactivat” sau „limită atinsă” sunt situații așteptate, nu defecțiuni, deci
 * nu se afișează în roșu.
 */
import { AlertCircle, Info } from "lucide-react";
import { Alert, AlertAction, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { describeError } from "@/lib/api/errors";

type ChatErrorAlertProps = {
  error: unknown;
  onDismiss?: () => void;
};

export function ChatErrorAlert({ error, onDismiss }: ChatErrorAlertProps) {
  const view = describeError(error);
  const Icon = view.tone === "soft" ? Info : AlertCircle;

  return (
    <div className="mx-auto w-full max-w-3xl px-4 pt-4">
      <Alert variant={view.tone === "soft" ? "default" : "destructive"}>
        <Icon />
        <AlertTitle>{view.title}</AlertTitle>
        <AlertDescription>{view.message}</AlertDescription>
        {onDismiss && (
          <AlertAction>
            <Button variant="ghost" size="sm" onClick={onDismiss}>
              Am înțeles
            </Button>
          </AlertAction>
        )}
      </Alert>
    </div>
  );
}
