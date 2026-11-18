# CPA — Car Purchase Advisor

Consilier personal pentru cumpărarea unei mașini. Un **agent AI** află nevoile cumpărătorului, caută și compară mașini dintr-un catalog propriu, calculează costul total de deținere și explică de ce recomandă ce recomandă. Prețurile și cifrele vin din unelte deterministe din backend, nu din memoria modelului.

**Stadiul actual:** Faza 9 — aplicația este în producție. Frontend-ul (Next.js, doar UI) rulează pe Vercel și vorbește cu backend-ul Java de pe Render (PostgreSQL pe Neon), singurul loc care apelează modelul OpenAI. Planul complet este în [docs/plan.md](docs/plan.md), iar cerințele în [docs/requirements.md](docs/requirements.md).

**Aplicația publicată:**

| Serviciu           | URL                                                                                              |
| ------------------ | ------------------------------------------------------------------------------------------------ |
| Aplicația (Vercel) | <https://cpa-car-purchase-advisor-borzrobert.vercel.app/>                                        |
| API (Render)       | <https://cpa-car-purchase-advisor.onrender.com/api/v1/health> (stare; backend-ul de pe planul Free adoarme, prima cerere poate dura ~1 minut) |

Un scenariu de prezentare verificat pe producție este în [docs/demo-prompt.txt](docs/demo-prompt.txt). Pașii de configurare a gazdelor: [docs/render/README.md](docs/render/README.md), [docs/neon/README.md](docs/neon/README.md), [docs/vercel/README.md](docs/vercel/README.md).

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
| `npm test`               | teste Vitest (logică pură: parser SSE, transformări, registre) |
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

## Convenția de nume

Din Faza 10, ramurile se numesc `CPA-<număr issue>-<mesaj-kebab>` (de exemplu `CPA-42-add-google-login`), iar prima linie a fiecărui commit și titlul PR-ului sunt `CPA-<număr issue> <mesaj>` (de exemplu `CPA-42 Add Google login`); numărul de issue este același în ramură, commit-uri și titlul PR-ului. Regulile sunt în `scripts/check-naming.sh`; hook-urile din `scripts/hooks/` le verifică local, iar verificarea CI `naming` le impune pe PR. Hook-urile se activează o singură dată cu:

```bash
git config core.hooksPath scripts/hooks
```

## Documente

| Fișier                                               | Ce conține                                                                                                                                               |
| ---------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [docs/requirements.md](docs/requirements.md)         | **Sursa de adevăr:** ce construim, faze, cerințe non-funcționale, non-scop                                                                               |
| [docs/plan.md](docs/plan.md)                         | Planul de transformare și de execuție (în engleză)                                                                                                       |
| [docs/adr/](docs/adr/)                               | Deciziile de arhitectură D1–D9 (plus ADR 0010)                                                                                                           |
| [docs/architecture/](docs/architecture/)             | Backend, frontend, contractul frontend ↔ backend, instantaneul OpenAPI                                                                                    |
| [docs/demo-prompt.txt](docs/demo-prompt.txt)         | Scenariul de demo (4 mesaje) verificat pe producție                                                                                                      |
| [docs/README.md](docs/README.md)                     | Indexul documentației + tabelul integrărilor externe                                                                                                     |
| [docs/_template/README.md](docs/_template/README.md) | Formatul obligatoriu pentru documentația unei integrări                                                                                                  |
| [AGENTS.md](AGENTS.md)                               | Convențiile pentru agenții AI. Fișier canonic — `CLAUDE.md` și `.github/copilot-instructions.md` se generează cu `sh scripts/sync-agent-instructions.sh` |

Cheile reale stau doar în fișiere `.env` gitignorate și în dashboard-urile gazdelor — în `docs/` se scriu doar numele variabilelor.
