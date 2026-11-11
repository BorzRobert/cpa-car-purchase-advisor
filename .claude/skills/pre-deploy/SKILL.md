---
name: pre-deploy
description: Verifică proiectul CPA (frontend + backend) înainte de publicare în Vercel și Render.
---

# Pre-deploy

Rulează această listă înainte de fiecare publicare. Comenzile npm se rulează din `frontend/`, cele Maven din `backend/`.

## Frontend (Vercel)

1. Mută temporar `frontend/.env.local` în afara proiectului și rulează `npm run build`. Build-ul trebuie să treacă fără nicio cheie.
2. Rulează `npm run lint` și `npm run format:check`.
3. Verifică faptul că `docs/requirements.md` descrie starea curentă.
4. Pentru fiecare variabilă din `frontend/.env.example` care mai e folosită (în arhitectura țintă: doar `NEXT_PUBLIC_API_BASE_URL`), verifică în Vercel → Project → Settings → Environment Variables că există atât pe `Production`, cât și pe `Preview`. `NEXT_PUBLIC_API_BASE_URL` trebuie să fie URL-ul public al backend-ului de pe Render, fără `/` final. Directorul rădăcină al proiectului Vercel este `frontend`.
5. Dacă ai adăugat sau modificat o variabilă, fă Redeploy pentru deployment-ul relevant (valorile `NEXT_PUBLIC_*` se aplică doar la build).

## Backend (Render + Neon)

1. Cu JDK 25 în `JAVA_HOME`, rulează `./mvnw verify` în `backend/` (include testul ArchUnit, pornirea fără cheie OpenAI, migrarea Flyway și profilul `prod`).
2. Dacă ai atins `backend/Dockerfile` sau `pom.xml`, rulează `docker build backend` (CI face același lucru).
3. Verifică `render.yaml`: nicio valoare secretă (doar `sync: false` pentru parola bazei, URL-ul bazei, `OPENAI_API_KEY`, `CORS_ALLOWED_ORIGINS`, `CPA_LLM_MODEL`) și că orice variabilă nouă din cod apare și în `backend/.env.example` și în `docs/<integrare>/README.md`.
4. În Render → serviciul `cpa-backend` → Environment: există toate variabilele din `render.yaml` (`SPRING_PROFILES_ACTIVE=prod`, `SPRING_DATASOURCE_URL` în format JDBC cu `?sslmode=require`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `CORS_ALLOWED_ORIGINS`, `OPENAI_API_KEY`, `CPA_LLM_MODEL`). `CORS_ALLOWED_ORIGINS` conține exact originea de producție a frontend-ului (și, dacă testezi un branch, aliasul lui stabil) — fără `*`.
5. `OPENAI_API_KEY` este substituentul `your_openai_api_key` până în Faza 5/9; o cheie reală se pune doar în dashboard, niciodată în repo, și cere limită lunară setată în contul OpenAI.
6. Verifică sănătatea după deploy (înlocuiește `<URL-backend>`):
   - `curl <URL-backend>/actuator/health` → `status: UP`;
   - `curl <URL-backend>/api/v1/health` → `status: UP`, `advisorConfigured` corespunde cheii (nu apare niciodată valoarea ei);
   - `curl -o /dev/null -w "%{http_code}" <URL-backend>/v3/api-docs` → `404` (Swagger oprit în producție).
7. În Neon (SQL Editor): `select version, description, success from flyway_schema_history;` arată toate migrările aplicate cu `success = true`.

## Final

1. Deschide URL-ul Preview pe telefon: sidebar-ul arată „Backend conectat · v…" (sau „Se trezește consilierul…" și apoi conectat dacă backend-ul dormea). După push-ul pe `main`, repetă pe URL-ul Production.
2. Dacă ai modificat acest fișier, rulează `sh scripts/sync-skills.sh` (oglinda Copilot din `.github/skills/`).

Nu urca niciodată `.env.local` sau `backend/.env` și nu scrie valori reale (chei, parole, șiruri de conexiune, identificatori de cont) în `docs/`, `.env.example`, `render.yaml` sau în loguri.
