"use client";

/**
 * Ecranul de conversație nouă: numele aplicației, salutul și sugestiile.
 *
 * DE CE sugestiile doar pre-completează inputul, fără să trimită: utilizatorul trebuie să poată ajusta întrebarea
 * înainte de trimitere, iar o sugestie care trimite direct ar consuma din bugetul zilnic pe o întrebare neintenționată.
 *
 * DE CE sugestiile sunt un registru (date), nu patru butoane scrise de mână: `prompt` stă lângă `label`, deci nu poți
 * schimba eticheta și uita textul. Registrul e `EXAMPLE_PROMPTS` din `lib/guide.ts`, aceeași listă pe care o arată ghidul;
 * primele patru sunt scenariile de acceptare ale produsului, iar fiecare e legată de o etapă a călătoriei de cumpărare.
 */

import { Button } from "@/components/ui/button";
import { EXAMPLE_PROMPTS, START_SCREEN_PROMPT_COUNT } from "@/lib/guide";
import { JOURNEY_STAGES } from "@/lib/journey";

/** Sugestiile de pe ecranul de start sunt primele exemple din registrul ghidului: aceeași sursă, nicio listă duplicată. */
const SUGGESTIONS = EXAMPLE_PROMPTS.slice(0, START_SCREEN_PROMPT_COUNT);

const stageLabel = (stage: string) => JOURNEY_STAGES.find(item => item.id === stage)?.label ?? stage;

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
        <h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">Ce mașină căutăm astăzi?</h2>
        <p className="max-w-xl text-sm text-muted-foreground">
          Spune-mi bugetul și cum conduci; compar, calculez costul total și îți explic de ce recomand fiecare variantă.
        </p>
      </div>

      {composer}

      <div className="flex flex-wrap justify-center gap-2">
        {SUGGESTIONS.map(suggestion => (
          <Button
            key={suggestion.id}
            variant="outline"
            size="sm"
            className="text-muted-foreground hover:text-foreground"
            title={`Etapa: ${stageLabel(suggestion.stage)}`}
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
