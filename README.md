# CPA — Car Purchase Advisor

Consilier personal pentru cumpărarea unei mașini. Un **agent AI** află nevoile cumpărătorului, caută și compară mașini dintr-un catalog propriu, calculează costul total de deținere și explică de ce recomandă ce recomandă. Prețurile și cifrele vin din unelte deterministe din backend, nu din memoria modelului.

**Stadiul actual:** Faza 6 — frontend-ul este conectat la backend: conversațiile, profilul („Nevoile tale”) și lista scurtă vin din backend, iar răspunsurile consilierului curg prin SSE; Next.js nu mai conține cod de server și nu apelează niciun model. Backend-ul Java (`backend/`) este publicat pe Render cu PostgreSQL pe Neon, iar frontend-ul pe Vercel. Redesign-ul, ghidul și întărirea pentru producție vin în fazele următoare. Planul complet este în [docs/plan.md](docs/plan.md), iar cerințele în [docs/requirements.md](docs/requirements.md).

**Aplicația publicată:** URL-urile se completează după primul deploy (Faza 3); pașii sunt în [docs/render/README.md](docs/render/README.md), [docs/neon/README.md](docs/neon/README.md) și [docs/vercel/README.md](docs/vercel/README.md).

## Structura repo-ului

| Cale                   | Ce e                                                                             |
| ---------------------- | -------------------------------------------------------------------------------- |
| `frontend/`            | aplicația Next.js (UI); rulează pe Vercel                                        |
| `backend/`             | serviciul Java 25 / Spring Boot 4 (API, logică de produs, singurul apel către OpenAI) |
| `docker-compose.yml`   | stiva locală: PostgreSQL + backend (`docker compose up --build`)                  |
| `docs/`                | cerințe, plan, ADR-uri, documentația integrărilor                                |
| `scripts/`             | sincronizarea instrucțiunilor pentru agenți și a skill-urilor, hook-ul de commit |
| `.claude/`, `.github/` | skill-uri și instrucțiuni pentru agenți (generate din `AGENTS.md`)               |

## Cum se rulează

Stiva completă local (backend pe PostgreSQL + frontend). Cere Docker și Node 20.9+:

```bash
docker compose up --build          # PostgreSQL + backend pe http://localhost:8080
cd frontend
cp .env.example .env.local         # NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
npm install
npm run dev                        # http://localhost:3000
```

Fără `OPENAI_API_KEY` totul pornește, iar consilierul arată starea „neactivat” (`503 advisor-not-configured`); pentru răspunsuri reale exportă cheia în shell înainte de `docker compose up` (vezi [docs/openai/README.md](docs/openai/README.md)).

| Comandă (în `frontend/`) | Ce face                                                   |
| ------------------------ | --------------------------------------------------------- |
| `npm run dev`            | server de dezvoltare (Turbopack)                          |
| `npm run build`          | build de producție                                        |
| `npm run lint`           | ESLint                                                    |
| `npm test`               | teste Vitest (parserul SSE, transformările mesajelor)     |
| `npm run api:types`      | regenerează `src/lib/api/schema.d.ts` din `docs/architecture/openapi.json` |
| `npm run format`         | formatează tot proiectul cu Prettier                      |
| `npm run format:check`   | verifică formatarea fără să modifice (varianta pentru CI) |

Variabilele de mediu ale frontend-ului sunt în `frontend/.env.example` (doar `NEXT_PUBLIC_API_BASE_URL`); valorile reale merg în `frontend/.env.local` (gitignorat).

```bash
cd backend
./mvnw spring-boot:run   # http://localhost:8080/api/v1/health
```

Pornește și fără cheie OpenAI. Variabilele sunt în `backend/.env.example` (copie în `backend/.env`, gitignorat); detalii în [docs/architecture/backend.md](docs/architecture/backend.md) și [docs/openai/README.md](docs/openai/README.md).

| Comandă (în `backend/`)                    | Ce face                                                           |
| ------------------------------------------ | ----------------------------------------------------------------- |
| `./mvnw verify`                            | compilează și rulează toate testele (inclusiv ArchUnit)           |
| `./mvnw spring-boot:run`                   | pornește serviciul local (H2 în memorie)                          |
| `docker build -t cpa-backend backend`      | construiește imaginea Docker                                      |

## Convenția de commit

Prima linie a fiecărui commit este `CPA-<titlu-kebab>`, de exemplu `CPA-added-backend-service`. Hook-ul din `scripts/hooks/` o verifică; se activează o singură dată cu:

```bash
git config core.hooksPath scripts/hooks
```

## Documente

| Fișier                                               | Ce conține                                                                                                                                               |
| ---------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [docs/requirements.md](docs/requirements.md)         | **Sursa de adevăr:** ce construim, faze, cerințe non-funcționale, non-scop                                                                               |
| [docs/plan.md](docs/plan.md)                         | Planul de transformare și de execuție (în engleză)                                                                                                       |
| [docs/adr/](docs/adr/)                               | Deciziile de arhitectură D1–D9                                                                                                                           |
| [docs/README.md](docs/README.md)                     | Indexul documentației + tabelul integrărilor externe                                                                                                     |
| [docs/_template/README.md](docs/_template/README.md) | Formatul obligatoriu pentru documentația unei integrări                                                                                                  |
| [AGENTS.md](AGENTS.md)                               | Convențiile pentru agenții AI. Fișier canonic — `CLAUDE.md` și `.github/copilot-instructions.md` se generează cu `sh scripts/sync-agent-instructions.sh` |

Cheile reale stau doar în fișiere `.env` gitignorate și în dashboard-urile gazdelor — în `docs/` se scriu doar numele variabilelor.
