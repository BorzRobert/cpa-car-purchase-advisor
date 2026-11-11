"use client";

/**
 * Preferințe → Providere: ce model de limbaj folosește agentul.
 *
 * DE CE secțiunea rămâne generată din registru: preferințele pot afișa aceeași informație ca
 * selectorul de moment, fără o a doua listă care s-ar desincroniza la următorul provider.
 *
 * DE CE nu apare nicio cheie de API în acest ecran:
 * cheia stă pe server, în `.env.local`. Un câmp „API key" în interfață ar însemna că cheia
 * trece prin browser — adică exact greșeala pe care aplicația e construită să o evite. Aici
 * se alege DOAR ce model se folosește; cu ce cheie se apelează e treaba serverului.
 */

import { Check } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { PROVIDER_LIST } from "@/lib/providers";
import { useAppStore } from "@/store/useAppStore";
import { cn } from "@/lib/utils";

export function ProvidersForm() {
  const providerId = useAppStore(state => state.providerId);
  const modelId = useAppStore(state => state.modelId);

  return (
    <div className="flex flex-col gap-6">
      {/*
        Lista se construiește iterând registrul din `src/lib/providers.ts`, astfel încât un provider
        nou să apară fără cod nou în preferințe.
      */}
      {PROVIDER_LIST.map((provider, index) => (
        <div key={provider.id} className="flex flex-col gap-3">
          {index > 0 && <Separator />}

          <div className="flex items-start justify-between gap-4">
            <div className="flex flex-col gap-1">
              <div className="flex items-center gap-2">
                <span className="text-sm font-medium">{provider.label}</span>
                {provider.id === providerId && <Badge variant="secondary">implicit în composer</Badge>}
              </div>
              <p className="text-xs text-muted-foreground">{provider.description}</p>
            </div>
          </div>

          {provider.models.length > 0 && (
            <div className="flex flex-col gap-1">
              {provider.models.map(model => {
                const isActive = provider.id === providerId && model.id === modelId;
                return (
                  <div
                    key={model.id}
                    className={cn(
                      "flex items-center justify-between gap-2 rounded-md px-2 py-1.5",
                      isActive && "bg-accent"
                    )}
                  >
                    <span>{model.label}</span>
                    {/* Id-ul real e afișat intenționat: e valoarea care va ajunge în cererea de la Faza 3. */}
                    <span className="flex items-center gap-2 font-mono text-xs text-muted-foreground">
                      {model.id}
                      {isActive && <Check className="size-3.5" />}
                    </span>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      ))}

      <p className="border-t pt-4 text-xs text-muted-foreground">
        Alegerea pentru mesajul următor se face în composer; această secțiune păstrează doar vizibilă configurația
        disponibilă în registru.
      </p>
    </div>
  );
}
