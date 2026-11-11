# CPA — Car Purchase Advisor — Cerințe

> Acest fișier este **sursa de adevăr** pentru ce construim. Orice schimbare de direcție se scrie AICI, nu doar în conversație. Planul de execuție pe faze, cu deciziile D1–D9, este în [`plan.md`](plan.md) (în engleză); deciziile au câte un ADR în [`adr/`](adr/).
>
> Ultima actualizare: 2026-11-11 · Faza curentă: **Faza 1 — restructurarea repo-ului** (frontend mutat în `frontend/`, curățenie de bază; backend-ul Java începe în Faza 2)

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
- **`backend/`** — Java 25 + Spring Boot 4.x (Maven Wrapper), Spring AI cu un singur model OpenAI, JPA + Flyway, PostgreSQL în producție și H2 local. Deține toată logica de produs: persona, uneltele, scoring-ul, TCO, memoria, limitele de rată și bugetul, persistența.
- **Comunicare:** REST/JSON sub `/api/v1` și un endpoint SSE pentru răspunsuri în streaming; identitate prin token de sesiune anonim (`Authorization: Bearer`); contract definit prima dată (OpenAPI), tipurile TypeScript se generează din el.
- **Hosting:** frontend pe Vercel, backend pe Render (Docker), bază de date pe Neon.

Detaliile (diagrame, evenimente SSE, straturi) sunt în `plan.md` §2 și vor ajunge în `docs/architecture/` pe măsură ce fazele se livrează.

---

## 3. Faze

| Faza | Conținut                                                                                   | Stare       |
| ---- | ------------------------------------------------------------------------------------------ | ----------- |
| 0    | Decizii (ADR D1–D9), unelte, regula de commit `CPA-<titlu>`                                | livrată     |
| 1    | Frontend în `frontend/`, curățenie de bază, documentație de pornire                        | în curs     |
| 2    | Schelet backend Spring Boot (straturi, ProblemDetail, CORS, OpenAPI, ArchUnit, Docker, CI) | următoarea  |
| 3    | Deploy „walking skeleton” (Render + Neon + Vercel)                                         | planificată |
| 4    | Domeniu, persistență, contract REST, sesiuni, limite                                       | planificată |
| 5    | Creierul consilierului: streaming, unelte, scoring/TCO, guardrails, evaluare               | planificată |
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
- **Un singur model:** configurat într-o singură proprietate de backend; fără selector de model în UI sau în cod.
- **Date și confidențialitate:** fără cont și fără date personale; token de sesiune stocat doar hash-uit; endpoint de „șterge-mi datele”; jurnalele nu conțin date personale.
- **Cost:** limită de rată pe sesiune și IP, buget zilnic de tokeni cu întrerupător global, lungime maximă a promptului, `max_output_tokens`.
- **Corectitudine:** prețurile, specificațiile și costurile vin doar din unelte deterministe; răspunsurile le marchează ca orientative, cu data „date la”.
- **Disponibilitate:** planul gratuit adoarme; interfața afișează „Se trezește consilierul…” în loc de eroare.
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
