"use client";

/**
 * Preferințe → Nevoile tale: formularul care înlocuiește profilul de skill-uri din aplicația veche.
 *
 * DE CE contează: din aceste câmpuri consilierul știe ce să caute și cum să calculeze (buget, kilometri, oraș/autostradă,
 * pasageri, încărcare acasă). Agentul le poate completa și el, în conversație (evenimentul `profile`); formularul este
 * locul unde cumpărătorul le vede și le corectează.
 *
 * DE CE salvăm la buton și nu la fiecare tastă: un profil pe jumătate scris („buget 2” în loc de 20000) ar ajunge în
 * promptul următorului răspuns. Backend-ul primește tot profilul (`PUT`), după validare.
 */

import { useState } from "react";
import { AlertCircle } from "lucide-react";
import { toast } from "sonner";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { FormField as Field } from "@/components/settings/form-field";
import { OptionToggle } from "@/components/settings/option-toggle";
import { useClearProfile, useSaveProfile } from "@/hooks/use-profile";
import { describeError } from "@/lib/api/errors";
import type { BuyerProfile } from "@/lib/api/endpoints";
import { toDraft, toRequest, type ProfileDraft, MAX_ITEM_LENGTH, MAX_LIST_ITEMS } from "@/lib/profile-draft";
import {
  BODY_TYPE_OPTIONS,
  BUDGET_KIND_OPTIONS,
  CONDITION_OPTIONS,
  FINANCING_OPTIONS,
  HOME_CHARGING_OPTIONS,
  POWERTRAIN_OPTIONS
} from "@/lib/profile-options";

export function BuyerNeedsForm({ profile }: { profile: BuyerProfile }) {
  const [draft, setDraft] = useState<ProfileDraft>(() => toDraft(profile));
  const [errors, setErrors] = useState<string[]>([]);
  const save = useSaveProfile();
  const clear = useClearProfile();

  function update<K extends keyof ProfileDraft>(key: K, value: ProfileDraft[K]) {
    setDraft(current => ({ ...current, [key]: value }));
  }

  function handleSave() {
    const { request, errors: found } = toRequest(draft);
    setErrors(found);
    if (found.length > 0) {
      toast.error("Profilul nu a fost salvat: verifică câmpurile marcate.");
      return;
    }
    save.mutate(request, {
      onSuccess: saved => {
        setDraft(toDraft(saved));
        toast.success("Nevoile tale au fost salvate");
      },
      onError: error => toast.error(describeError(error).title)
    });
  }

  function handleClear() {
    if (!window.confirm("Ștergi tot profilul? Consilierul va începe din nou să te întrebe.")) return;
    clear.mutate(undefined, {
      onSuccess: () => {
        setDraft(toDraft({}));
        setErrors([]);
        toast.success("Profil șters");
      },
      onError: error => toast.error(describeError(error).title)
    });
  }

  return (
    <div className="flex flex-col gap-6">
      <Field label="Buget" hint="Suma totală pe care o poți cheltui sau rata lunară la care te simți confortabil.">
        <div className="flex flex-wrap items-center gap-2">
          <Input
            type="number"
            inputMode="decimal"
            min={0}
            value={draft.budgetAmount}
            onChange={event => update("budgetAmount", event.target.value)}
            placeholder="Ex. 20000"
            aria-label="Suma bugetului în euro"
            className="w-36"
          />
          <span className="text-sm text-muted-foreground">€</span>
          <OptionToggle
            label="Tipul bugetului"
            options={BUDGET_KIND_OPTIONS}
            value={draft.budgetKind}
            onChange={value => update("budgetKind", value)}
          />
        </div>
      </Field>

      <Field label="Cum plătești">
        <OptionToggle
          label="Modul de plată"
          options={FINANCING_OPTIONS}
          value={draft.financingMode}
          onChange={value => update("financingMode", value)}
        />
      </Field>

      <div className="grid gap-4 sm:grid-cols-3">
        <Field label="Kilometri pe an">
          <Input
            type="number"
            inputMode="numeric"
            min={0}
            max={200000}
            value={draft.yearlyKm}
            onChange={event => update("yearlyKm", event.target.value)}
            placeholder="Ex. 15000"
          />
        </Field>
        <Field label="Condus în oraș (%)" hint="Restul se consideră autostradă și drum național.">
          <Input
            type="number"
            inputMode="numeric"
            min={0}
            max={100}
            value={draft.cityPercent}
            onChange={event => update("cityPercent", event.target.value)}
            placeholder="Ex. 70"
          />
        </Field>
        <Field label="Pasageri">
          <Input
            type="number"
            inputMode="numeric"
            min={1}
            max={9}
            value={draft.passengers}
            onChange={event => update("passengers", event.target.value)}
            placeholder="Ex. 4"
          />
        </Field>
      </div>

      <Field label="Caroserie">
        <OptionToggle
          multiple
          label="Tipuri de caroserie"
          options={BODY_TYPE_OPTIONS}
          value={draft.bodyTypes}
          onChange={value => update("bodyTypes", value)}
        />
      </Field>

      <Field label="Motorizare">
        <OptionToggle
          multiple
          label="Motorizări"
          options={POWERTRAIN_OPTIONS}
          value={draft.powertrains}
          onChange={value => update("powertrains", value)}
        />
      </Field>

      <Field label="Stare">
        <OptionToggle
          label="Mașină nouă sau second-hand"
          options={CONDITION_OPTIONS}
          value={draft.condition}
          onChange={value => update("condition", value)}
        />
      </Field>

      <Field label="Poți încărca acasă?" hint="Contează pentru electric și hibrid plug-in.">
        <OptionToggle
          label="Încărcare acasă"
          options={HOME_CHARGING_OPTIONS}
          value={draft.homeCharging}
          onChange={value => update("homeCharging", value)}
        />
      </Field>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field
          label="Obligatoriu"
          hint={`Un rând pe dotare (cel mult ${MAX_LIST_ITEMS}, ${MAX_ITEM_LENGTH} de caractere).`}
        >
          <Textarea
            value={draft.mustHaves}
            onChange={event => update("mustHaves", event.target.value)}
            rows={4}
            placeholder={"Cutie automată\nApple CarPlay"}
          />
        </Field>
        <Field label="Eliminatoriu" hint="Ce nu accepți sub nicio formă, un rând pe element.">
          <Textarea
            value={draft.dealBreakers}
            onChange={event => update("dealBreakers", event.target.value)}
            rows={4}
            placeholder={"Fără motor diesel\nFără kilometraj mare"}
          />
        </Field>
      </div>

      {/* Alerta apare doar după o încercare de salvare — nu te ceartă în timp ce scrii. */}
      {errors.length > 0 && (
        <Alert variant="destructive">
          <AlertCircle />
          <AlertTitle>Verifică aceste câmpuri</AlertTitle>
          <AlertDescription>
            <ul className="list-inside list-disc">
              {errors.map(error => (
                <li key={error}>{error}</li>
              ))}
            </ul>
          </AlertDescription>
        </Alert>
      )}

      <div className="flex flex-wrap items-center justify-between gap-2 border-t pt-5">
        <p className="max-w-sm text-xs text-muted-foreground">
          Profilul este salvat în backend, legat de sesiunea ta anonimă; nu conține date personale. Consilierul îl
          primește ca date, nu ca instrucțiuni.
        </p>
        <div className="flex gap-2">
          <Button variant="outline" onClick={handleClear} disabled={clear.isPending}>
            Șterge profilul
          </Button>
          <Button onClick={handleSave} disabled={save.isPending}>
            Salvează
          </Button>
        </div>
      </div>
    </div>
  );
}
