# CPA — Car Purchase Advisor

Consilier personal pentru cumpărarea unei mașini. Un **agent AI** află nevoile cumpărătorului, caută și compară mașini dintr-un catalog propriu, calculează costul total de deținere și explică de ce recomandă ce recomandă. Prețurile și cifrele vin din unelte deterministe din backend, nu din memoria modelului.

**Stadiul actual:** Faza 1 — frontend-ul (Next.js) a fost mutat în `frontend/`; backend-ul Java (Spring Boot) începe în Faza 2. Planul complet este în [docs/plan.md](docs/plan.md), iar cerințele în [docs/requirements.md](docs/requirements.md).

**Aplicația publicată:** se completează după Faza 3 (deploy).

## Structura repo-ului

| Cale                   | Ce e                                                                             |
| ---------------------- | -------------------------------------------------------------------------------- |
| `frontend/`            | aplicația Next.js (UI); rulează pe Vercel                                        |
| `backend/`             | serviciul Java 25 / Spring Boot — **nu există încă**, se adaugă în Faza 2        |
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

Variabilele de mediu ale frontend-ului sunt în `frontend/.env.example`; valorile reale merg în `frontend/.env.local` (gitignorat).

Backend: va fi descris aici când apare, în Faza 2 (JDK 25 și Maven Wrapper, fără Maven global).

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
