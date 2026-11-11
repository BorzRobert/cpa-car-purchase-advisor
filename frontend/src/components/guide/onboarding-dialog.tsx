"use client";

/**
 * Dialogul de bun venit, arătat o singură dată la prima vizită.
 *
 * DE CE așteaptă rehidratarea store-ului: `cpa-app` se citește din `localStorage` abia după montare (vezi `useAppStore`);
 * înainte de asta `onboardingSeen` e `false` și dialogul ar clipi la fiecare vizită, chiar și la cei care l-au văzut deja.
 * DE CE orice închidere (Sari peste, Escape, click în afară) îl marchează ca văzut: un dialog care revine la fiecare
 * vizită după ce l-ai închis e o piedică, nu un ajutor. Ghidul complet rămâne în sidebar și pe tasta `?`.
 */
import { useEffect, useState, useSyncExternalStore } from "react";
import { ArrowLeft, ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle
} from "@/components/ui/dialog";
import { ONBOARDING_STEPS } from "@/lib/guide";
import { cn } from "@/lib/utils";
import { useAppStore } from "@/store/useAppStore";

function useStoreHydrated(): boolean {
  return useSyncExternalStore(
    onChange => useAppStore.persist.onFinishHydration(onChange),
    () => useAppStore.persist.hasHydrated(),
    () => false
  );
}

export function OnboardingDialog() {
  const hydrated = useStoreHydrated();
  const seen = useAppStore(state => state.onboardingSeen);
  const setSeen = useAppStore(state => state.setOnboardingSeen);
  const setGuideOpen = useAppStore(state => state.setGuideOpen);
  const [stepIndex, setStepIndex] = useState(0);

  // Dacă nicio cerere nu a pornit încă rehidratarea (ex. backend neconfigurat), o pornim noi.
  useEffect(() => {
    if (!useAppStore.persist.hasHydrated()) void useAppStore.persist.rehydrate();
  }, []);

  const step = ONBOARDING_STEPS[stepIndex];
  const last = stepIndex === ONBOARDING_STEPS.length - 1;

  return (
    <Dialog open={hydrated && !seen} onOpenChange={open => !open && setSeen(true)}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div className="mb-1 grid size-10 place-items-center rounded-md bg-primary text-primary-foreground">
            <step.icon className="size-5" aria-hidden="true" />
          </div>
          <DialogTitle>{step.title}</DialogTitle>
          <DialogDescription>{step.body}</DialogDescription>
        </DialogHeader>

        <ol
          className="flex justify-center gap-1.5"
          aria-label={`Pasul ${stepIndex + 1} din ${ONBOARDING_STEPS.length}`}
        >
          {ONBOARDING_STEPS.map((item, index) => (
            <li
              key={item.id}
              className={cn("h-1.5 w-6 rounded-full bg-muted", index === stepIndex && "bg-primary")}
              aria-hidden="true"
            />
          ))}
        </ol>

        <DialogFooter className="sm:justify-between">
          <Button variant="ghost" onClick={() => setSeen(true)}>
            Sari peste
          </Button>
          <div className="flex gap-2">
            {stepIndex > 0 && (
              <Button variant="outline" onClick={() => setStepIndex(stepIndex - 1)}>
                <ArrowLeft />
                Înapoi
              </Button>
            )}
            {last ? (
              <>
                <Button
                  variant="outline"
                  onClick={() => {
                    setSeen(true);
                    setGuideOpen(true);
                  }}
                >
                  Vezi ghidul
                </Button>
                <Button onClick={() => setSeen(true)}>Începe</Button>
              </>
            ) : (
              <Button onClick={() => setStepIndex(stepIndex + 1)}>
                Înainte
                <ArrowRight />
              </Button>
            )}
          </div>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
