/**
 * Inelul de scor (0–100) al unei variante, desenat în SVG.
 *
 * DE CE SVG și nu o bibliotecă de grafice: e un singur cerc cu un arc; nu merită o dependență. Cifra din mijloc și
 * `aria-label` spun același lucru ca arcul, deci informația nu depinde de culoare.
 */
import { cn } from "@/lib/utils";

type ScoreRingProps = { score: number; size?: number; className?: string };

const STROKE = 5;

export function ScoreRing({ score, size = 52, className }: ScoreRingProps) {
  const clamped = Math.min(100, Math.max(0, score));
  const radius = (size - STROKE) / 2;
  const circumference = 2 * Math.PI * radius;

  return (
    <div
      role="img"
      aria-label={`Scor ${Math.round(clamped)} din 100`}
      className={cn("relative grid shrink-0 place-items-center", className)}
      style={{ width: size, height: size }}
    >
      <svg width={size} height={size} className="-rotate-90" aria-hidden="true">
        <circle cx={size / 2} cy={size / 2} r={radius} fill="none" strokeWidth={STROKE} className="stroke-muted" />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          strokeWidth={STROKE}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={circumference * (1 - clamped / 100)}
          className="stroke-signal transition-[stroke-dashoffset] duration-500 motion-reduce:transition-none"
        />
      </svg>
      <span className="absolute font-mono text-sm font-semibold tabular-nums">{Math.round(clamped)}</span>
    </div>
  );
}
