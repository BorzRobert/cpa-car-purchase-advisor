# Frontend — arhitectură

> Stadiu: **final (Faza 9)** — frontend-ul rulează în producție pe Vercel și vorbește cu backend-ul de pe Render. Sursa de adevăr pentru ce construim rămâne [`../requirements.md`](../requirements.md); contractul cu backend-ul este în [`communication.md`](communication.md).

## Rolul frontend-ului

Frontend-ul (`frontend/`, Next.js 16 + TypeScript + Tailwind v4 + shadcn/ui) este **doar interfață**: colectează input și randează. Nu are Route Handlers, nu apelează niciun model și nu are secrete; singura variabilă de mediu este `NEXT_PUBLIC_API_BASE_URL` (publică prin definiție). Promptul, uneltele, scorurile și calculele TCO sunt ale backend-ului.

## Structura

| Folder                       | Conține                                                                                       |
| ---------------------------- | --------------------------------------------------------------------------------------------- |
| `src/app/`                   | doar pagina (`page.tsx` compune zonele) și `layout.tsx` (fonturi, teme, provideri)           |
| `src/components/chat/`       | conversația: listă de mesaje, câmp de scris, card de vehicul, tabel de comparație, TCO, chip-uri de activitate |
| `src/components/panel/`      | panoul lateral: stepper, „Nevoile tale” cu cursor de buget, lista scurtă, costul total curent |
| `src/components/guide/`      | ghidul (dialog cu file) și dialogul de bun venit                                              |
| `src/components/help/`       | ajutor contextual (termeni de glosar cu tooltip)                                              |
| `src/components/settings/`   | Preferințe: aspect, formularul de nevoi, lista scurtă                                         |
| `src/components/ui/`         | componentele shadcn generate (nu se editează de mână; excepții documentate în `AGENTS.md`)   |
| `src/lib/`                   | logică pură și testată (registre, transformări), clientul API (`lib/api/`), parserul SSE      |
| `src/hooks/`, `src/store/`   | TanStack Query (date din backend), `useAdvisorChat`, store-ul `cpa-app`                       |

Regula „registre, nu lanțuri de `if`”: listele care cresc sunt date. Exemple: `lib/guide.ts` (conținutul ghidului și exemplele de întrebări), `lib/glossary.ts`, `lib/journey.ts` (etapele), `lib/profile-chips.ts`, `lib/vehicle-badges.ts`.

## Design

Concept: „tablou de bord / showroom noaptea”. Tokenii stau în `src/app/globals.css`, pe `:root` (varianta caldă, deschisă) și `.dark` (grafit), plus `@theme inline` și `@custom-variant dark`; nu există CSS scris de mână pentru componente.

- **Culori:** grafit cald; chihlimbar (`--primary`) pentru acțiuni; turcoaz (`--signal`, singurul token adăugat peste setul shadcn) pentru scoruri, bare și stări reușite. Chihlimbarul nu se folosește ca text pe fundal deschis (contrast insuficient).
- **Formă:** raza de bază `0.375rem`.
- **Tipografie:** Geist pentru text, **Space Grotesk** (`--font-display`, cu `latin-ext` pentru diacritice) pentru titlurile `h2`/`h3`, Geist Mono cu `tabular-nums` pentru prețuri și cifre. Componenta `Price` afișează mereu „orientativ · date la <data>” lângă sumă.
- **Tema** se schimbă doar din Preferințe → General → Aspect (`ThemeProvider`, `localStorage["cpa-theme"]`); header-ul nu are comutator de temă.

## Aspectul în trei zone

1. **Navigația subțire** (sidebar shadcn, 15 rem): conversații, „Ghid”, Preferințe, starea backend-ului.
2. **Conversația:** mesaje (`role="log"`, `aria-live="polite"`, `aria-busy` cât curge răspunsul), câmpul de scris, ecranul de start cu sugestii.
3. **Panoul lateral viu**, de la 1280 px lățime, ca o coloană; sub 1280 px devine o foaie de jos deschisă din butonul din header (`PanelTrigger`). Se randează o singură variantă la un moment dat (`useIsWideLayout`).

Ce arată panoul și de unde își ia datele:

| Secțiune                | Sursa                                                                                                  |
| ----------------------- | ------------------------------------------------------------------------------------------------------ |
| Stepper (Nevoi → Căutare → Comparație → Listă scurtă) | dedus din date (`lib/journey.ts`): profil completat, carduri primite, comparație primită, listă scurtă nevidă |
| Nevoile tale            | profilul din cache-ul TanStack Query; evenimentul SSE `profile` îl actualizează direct (`lib/profile-patch.ts`), apoi se reia din backend |
| Cursor de buget         | `PUT /profile` cu tot profilul, la eliberarea cursorului; intervale diferite pentru „total” și „pe lună” (`lib/budget.ts`) |
| Lista scurtă            | `GET /shortlist`, reîncărcat când unealta `addToShortlist` se termină                                  |
| Cost total curent       | ultima comparație (`compare`) din conversație: varianta de pe primul loc, segmentele din `lib/tco-breakdown.ts` |

## Datele din fluxul SSE

`useAdvisorChat` ține răspunsul care curge (text, unelte, carduri, clasament); după `done` istoricul se reia din backend. Backend-ul salvează **doar textul**, deci cardurile și clasamentul **dispar la reîncărcarea paginii** — comportament documentat în [`communication.md`](communication.md); persistarea lor ar fi o schimbare de backend (neplanificată). Panoul lateral citește „ultima comparație” din mesajele de pe ecran.

