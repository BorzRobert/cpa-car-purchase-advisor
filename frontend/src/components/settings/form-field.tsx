/** Eticheta + câmpul + indiciul unui rând de formular; același aspect în tot formularul de nevoi. */
import { Label } from "@/components/ui/label";

type FormFieldProps = { label: string; hint?: string; children: React.ReactNode };

export function FormField({ label, hint, children }: FormFieldProps) {
  return (
    <div className="flex flex-col gap-2">
      <Label>{label}</Label>
      {children}
      {hint && <p className="text-xs text-muted-foreground">{hint}</p>}
    </div>
  );
}
