"use client";

/**
 * Exemplele de întrebări din ghid, grupate pe etapele călătoriei; un click pune textul în câmpul de scris.
 *
 * DE CE prin store (`pendingPrompt`): ghidul e un dialog, iar ciorna stă în `Chat`; store-ul e puntea dintre ele, fără ca
 * dialogul să știe nimic despre conversație. Dialogul se închide ca utilizatorul să vadă câmpul completat.
 */
import { Button } from "@/components/ui/button";
import { EXAMPLE_PROMPTS, type GuideBlock } from "@/lib/guide";
import { JOURNEY_STAGES } from "@/lib/journey";
import { useAppStore } from "@/store/useAppStore";

export function GuideBlockPrompts({ block }: { block: GuideBlock }) {
  const setPendingPrompt = useAppStore(state => state.setPendingPrompt);
  const setGuideOpen = useAppStore(state => state.setGuideOpen);

  function pick(prompt: string) {
    setPendingPrompt(prompt);
    setGuideOpen(false);
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-col gap-1">
        <h3 className="text-sm font-semibold">{block.title}</h3>
        <p className="text-sm text-muted-foreground">{block.body}</p>
      </div>
      {JOURNEY_STAGES.map(stage => {
        const prompts = EXAMPLE_PROMPTS.filter(prompt => prompt.stage === stage.id);
        if (prompts.length === 0) return null;
        return (
          <div key={stage.id} className="flex flex-col gap-1.5">
            <span className="text-xs font-medium text-muted-foreground">Etapa: {stage.label}</span>
            <div className="flex flex-wrap gap-2">
              {prompts.map(prompt => (
                <Button key={prompt.id} variant="outline" size="sm" onClick={() => pick(prompt.prompt)}>
                  <prompt.icon />
                  {prompt.label}
                </Button>
              ))}
            </div>
          </div>
        );
      })}
    </div>
  );
}
