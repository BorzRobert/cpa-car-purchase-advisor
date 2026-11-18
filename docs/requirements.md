# CPA — Car Purchase Advisor — Cerințe

> Acest fișier este **sursa de adevăr** pentru ce construim. Orice schimbare de direcție se scrie AICI, nu doar în conversație. Planul de execuție pe faze, cu deciziile D1–D9, este în [`plan.md`](plan.md) (în engleză); deciziile au câte un ADR în [`adr/`](adr/).
>
> Ultima actualizare: 2026-11-12 · Faza curentă: **Faza 9 — producție, documentație finală și audit de curățenie**. Fazele 1–8 sunt livrate; aplicația rulează în producție (Vercel + Render + Neon, URL-urile sunt în [`../README.md`](../README.md)), iar modelul real a fost verificat pe producție (vezi [`openai/README.md`](openai/README.md#verificare)). Rămân de făcut de mână: o verificare de pe telefon și măsurarea cold start-ului, vezi [`render/README.md`](render/README.md#observat-pe-render)

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

România/UE, prețuri în EUR (cu conversie în RON). Codul, comentariile, commit-urile și `docs/` sunt în engleză; interfața este în română (implicit) și în engleză, cu comutator în Preferințe → General ([ADR 0001](adr/0001-piata-moneda-limba.md), înlocuit parțial de [ADR 0011](adr/0011-limba-engleza-si-i18n.md)).

---

## 2. Arhitectură

- **`frontend/`** — Next.js 16 + React 19 + TypeScript + Tailwind v4 + shadcn/ui, folosit **doar ca UI**: fără Route Handlers, fără chei, doar `NEXT_PUBLIC_API_BASE_URL`.
- **`backend/`** — Java 25 + Spring Boot 4.1.2 (Maven Wrapper), Spring AI 2.0.2 cu un singur model OpenAI, JPA + Flyway, PostgreSQL în producție și H2 local. Deține toată logica de produs: persona, uneltele, scoring-ul, TCO, memoria, limitele de rată și bugetul, persistența. Aplicația pornește și fără `OPENAI_API_KEY` (consilierul răspunde `503 advisor-not-configured`).
- **Comunicare:** REST/JSON sub `/api/v1` și un endpoint SSE pentru răspunsuri în streaming; identitate delegată unui furnizor terț de autentificare (Google, GitHub etc.), cu token anonim de sesiune păstrat pentru invitați (`Authorization: Bearer`); contract definit prima dată (OpenAPI), tipurile TypeScript se generează din el.
- **Hosting:** frontend pe Vercel, backend pe Render (Docker, plan Free), bază de date pe Neon (Free). Backend-ul rulează în producție cu profilul Spring `prod` (baza din mediu, Swagger/OpenAPI oprite, pool reglat pentru Neon); migrările Flyway rulează la pornire pe H2 și pe PostgreSQL. CORS: listă explicită de origini (producția Vercel + aliasul stabil al branch-ului testat), fără `*` — vezi [`vercel/README.md`](vercel/README.md).

Detaliile (diagrame, evenimente SSE, straturi) sunt în `plan.md` §2; backend-ul așa cum este construit acum e descris în [`architecture/backend.md`](architecture/backend.md), contractul frontend ↔ backend în [`architecture/communication.md`](architecture/communication.md) (cu instantaneul OpenAPI [`architecture/openapi.json`](architecture/openapi.json)), iar integrările în [`render/`](render/README.md), [`neon/`](neon/README.md) și [`vercel/`](vercel/README.md); restul ajunge în `docs/architecture/` pe măsură ce fazele se livrează.

**Livrat în Faza 4 (REST fără model):** sesiuni anonime (`/sessions`, token hash-uit), profil, conversații și mesaje, lista scurtă, catalogul de vehicule (14 modele / 21 de variante, cifre **orientative și neverificate**, „date la” 2026-11-11), consum și buget zilnic de tokeni, limite de rată (Bucket4j, per instanță) și de volum.

**Livrat în Faza 5a (streaming LLM):** `POST /conversations/{id}/messages` răspunde cu `text/event-stream` (evenimente `meta`, `token`, `done`, `error`; heartbeat), salvează răspunsul consilierului, înregistrează tokenii reali (sau estimați) și costul fiecărui răspuns în jurnal și în `message.metadata`, verifică bugetul înainte de apelul către model. Promptul de sistem este compus într-un singur loc (`ai.AdvisorPrompt`, versionat), iar profilul și mesajele utilizatorului intră ca date delimitate. Fără cheie OpenAI: `503 advisor-not-configured`. CI rulează cu un model înregistrat, fără cheie. Apelul real către model a fost verificat ulterior pe producție (vezi [`openai/README.md`](openai/README.md#verificare)). **Livrat în Faza 5b (unelte):** agentul are cinci unelte deterministe — `updateBuyerProfile`, `searchVehicles`, `compareVehicles`, `addToShortlist`, `getShortlist` — pe care le cheamă prin bucla de unelte Spring AI; fiecare apel apare în UI ca eveniment `tool`, cardurile ca `vehicles`, iar schimbările de profil ca `profile`. Prețurile și specificațiile vin doar din rezultatele uneltelor (cu „date la”); numărul de apeluri dintr-un răspuns este plafonat. **Livrat în Faza 5c (scoring, TCO, finanțare):** clasament pe șase criterii cu ponderi fixe și explicații (`rankVehicles`, `compareVehicles` → evenimentul `compare`), cost total de deținere (`calculateTco`), finanțare cu anuitate (`calculateFinancing`) și punctul de echilibru electric vs. ardere (`evVsIceBreakEven`), toate ca **cod pur** testat cu cifre calculate independent; rezultatele își enumeră ipotezele. Prețurile de energie din calculator (`cpa.pricing.*`) sunt **orientative și neverificate**, ca și catalogul, și trebuie revizuite înainte de producție; „fiabilitatea” este o aproximare (valoarea reziduală), iar must-have-urile din profil nu se potrivesc automat. **Livrat în Faza 5d (garduri, memorie, evaluare):** gard determinist împotriva scurgerii promptului, audit de ancorare al cifrelor și mărcilor din fiecare răspuns (măsurat, nu blocant), prompt de analiză a anunțurilor lipite, memorie cu rezumat rulant (migrarea `V4`) și un set de scenarii „de aur” (cele 4 scenarii de acceptare + scurgere + memorie) rulat în CI cu un model înregistrat; același set rulează manual cu modelul real (`RealModelEvalTest`, oprit implicit). **Criteriul „done when” al Fazei 5 este îndeplinit prin varianta „model înregistrat în CI”, iar modelul real a fost verificat pe producție** (vezi [`openai/README.md`](openai/README.md#verificare)); scenariul de acceptare 2 („Golf, Corolla, Octavia”) nu trece pe modelul real din cauza căutării fără filtru după nume (vezi [`architecture/backend.md`](architecture/backend.md)), iar scenariul de demo verificat este în [`demo-prompt.txt`](demo-prompt.txt).

**Livrat în Faza 6 (frontend conectat):** frontend-ul este acum doar UI. Clientul `lib/api` are tipurile generate din `architecture/openapi.json` (`npm run api:types`), `lib/sse.ts` parsează fluxul SSE (`fetch` + `ReadableStream`, `AbortController`; comentariile sunt heartbeat, iar un flux fără `done`/`error` este „incomplet”), sesiunea anonimă se creează la prima cerere și tokenul stă în `cpa-app` (un 401 înseamnă sesiune nouă), datele (conversații, mesaje, profil, listă scurtă) vin din backend prin TanStack Query, iar `useAdvisorChat` ține răspunsul care curge (text, activitate de unelte, carduri, clasament) până se încheie fluxul, apoi istoricul se reia din backend. Profilul de skill-uri a fost înlocuit de formularul „Nevoile tale” (Preferințe), iar lista scurtă are un panou în Preferințe și buton pe cardurile de vehicule. Stări prietenoase pentru `503 advisor-not-configured`, `429` (`budget-exceeded` și `rate-limited`, cu `Retry-After`), rețea/cold start și flux întrerupt. Forma stocată `cpa-app` a trecut la versiunea 8 (conține doar tokenul). **Curățenie (Wave 2):** șterse `api/chat`, `api/providers`, `api/auth`, `api/about`, `lib/providers*`, `system-prompt`, `cost`, `cache`, `mock`, Auth.js, formularele de provider și „Despre”, dependențele `ai`, `@ai-sdk/*` și `next-auth`, plus documentația integrărilor vechi de LLM și de autentificare; build-ul Next.js nu mai conține nicio rută de server și niciun cod care apelează un model. `docker-compose.yml` pornește PostgreSQL și backend-ul local (`docker compose up --build`), iar frontend-ul se pornește separat. Cardurile și clasamentul erau afișate în Faza 6 într-o formă minimă (widget-urile finale: Faza 7, mai jos); backend-ul salvează doar textul mesajelor, deci cardurile dispar la reîncărcarea paginii.

**Livrat în Faza 7 (redesign și componente):** identitate vizuală nouă („tablou de bord / showroom noaptea”: grafit cald, chihlimbar pentru acțiuni, turcoaz pentru semnale, rază mică, font de afișare pentru titluri și cifre cu lățime fixă pentru prețuri), toate ca tokeni în `globals.css` (`:root` și `.dark`). Aspectul are trei zone: navigația subțire (conversații), conversația și un **panou lateral viu** („Nevoile tale” ca chip-uri cu cursor de buget total/pe lună, lista scurtă, costul total curent, stepper-ul etapelor), actualizat din evenimentele `profile`, `vehicles` și `compare`; sub 1280 px panoul devine foaie de jos, deschisă dintr-un singur buton din header (perechea lui `SidebarTrigger`, nu o bară de acțiuni). Componente: card de vehicul (inel de scor, insigne, „date la” lângă fiecare preț), tabel de comparație cu bare pe criterii, descompunerea TCO ca bară stivuită, chip-uri de activitate pe unelte, vizualizarea listei scurte. Calitate: responsive până la 360 px, navigare completă de la tastatură, `role="log"` cu `aria-live` pentru răspunsul care curge, mișcare redusă respectată, scheleturi și stări goale/eroare/cold start. Verificat cu axe (0 încălcări pe ecran de start, conversație, ghid, panou mobil, temă închisă și deschisă) și Lighthouse (accesibilitate 100 pe desktop și mobil), fără derulare orizontală la 360 și 390 px. Cardurile și clasamentul rămân doar în memoria paginii (backend-ul salvează doar textul).

**Livrat în Faza 8 (ghid în aplicație):** registrul `lib/guide.ts` (ce poate face consilierul, cum obții răspunsuri bune, cum citești cardurile/scorul/TCO, panoul și lista scurtă, limite și disclaimer-e, confidențialitate) alimentează dialogul „Ghid” (file), dialogul de bun venit la prima vizită (3 pași, se poate sări; marcajul `onboardingSeen` stă în `cpa-app`, trecut la versiunea 9 cu `migrate`) și sugestiile de pe ecranul de start (aceeași listă de exemple, legate de etapele călătoriei; un click le pune în câmpul de scris, nu le trimite). Puncte de intrare: „Ghid” în sidebar și tasta `?`. Ajutor contextual: glosar (TCO, DAE/APR, depreciere, valoare reziduală) ca tooltip-uri și ca tab în ghid, dintr-un singur registru. „Șterge-mi datele” cheamă `DELETE /api/v1/sessions/current`, apoi pornește o sesiune nouă. Dialogul ghidului are filele într-o coloană verticală de la 768 px și într-o grilă cu două coloane pe telefon, fără derulare orizontală.

**Livrat în Faza 9 (producție și curățenie):** aplicația rulează pe Vercel (frontend) și Render + Neon (backend); verificat pe producție: stare backend, CORS (originea de producție acceptată, una străină refuzată), Swagger oprit, scenariul de demo din [`demo-prompt.txt`](demo-prompt.txt) rulat de două ori cu modelul real. Documentația finală: `architecture/frontend.md` și `architecture/backend.md` finalizate, `README.md` cu URL-urile publice, indexul `docs/README.md`. Audit de curățenie: comanda `git grep` din `plan.md` (Faza 9) găsește doar istoricul din `plan.md` (descrierea transformării); `knip` și `depcheck` fără fișiere sau dependențe nefolosite (singurele exporturi nefolosite sunt în componentele shadcn generate); fișierul orfan `docs/claude.example.png` șters; `.env.example` ale ambelor servicii corespund codului. **Rămân de făcut de mână:** verificarea aplicației de pe un telefon real și măsurarea cold start-ului.

---

## 3. Faze

| Faza | Conținut                                                                                   | Stare       |
| ---- | ------------------------------------------------------------------------------------------ | ----------- |
| 0    | Decizii (ADR D1–D9), unelte, regula de commit `CPA-<titlu>`                                | livrată     |
| 1    | Frontend în `frontend/`, curățenie de bază, documentație de pornire                        | livrată     |
| 2    | Schelet backend Spring Boot (straturi, ProblemDetail, CORS, OpenAPI, ArchUnit, Docker, CI) | livrată     |
| 3    | Deploy „walking skeleton” (Render + Neon + Vercel): stare backend în UI, cold start, Flyway pe Postgres, `render.yaml`, CI frontend | livrată (cold start de măsurat) |
| 4    | Domeniu, persistență, contract REST, sesiuni, limite                                       | livrată     |
| 5    | Creierul consilierului: streaming (5a), unelte (5b), scoring/TCO (5c), guardrails/evaluare (5d) | livrată (verificată cu modelul real pe producție, 2026-11-12) |
| 6    | Integrarea frontend-ului, ștergerea codului server vechi (AI SDK, Auth.js, `/api/*`)       | livrată     |
| 7    | Redesign și componente specifice (card vehicul, comparație, TCO, panou lateral)            | livrată     |
| 8    | Ghid în aplicație                                                                          | livrată     |
| 9    | Întărire, producție, documentație finală, audit de curățenie                               | în curs (în producție; rămân verificarea de pe telefon și cold start-ul) |
| 10   | Iterația următoare: engleză + i18n RO/EN, login terț cu istoric per utilizator, aspect desktop/mobil, titlu și iconiță, reguli de branch, cercetări — vezi [`next-iteration-requirements.md`](next-iteration-requirements.md) | planificată |

### Scenarii de acceptare

1. _„Am 20.000 €, fac ~15.000 km/an mai ales în oraș și nu pot încărca acasă. Benzină, hibrid sau electric?”_ — întreabă doar ce lipsește, apelează uneltele TCO / EV-vs-ICE, cifrele se potrivesc cu calculatorul.
2. _„Compară un Golf, un Corolla și un Octavia pentru o familie de patru persoane pe 5 ani.”_ — widget de comparație cu TCO și scor pe criterii, nu un tabel inventat.
3. _[anunț lipit]_ _„E o ofertă bună?”_ — fapte extrase, semnale de alarmă, întrebări pentru vânzător, listă de inspecție; fără istoric inventat.
4. O cerere în afara subiectului sau o încercare de prompt injection — redirecționare politicoasă, fără scurgeri de reguli.

---

## 4. Cerințe non-funcționale

- **Chei:** `OPENAI_API_KEY` există doar în mediul backend-ului; niciodată în browser, în URL, în loguri sau în repo. Fișierele `.env.example` conțin doar valori-substituent.
- **Un singur model:** configurat într-o singură proprietate de backend (`cpa.llm.model`); fără selector de model în UI sau în cod. Valoarea implicită este `gpt-5.6-terra` ([ADR 0010](adr/0010-model-openai-inlocuitor-gpt-5-mini.md), acceptat la 2026-11-11): `gpt-5-mini` din D7 este deprecat, cu oprire la 2026-12-11. Detalii și prețuri în [`openai/README.md`](openai/README.md).
- **Date și confidențialitate:** autentificarea este delegată unui furnizor terț (proiectul nu stochează parole și nu implementează securitate de autentificare); se păstrează doar minimul (identificatorul stabil al utilizatorului la furnizor, opțional nume/avatar), iar fiecare utilizator autentificat își vede doar istoricul propriu; invitații folosesc un token de sesiune stocat doar hash-uit (SHA-256); `DELETE /api/v1/sessions/current` („șterge-mi datele”) șterge în cascadă profilul, conversațiile și lista scurtă (contorii de consum rămân anonimi, ca să nu se poată reseta bugetul); jurnalele nu conțin date personale sau tokenuri. Textul din browser este curățat și limitat la intrare.
- **Cost:** limită de rată pe sesiune și IP, buget zilnic de tokeni pe sesiune și global (întrerupătorul global este bugetul global), lungime maximă a mesajului (4000 de caractere), plafoane de volum (conversații, mesaje, lista scurtă) — toate implementate în Faza 4 și testate; din Faza 5a `max_completion_tokens` (`cpa.llm.max-output-tokens`) plafonează un răspuns, iar tokenii reali se înregistrează după fiecare răspuns. Limitele de rată sunt în memorie, per instanță (o singură instanță pe planul Free); verificarea adresei IP în spatele proxy-ului Render nu a fost confirmată pe instanța reală (`CPA_TRUSTED_PROXY_HOPS` o corectează fără cod). Bugetul pe sesiune (60.000 de tokeni pe zi) acoperă un scenariu de ~4 mesaje cu unelte (~15.000 de tokeni pe mesaj).
- **Corectitudine:** prețurile, specificațiile și costurile vin doar din unelte deterministe; răspunsurile le marchează ca orientative, cu data „date la”.
- **Disponibilitate:** planul gratuit adoarme (Render după 15 minute fără trafic, ~1 minut pentru trezire + pornirea JVM; Neon după 5 minute, trezire în sute de ms); interfața pornește un ping la `/actuator/health` la încărcare și afișează „Se trezește consilierul…” în loc de eroare, reîncercând timp de ~2,5 minute. `/actuator/health` nu interoghează baza, ca să nu țină Neon treaz și să nu consume cota gratuită. Durata reală a cold start-ului este documentată în [`render/README.md`](render/README.md).
- **Calitate:** build și teste verzi în CI pentru ambele servicii; formatare Prettier (frontend); teste Vitest pentru parserul SSE și transformările mesajelor (`npm test`); straturile backend-ului verificate cu ArchUnit.
- **Accesibilitate:** responsive de la 360 px, navigare cu tastatura, Lighthouse accessibility ≥ 90.

---

## 5. Non-scop (amânat, nu uitat)

- RAG peste un ghid de cumpărare (pgvector).
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