## Ghidul din aplicație (Faza 8)

- **Registrul** `lib/guide.ts` descrie secțiunile ghidului (blocuri de tip `text`, `prompts`, `glossary`, `delete-data`), exemplele de întrebări (cu etapa lor) și pașii dialogului de bun venit. Tipul blocului alege componenta printr-un registru în `guide-dialog.tsx`.
- **Puncte de intrare:** „Ghid” din sidebar, tasta `?` (ignorată în câmpuri de text și când e deschis alt dialog) și dialogul de bun venit la prima vizită.
- **Aspectul dialogului:** cele șapte file nu încap pe un rând, deci de la 768 px stau într-o coloană verticală lângă conținut (`Tabs` cu `orientation="vertical"`), iar pe telefon într-o grilă cu două coloane, ca să se vadă toate fără derulare orizontală. Doar conținutul se derulează.
- **Dialogul de bun venit** apare o singură dată; marcajul `onboardingSeen` stă în `cpa-app` și se citește abia după rehidratarea store-ului. Orice închidere îl marchează ca văzut.
- **Exemple care completează câmpul de scris:** ghidul pune textul în `pendingPrompt` din store, iar `Chat` îl preia (nu se trimite singur).
- **Glosar:** TCO, DAE (APR), depreciere, valoare reziduală — un singur registru, afișat ca tooltip (`GlossaryTerm`) și ca tab.
- **„Șterge-mi datele”:** `DELETE /api/v1/sessions/current`, apoi tokenul se uită și o sesiune nouă se creează la prima interogare.

## Starea

- Date de pe server: TanStack Query (conversații, mesaje, profil, listă scurtă).
- Store-ul Zustand `cpa-app` (versiunea **9**): persistat doar `sessionToken` și `onboardingSeen`; efemer: conversația activă, dialogurile, panoul, `pendingPrompt`. Migrarea din v8 păstrează tokenul (`migrateAppState`, testat). Orice schimbare a formei persistate cere creșterea `version` și un `migrate`.
- Tema nu este în store.

## Accesibilitate

Țintă: Lighthouse accesibilitate ≥ 90. Practici: navigare completă de la tastatură (dialoguri cu focus prins, file cu săgeți și Enter, butoane etichetate), `role="log"` pentru conversație, `aria-label` pe inelul de scor și pe bara TCO (informația nu depinde de culoare), bare de scor cu cifra alăturată, `prefers-reduced-motion` respectat (animații cu `motion-reduce:`, derulare fără animație), un singur `h1` pe pagină (titlul conversației), tabele cu `th scope`.

## Teste și verificări

- `npm test` (Vitest, doar logică pură): parserul SSE, transformările mesajelor, patch-ul de profil, chip-urile, etapele, intervalele de buget, segmentele TCO, insignele, migrarea store-ului.
- `npm run build`, `npm run lint`, `npx prettier --end-of-line auto --check .` (pe Windows cu `autocrlf`).
- Verificare manuală cu backend-ul local (`docker compose up --build`) și un server de test care simulează SSE-ul când nu există cheie OpenAI: evenimentele `vehicles`, `compare`, `tool`, `token`, flux incomplet și starea `503 advisor-not-configured` (cea din urmă vine de la backend-ul real).
- Verificări de accesibilitate: axe-core pe ecranul de start, conversație cu comparație, ghid și foaia mobilă (temă închisă și deschisă); Lighthouse pe build-ul de producție; fără derulare orizontală la 360 și 390 px.

## Producție (Faza 9)

- **Gazdă:** Vercel, directorul rădăcină `frontend`; singura variabilă este `NEXT_PUBLIC_API_BASE_URL` (URL-ul public al backend-ului, fără `/` final), copiată în build — orice schimbare cere Redeploy. Pașii: [`../vercel/README.md`](../vercel/README.md). URL-urile publice sunt în [`../../README.md`](../../README.md).
- **CORS:** backend-ul acceptă doar originea Vercel de producție; de pe `localhost` cererile către backend-ul de producție sunt refuzate, deci dezvoltarea locală folosește backend-ul local (`docker compose up --build`).
- **Cold start:** la încărcare se trimite un ping la `/actuator/health`; cât backend-ul doarme, sidebar-ul arată „Se trezește consilierul…” și reîncearcă ~2,5 minute. Durata reală a trezirii nu a fost încă măsurată (vezi [`../render/README.md`](../render/README.md#observat-pe-render)).
- **Buget:** o sesiune are 60.000 de tokeni pe zi; un scenariu complet (profil, clasament, TCO, listă scurtă, finanțare) îl consumă aproape integral. Când se epuizează, UI-ul arată starea `budget-exceeded`; „Șterge-mi datele” pornește o sesiune nouă. Scenariul de demo verificat: [`../demo-prompt.txt`](../demo-prompt.txt).
- **Audit de dependențe (`knip`, `depcheck`):** niciun fișier orfan și nicio dependență neutilizată. `knip` raportează doar exporturi nefolosite din `src/components/ui/` (componentele shadcn generate, care nu se editează de mână); `depcheck` raportează fals `shadcn`, `tw-animate-css`, `tailwindcss`, `@tailwindcss/postcss`, `prettier-plugin-tailwindcss` și `@types/react-dom`, folosite din `globals.css`, `postcss.config.mjs`, `.prettierrc` și tipurile ambientale. Nu sunt instrumente adăugate în proiect (rulate cu `npx`).
