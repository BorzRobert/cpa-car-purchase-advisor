/**
 * O bară de scor 0–100 cu cifra alăturată, folosită în tabelul de comparație.
 *
 * DE CE cifra stă lângă bară: bara singură nu spune cât e „72”, iar culoarea nu trebuie să poarte informația. Un criteriu
 * care nu se aplică profilului (`applicable: false`) nu are bară, ca să nu pară un scor prost.
 */
import { Progress } from "@/components/ui/progress";

type ScoreBarProps = { label: string; score: number | null };

export function ScoreBar({ label, score }: ScoreBarProps) {
  if (score === null) return <span className="text-xs text-muted-foreground">nu se aplică</span>;

  return (
    <div className="flex items-center gap-2">
      <Progress
        value={score}
        aria-label={label}
        className="w-16 gap-0 sm:w-24 [&_[data-slot=progress-indicator]]:bg-signal [&_[data-slot=progress-track]]:h-1.5"
      />
      <span className="w-7 text-right font-mono text-xs tabular-nums">{Math.round(score)}</span>
    </div>
  );
}
