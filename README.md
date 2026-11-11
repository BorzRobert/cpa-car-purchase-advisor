# CPA — Car Purchase Advisor

Consilier personal pentru cumpărarea unei mașini. Un **agent AI** află nevoile cumpărătorului, caută și compară mașini dintr-un catalog propriu, calculează costul total de deținere și explică de ce recomandă ce recomandă. Prețurile și cifrele vin din unelte deterministe din backend, nu din memoria modelului.

**Stadiul actual:** Faza 3 — „walking skeleton": backend-ul Java (`backend/`) este publicat pe Render cu PostgreSQL pe Neon, iar frontend-ul de pe Vercel afișează starea lui (cu tratarea cold start-ului). Domeniul, persistența și consilierul vin în fazele următoare. Planul complet este în [docs/plan.md](docs/plan.md), iar cerințele în [docs/requirements.md](docs/requirements.md).

**Aplicația publicată:** URL-urile se completează după primul deploy (Faza 3); pașii sunt în [docs/render/README.md](docs/render/README.md), [docs/neon/README.md](docs/neon/README.md) și [docs/vercel/README.md](docs/vercel/README.md).

## Structura repo-ului

| Cale                   | Ce e                                                                             |
| ---------------------- | -------------------------------------------------------------------------------- |
| `frontend/`            | aplicația Next.js (UI); rulează pe Vercel                                        |
| `backend/`             | serviciul Java 25 / Spring Boot 4 (API, logică de produs, singurul apel către OpenAI) |
| `docs/`                | cerințe, plan, ADR-uri, documentația integrărilor                                |
| `scripts/`             | sincronizarea instrucțiunilor pentru agenți și a skill-urilor, hook-ul de commit |
| `.claude/`, `.github/` | skill-uri și instrucțiuni pentru agenți (generate din `AGENTS.md`)               |

## Cum se rulează

Frontend (Node 20.9+):

```bash
cd frontend
npm install
npm run dev   # http://localhost:3000
```

| Comandă (în `frontend/`) | Ce face                                                   |
| ------------------------ | --------------------------------------------------------- |
| `npm run dev`            | server de dezvoltare (Turbopack)                          |
| `npm run build`          | build de producție                                        |
| `npm run lint`           | ESLint                                                    |
| `npm run format`         | formatează tot proiectul cu Prettier                      |
| `npm run format:check`   | verifică formatarea fără să modifice (varianta pentru CI) |

Variabilele de mediu ale frontend-ului sunt în `frontend/.env.example`; valorile reale merg în `frontend/.env.local` (gitignorat). Pentru a vedea starea backend-ului în UI, pornește backend-ul (mai jos) și pune `NEXT_PUBLIC_API_BASE_URL="http://localhost:8080"` în `frontend/.env.local`.

Backend (JDK 25 — Temurin — cu `JAVA_HOME` setat pe el; Maven Wrapper, fără Maven global):

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
