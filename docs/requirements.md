# CPA — Car Purchase Advisor — Cerințe

> Acest fișier este **sursa de adevăr** pentru ce construim. Orice schimbare de direcție se scrie AICI, nu doar în conversație. Planul de execuție pe faze, cu deciziile D1–D9, este în [`plan.md`](plan.md) (în engleză); deciziile au câte un ADR în [`adr/`](adr/).
>
> Ultima actualizare: 2026-11-11 · Faza curentă: **Faza 5 — creierul consilierului** (în lucru, pe branch-ul `cpa/phase-5-advisor-brain`; sub-fazele 5a–5d sunt livrate (Faza 5 completă, cu verificarea modelului real încă de făcut); Faza 6 nu a început). Faza 4 este integrată în `main`; Faza 3 este în producție, cu o singură măsurătoare rămasă: durata cold start-ului, vezi [`render/README.md`](render/README.md#observat-pe-render)

---

## 1. Ce este CPA și pentru cine

**CPA este un consilier personal pentru cumpărarea unei mașini.** O aplicație web în centrul căreia stă un **agent AI** care:

- află, treptat, nevoile reale ale cumpărătorului (buget, kilometri pe an, oraș/autostradă, pasageri, încărcare acasă, nou/second-hand, condiții obligatorii și eliminatorii);
- caută, compară și clasifică mașini dintr-un catalog propriu, cu unelte deterministe (nu din memoria modelului);
- calculează costul total de deținere (TCO), finanțarea și punctul de echilibru EV vs. benzină/diesel;
- explică de ce recomandă o mașină, cu un scor pe criterii;
- ajută la analiza unui anunț, la inspecție și la negociere.

### Personas

- **Primul cumpărător** — nu știe ce clasă sau ce motorizare să caute.
- **Familia** — spațiu, siguranță, cost pe 5 ani.
- **Curiosul de electric** — vrea să afle dacă un EV se plătește pentru kilometrajul și accesul lui la încărcare.
- **Vânătorul de second-hand** — are nevoie de verificarea anunțurilor și a prețurilor.

### Piață, monedă, limbă

România/UE, prețuri în EUR (cu conversie în RON), interfața și `docs/` în română, codul și commit-urile în engleză ([ADR 0001](adr/0001-piata-moneda-limba.md)).

---

## 2. Arhitectură

- **`frontend/`** — Next.js 16 + React 19 + TypeScript + Tailwind v4 + shadcn/ui, folosit **doar ca UI**: fără Route Handlers, fără chei, doar `NEXT_PUBLIC_API_BASE_URL`.
- **`backend/`** — Java 25 + Spring Boot 4.1.2 (Maven Wrapper), Spring AI 2.0.2 cu un singur model OpenAI, JPA + Flyway, PostgreSQL în producție și H2 local. Deține toată logica de produs: persona, uneltele, scoring-ul, TCO, memoria, limitele de rată și bugetul, persistența. Aplicația pornește și fără `OPENAI_API_KEY` (consilierul răspunde `503 advisor-not-configured`).
- **Comunicare:** REST/JSON sub `/api/v1` și un endpoint SSE pentru răspunsuri în streaming; identitate prin token de sesiune anonim (`Authorization: Bearer`); contract definit prima dată (OpenAPI), tipurile TypeScript se generează din el.
- **Hosting:** frontend pe Vercel, backend pe Render (Docker, plan Free), bază de date pe Neon (Free). Backend-ul rulează în producție cu profilul Spring `prod` (baza din mediu, Swagger/OpenAPI oprite, pool reglat pentru Neon); migrările Flyway rulează la pornire pe H2 și pe PostgreSQL. CORS: listă explicită de origini (producția Vercel + aliasul stabil al branch-ului testat), fără `*` — vezi [`vercel/README.md`](vercel/README.md).

Detaliile (diagrame, evenimente SSE, straturi) sunt în `plan.md` §2; backend-ul așa cum este construit acum e descris în [`architecture/backend.md`](architecture/backend.md), contractul frontend ↔ backend în [`architecture/communication.md`](architecture/communication.md) (cu instantaneul OpenAPI [`architecture/openapi.json`](architecture/openapi.json)), iar integrările în [`render/`](render/README.md), [`neon/`](neon/README.md) și [`vercel/`](vercel/README.md); restul ajunge în `docs/architecture/` pe măsură ce fazele se livrează.

**Livrat în Faza 4 (REST fără model):** sesiuni anonime (`/sessions`, token hash-uit), profil, conversații și mesaje, lista scurtă, catalogul de vehicule (14 modele / 21 de variante, cifre **orientative și neverificate**, „date la” 2026-11-11 — de revizuit și extins înainte de producție), consum și buget zilnic de tokeni, limite de rată (Bucket4j, per instanță) și de volum.

**Livrat în Faza 5a (streaming LLM):** `POST /conversations/{id}/messages` răspunde cu `text/event-stream` (evenimente `meta`, `token`, `done`, `error`; heartbeat), salvează răspunsul consilierului, înregistrează tokenii reali (sau estimați) și costul fiecărui răspuns în jurnal și în `message.metadata`, verifică bugetul înainte de apelul către model. Promptul de sistem este compus într-un singur loc (`ai.AdvisorPrompt`, versionat), iar profilul și mesajele utilizatorului intră ca date delimitate. Fără cheie OpenAI: `503 advisor-not-configured`. CI rulează cu un model înregistrat, fără cheie. Apelul real către model nu a fost încă verificat cu o cheie reală (pașii: [`openai/README.md`](openai/README.md#verificare)). **Livrat în Faza 5b (unelte):** agentul are cinci unelte deterministe — `updateBuyerProfile`, `searchVehicles`, `compareVehicles`, `addToShortlist`, `getShortlist` — pe care le cheamă prin bucla de unelte Spring AI; fiecare apel apare în UI ca eveniment `tool`, cardurile ca `vehicles`, iar schimbările de profil ca `profile`. Prețurile și specificațiile vin doar din rezultatele uneltelor (cu „date la”); numărul de apeluri dintr-un răspuns este plafonat. **Livrat în Faza 5c (scoring, TCO, finanțare):** clasament pe șase criterii cu ponderi fixe și explicații (`rankVehicles`, `compareVehicles` → evenimentul `compare`), cost total de deținere (`calculateTco`), finanțare cu anuitate (`calculateFinancing`) și punctul de echilibru electric vs. ardere (`evVsIceBreakEven`), toate ca **cod pur** testat cu cifre calculate independent; rezultatele își enumeră ipotezele. Prețurile de energie din calculator (`cpa.pricing.*`) sunt **orientative și neverificate**, ca și catalogul, și trebuie revizuite înainte de producție; „fiabilitatea” este o aproximare (valoarea reziduală), iar must-have-urile din profil nu se potrivesc automat. **Livrat în Faza 5d (garduri, memorie, evaluare):** gard determinist împotriva scurgerii promptului, audit de ancorare al cifrelor și mărcilor din fiecare răspuns (măsurat, nu blocant), prompt de analiză a anunțurilor lipite, memorie cu rezumat rulant (migrarea `V4`) și un set de scenarii „de aur” (cele 4 scenarii de acceptare + scurgere + memorie) rulat în CI cu un model înregistrat; același set rulează manual cu modelul real (`RealModelEvalTest`, oprit implicit). **Criteriul „done when” al Fazei 5 este îndeplinit prin varianta „model înregistrat în CI”; verificarea cu modelul real este încă de făcut** (vezi [`openai/README.md`](openai/README.md#verificare)).

---

## 3. Faze

| Faza | Conținut                                                                                   | Stare       |
| ---- | ------------------------------------------------------------------------------------------ | ----------- |
| 0    | Decizii (ADR D1–D9), unelte, regula de commit `CPA-<titlu>`                                | livrată     |
| 1    | Frontend în `frontend/`, curățenie de bază, documentație de pornire                        | livrată     |
| 2    | Schelet backend Spring Boot (straturi, ProblemDetail, CORS, OpenAPI, ArchUnit, Docker, CI) | livrată     |
| 3    | Deploy „walking skeleton” (Render + Neon + Vercel): stare backend în UI, cold start, Flyway pe Postgres, `render.yaml`, CI frontend | livrată (cold start de măsurat) |
| 4    | Domeniu, persistență, contract REST, sesiuni, limite                                       | livrată     |
| 5    | Creierul consilierului: streaming (5a), unelte (5b), scoring/TCO (5c), guardrails/evaluare (5d) | livrată (verificare cu model real de făcut) |
| 6    | Integrarea frontend-ului, ștergerea codului server vechi (AI SDK, Auth.js, `/api/*`)       | planificată |
| 7    | Redesign și componente specifice (card vehicul, comparație, TCO, panou lateral)            | planificată |
| 8    | Ghid în aplicație                                                                          | planificată |
| 9    | Întărire, producție, documentație finală, audit de curățenie                               | planificată |

### Scenarii de acceptare

1. _„Am 20.000 €, fac ~15.000 km/an mai ales în oraș și nu pot încărca acasă. Benzină, hibrid sau electric?”_ — întreabă doar ce lipsește, apelează uneltele TCO / EV-vs-ICE, cifrele se potrivesc cu calculatorul.
2. _„Compară un Golf, un Corolla și un Octavia pentru o familie de patru persoane pe 5 ani.”_ — widget de comparație cu TCO și scor pe criterii, nu un tabel inventat.
3. _[anunț lipit]_ _„E o ofertă bună?”_ — fapte extrase, semnale de alarmă, întrebări pentru vânzător, listă de inspecție; fără istoric inventat.
4. O cerere în afara subiectului sau o încercare de prompt injection — redirecționare politicoasă, fără scurgeri de reguli.

---

## 4. Cerințe non-funcționale

- **Chei:** `OPENAI_API_KEY` există doar în mediul backend-ului; niciodată în browser, în URL, în loguri sau în repo. Fișierele `.env.example` conțin doar valori-substituent.
- **Un singur model:** configurat într-o singură proprietate de backend (`cpa.llm.model`); fără selector de model în UI sau în cod. Valoarea implicită este `gpt-5.6-terra` ([ADR 0010](adr/0010-model-openai-inlocuitor-gpt-5-mini.md), acceptat la 2026-11-11): `gpt-5-mini` din D7 este deprecat, cu oprire la 2026-12-11. Detalii și prețuri în [`openai/README.md`](openai/README.md).
- **Date și confidențialitate:** fără cont și fără date personale; token de sesiune stocat doar hash-uit (SHA-256); `DELETE /api/v1/sessions/current` („șterge-mi datele”) șterge în cascadă profilul, conversațiile și lista scurtă (contorii de consum rămân anonimi, ca să nu se poată reseta bugetul); jurnalele nu conțin date personale sau tokenuri. Textul din browser este curățat și limitat la intrare.
- **Cost:** limită de rată pe sesiune și IP, buget zilnic de tokeni pe sesiune și global (întrerupătorul global este bugetul global), lungime maximă a mesajului (4000 de caractere), plafoane de volum (conversații, mesaje, lista scurtă) — toate implementate în Faza 4 și testate; din Faza 5a `max_completion_tokens` (`cpa.llm.max-output-tokens`) plafonează un răspuns, iar tokenii reali se înregistrează după fiecare răspuns. Limitele de rată sunt în memorie, per instanță (o singură instanță pe planul Free); verificarea adresei IP în spatele proxy-ului Render rămâne de confirmat în Faza 9.
- **Corectitudine:** prețurile, specificațiile și costurile vin doar din unelte deterministe; răspunsurile le marchează ca orientative, cu data „date la”.
- **Disponibilitate:** planul gratuit adoarme (Render după 15 minute fără trafic, ~1 minut pentru trezire + pornirea JVM; Neon după 5 minute, trezire în sute de ms); interfața pornește un ping la `/actuator/health` la încărcare și afișează „Se trezește consilierul…” în loc de eroare, reîncercând timp de ~2,5 minute. `/actuator/health` nu interoghează baza, ca să nu țină Neon treaz și să nu consume cota gratuită. Durata reală a cold start-ului este documentată în [`render/README.md`](render/README.md).
- **Calitate:** build și teste verzi în CI pentru ambele servicii; formatare Prettier (frontend); straturile backend-ului verificate cu ArchUnit.
- **Accesibilitate:** responsive de la 360 px, navigare cu tastatura, Lighthouse accessibility ≥ 90.

---

## 5. Non-scop (amânat, nu uitat)

- RAG peste un ghid de cumpărare (pgvector).
- Autentificare opțională (GitHub/Google) pentru istoric între dispozitive.
- i18n RO/EN.
- Date live (rechemări, prețul combustibilului) cu cache.
- Comutator de provider LLM (se folosește un singur model, [ADR 0007](adr/0007-model-openai.md)).

---

## 6. Cum se schimbă cerințele

1. Se discută.
2. Se actualizează acest fișier (faza curentă, tabelul fazelor, cerințele atinse) **în același pas cu codul**.
3. Dacă se schimbă una dintre deciziile D1–D9, se adaugă un ADR nou în `docs/adr/` și se actualizează `plan.md`.
4. O cerință amânată nu se șterge: se mută într-o fază viitoare sau în „Non-scop”.

---

## 7. Glosar

- **TCO** — costul total de deținere: achiziție, depreciere, energie, asigurare, taxe, mentenanță, dobândă.
- **APR / DAE** — costul anual efectiv al unui credit.
- **EV / ICE** — vehicul electric / cu ardere internă.
- **SSE** — Server-Sent Events: fluxul HTTP unidirecțional prin care backend-ul trimite răspunsul în bucăți.
- **ADR** — Architecture Decision Record: o notă scurtă cu o decizie și motivul ei.
