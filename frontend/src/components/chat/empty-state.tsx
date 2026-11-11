"use client";

/**
 * Ecranul de conversație nouă: numele aplicației, salutul și sugestiile.
 *
 * DE CE sugestiile doar pre-completează inputul, fără să trimită: utilizatorul trebuie să poată ajusta întrebarea
 * înainte de trimitere, iar o sugestie care trimite direct ar consuma din bugetul zilnic pe o întrebare neintenționată.
 *
 * DE CE sugestiile sunt un registru (date), nu patru butoane scrise de mână: `prompt` stă lângă `label`, deci nu poți
 * schimba eticheta și uita textul. Primele trei sunt cele trei scenarii de acceptare ale produsului.
 */

import { Calculator, ClipboardCheck, Scale, Zap } from "lucide-react";
import { Button } from "@/components/ui/button";

const SUGGESTIONS = [
  {
    label: "Benzină, hibrid sau electric?",
    icon: Zap,
    prompt:
      "Am 20.000 € și fac aproximativ 15.000 km pe an, mai ales în oraș. Nu pot încărca acasă. Ce motorizare mi se potrivește: benzină, hibrid sau electric?"
  },
  {
    label: "Compară trei modele",
    icon: Scale,
    prompt: "Compară un Golf, un Corolla și un Octavia pentru o familie de patru persoane, pe 5 ani."
  },
  {
    label: "Verifică un anunț",
    icon: ClipboardCheck,
    prompt: "Vreau să verifici un anunț. Îl lipesc mai jos și îmi spui dacă e o ofertă bună:\n\n"
  },
  {
    label: "Cât costă pe 5 ani?",
    icon: Calculator,
    prompt: "Calculează costul total de deținere pe 5 ani pentru cele mai potrivite mașini pentru profilul meu."
  }
] as const;

type EmptyStateProps = {
  onPickSuggestion: (prompt: string) => void;
  /** Composer-ul, primit ca element: pe ecranul de start el stă între salut și sugestii. */
  composer: React.ReactNode;
};

export function EmptyState({ onPickSuggestion, composer }: EmptyStateProps) {
  return (
    // `justify-center` cu un pic de aer în plus jos: blocul stă puțin peste mijloc, altfel sugestiile ajung prea jos pe ecranele înalte.
    <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col justify-center gap-8 px-4 pb-16">
      <div className="flex flex-col items-center gap-3 text-center">
        <span className="text-sm font-medium tracking-wide text-muted-foreground uppercase">CPA</span>
        <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">Ce mașină căutăm astăzi?</h1>
        <p className="max-w-xl text-sm text-muted-foreground">
          Spune-mi bugetul și cum conduci; compar, calculez costul total și îți explic de ce recomand fiecare variantă.
        </p>
      </div>

      {composer}

      <div className="flex flex-wrap justify-center gap-2">
        {SUGGESTIONS.map(suggestion => (
          <Button
            key={suggestion.label}
            variant="outline"
            size="sm"
            className="text-muted-foreground hover:text-foreground"
            onClick={() => onPickSuggestion(suggestion.prompt)}
          >
            <suggestion.icon />
            {suggestion.label}
          </Button>
        ))}
      </div>
    </div>
  );
}
