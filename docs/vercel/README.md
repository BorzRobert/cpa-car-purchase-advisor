# Vercel — pași manuali

## Ce face

Găzduiește frontend-ul CPA online: construiește proiectul Next.js din `frontend/` la fiecare push pe GitHub și îl servește pe un URL public. Fără ea, aplicația rulează doar pe calculatorul tău, iar „merge la mine" nu poate fi verificat de nimeni. Backend-ul Java se găzduiește separat, pe Render ([`../render/README.md`](../render/README.md)); din Faza 3 frontend-ul îl apelează prin `NEXT_PUBLIC_API_BASE_URL`.

**Adăugată la:** Faza 4.1 (veche, publicare) — imediat după primul răspuns real de model, ca fiecare funcționalitate următoare să fie verificată pe o aplicație deja publică. **Actualizată în Faza 3:** variabila `NEXT_PUBLIC_API_BASE_URL`, strategia CORS pentru Production vs. Preview și „starea backend-ului" din UI.
**Obligatorie / opțională:** opțională pentru dezvoltare, obligatorie pentru a arăta aplicația altcuiva.

## Cont & chei

- Cont: <https://vercel.com/signup> — planul **Hobby** (gratuit) e suficient pentru acest proiect.
- Cel mai simplu e autentificarea cu contul de GitHub: așa Vercel vede direct repo-urile și nu mai trebuie configurat nimic pentru acces.
- **Nu ai nevoie de nicio cheie de API de la Vercel** pentru deploy din interfață. Un token (`Account Settings → Tokens`) e necesar doar dacă vrei deploy din linia de comandă sau din CI.

### Înainte de deploy: repo pe GitHub

1. Creează un repo nou (privat sau public) pe <https://github.com/new>. Nu bifa „Add a README" — proiectul are deja unul.
2. Local, în rădăcina repo-ului:
   ```bash
   git add .
   git commit -m "CPA-<titlu-kebab>"
   git remote add origin git@github.com:<utilizator>/<repo>.git
   git push -u origin main
   ```
3. Verifică pe GitHub că **NU** apare niciun fișier `.env.local`. E ignorat prin `.gitignore`, dar verificarea costă 5 secunde și o cheie publicată nu se mai poate „retrage".

## Variabile de mediu

Frontend-ul nu are nevoie de nicio variabilă ca să facă build — deploy-ul trece fără să configurezi nimic (fără `NEXT_PUBLIC_API_BASE_URL`, UI-ul afișează „Backend neconfigurat"). Singura variabilă a frontend-ului este:

| Variabilă                  | Fișier (local)        | Ajunge în browser?                    |
| -------------------------- | --------------------- | ------------------------------------- |
| `NEXT_PUBLIC_API_BASE_URL` | `frontend/.env.local` | DA (prefixul `NEXT_PUBLIC_` = public) |

Rândul din `frontend/.env.example` (local, backend-ul din `docker compose up` sau din `backend/`, pe portul 8080):

```dotenv
NEXT_PUBLIC_API_BASE_URL="http://localhost:8080"
```

În producție valoarea este **URL-ul public al backend-ului de pe Render** (`https://….onrender.com`), fără `/` la sfârșit. Se configurează în: **Vercel → Project → Settings → Environment Variables**, atât pentru `Production`, cât și pentru `Preview`. Valorile din `.env.example` sunt placeholdere; valorile reale nu intră în repo.

**Atenție:** valoarea este „coaptă" în JavaScript la `next build` (nu se citește la rulare), deci **după orice modificare e nevoie de Redeploy**. Este publică prin definiție: nu pune niciodată o cheie sau un secret într-o variabilă `NEXT_PUBLIC_*`.

Cheile de LLM nu au loc în Vercel: `OPENAI_API_KEY` există doar pe backend (Render), iar Next.js nu are Route Handlers și nu apelează niciun model. Dacă mai există în Vercel variabile din aplicația veche (`ANTHROPIC_API_KEY`, `OPENAI_API_KEY`, `AUTH_*`), pot fi șterse: codul nu le mai citește.

CORS nu se schimbă în Faza 6: frontend-ul trimite `Authorization` și `X-Request-Id` și citește `Retry-After`, iar toate trei sunt deja permise/expuse de backend (vezi mai jos și [`../architecture/communication.md`](../architecture/communication.md)).

## CORS: Production vs. Preview

Backend-ul acceptă apeluri din browser **doar** de la originile enumerate în `CORS_ALLOWED_ORIGINS` (variabilă Render, separate prin virgulă, niciodată `*` — o valoare cu `*` oprește pornirea backend-ului). Strategia decisă în Faza 3 (riscul „CORS surprises with Vercel preview URLs" din `plan.md` §10):

| Mediu                                | Origine în lista CORS                                                                                                      |
| ------------------------------------ | -------------------------------------------------------------------------------------------------------------------------- |
| Production (`main`)                  | URL-ul exact de producție, ex. `https://<proiect>.vercel.app` (sau domeniul propriu, când există) — **permanent**           |
| Preview al unui branch testat acum   | Aliasul **stabil al branch-ului**, `https://<proiect>-git-<branch>-<echipa>.vercel.app` (vezi Vercel → Deployment → Domains) — adăugat **deliberat** și șters la merge |
| Preview-uri pe URL de deployment (cu hash) | **Nu se listează**: se schimbă la fiecare push; se testează prin aliasul de branch                                    |
| Local                                | `http://localhost:3000` (implicit în dezvoltare; în producție nu există implicit)                                          |

De ce nu un wildcard (`https://*.vercel.app` sau un șablon pe echipă): orice proiect Vercel din lume ar putea chema API-ul care, mai târziu, costă bani (OpenAI). CORS nu este autentificare — protecția reală sunt tokenul de sesiune, limitele de rată și bugetul zilnic (Faza 4) —, dar lista strictă reduce suprafața, iar costul e mic: un branch nou nu funcționează până nu-i adaugi aliasul. Dacă fluxul de Preview o cere, în Faza 9 se poate reconsidera un șablon limitat la echipa proprie.

Cum arată simptomul unei origini lipsă: în UI, „Consilierul nu răspunde" după ~2,5 minute (browserul blochează răspunsul, deci pentru frontend arată ca o eroare de rețea). Verificare directă: vezi „Verificare" în [`../render/README.md`](../render/README.md).

Notă: Vercel poate proteja Preview-urile cu autentificare (Deployment Protection); un Preview protejat se deschide doar autentificat în contul tău, ceea ce e util: un URL de test nu e public.

## Swagger / OpenAPI în producție

Decis în Faza 3: **Swagger UI și `/v3/api-docs` sunt oprite în producție** (profilul `prod` setează `springdoc.api-docs.enabled=false` și `springdoc.swagger-ui.enabled=false`; cererile primesc `404`). Rămân active local și în CI — de acolo se obține documentul OpenAPI pentru tipurile TypeScript (Faza 6) și instantaneul `docs/architecture/openapi.json` (Faza 4). Motiv: un Swagger public ar enumera toate endpoint-urile și ar permite „Try it out" împotriva unui API care va costa bani; se reconsideră în Faza 9 doar dacă există un motiv.

## Pași manuali

Ce nu poate face agentul:

1. **Import proiect:** Vercel → `Add New… → Project` → alege repo-ul de pe GitHub. La **Root Directory** alege `frontend` (repo-ul conține și `backend/`). Framework-ul e detectat automat („Next.js"); comenzile implicite de build sunt corecte, nu le modifica.
2. **Deploy:** apasă `Deploy` și așteaptă. Primul build durează câteva minute (instalează dependințele de la zero).
3. **Branch de producție:** Settings → Git → `Production Branch` → `main`. Push-urile pe `main` devin versiunea publică; orice alt branch primește un URL de `Preview` pentru review.
4. **Variabile:** Settings → Environment Variables → adaugă `NEXT_PUBLIC_API_BASE_URL` (URL-ul public al backend-ului de pe Render, fără `/` final) pe `Production` **și** `Preview`. Cât timp codul server vechi încă există (până în Faza 6), variabilele lui din `.env.example` rămân relevante doar dacă folosești acele funcții; nu adăuga chei noi de LLM în Vercel.
5. **CORS:** pune originea Vercel (producție și, la nevoie, aliasul stabil al branch-ului de test) în `CORS_ALLOWED_ORIGINS` din Render — vezi secțiunea „CORS: Production vs. Preview".
6. **Redeploy:** după adăugarea sau modificarea unei variabile, deschide deployment-ul relevant → meniul `⋯` → `Redeploy` (variabila `NEXT_PUBLIC_*` se aplică doar la build).
7. La fiecare push pe `main`, Vercel face automat deploy public. Un push pe orice alt branch produce un Preview URL diferit.

## Cost & limite

- **Hobby: gratuit**, pentru proiecte necomerciale. Include build-uri automate, HTTPS și URL-uri de preview.
- Limite relevante pentru noi, pe planul gratuit: număr limitat de build-uri pe zi și un plafon lunar de transfer/execuție. Pentru un proiect de curs nu se ating.
- **Ce va umfla factura mai târziu (nu Vercel, ci providerul de LLM):** de la Faza 3, fiecare răspuns costă tokeni. Un URL public + o cheie de API înseamnă că oricine deschide aplicația consumă pe cheia ta — de asta Deployment Protection devine relevantă exact atunci.
- Verifică cifrele actuale înainte să te bazezi pe ele: <https://vercel.com/pricing>.

## Verificare

1. Local, înainte de deploy, mută temporar `frontend/.env.local` în afara proiectului și rulează, din `frontend/`:
   ```bash
   npm run build
   ```
   Trebuie să treacă fără nicio cheie. Vercel rulează aceeași comandă.
2. Rulează `npm run format:check` și verifică skill-ul `pre-deploy`.
3. După deploy, deschide URL-ul primit pe laptop și telefon: ecranul de start apare, lista de conversații e populată, preferințele se deschid, iar tema se schimbă din General → Appearance.
4. **Starea backend-ului (Faza 3):** în subsolul sidebar-ului apare „Backend conectat · v…" (și, dacă backend-ul doarme, „Se trezește consilierul…" cu un mesaj temporar, apoi „Consilierul este gata."). Dacă backend-ul nu răspunde în ~2,5 minute, apare „Consilierul nu răspunde" cu buton „Reîncearcă".
5. Fă push pe un branch de lucru și confirmă un Preview URL diferit de Production. După push pe `main`, confirmă URL-ul public.

## Erori frecvente

| Eroare                                             | Cauză                                                  | Rezolvare                                                      |
| -------------------------------------------------- | ------------------------------------------------------ | -------------------------------------------------------------- |
| Build-ul cade cu erori de TypeScript / ESLint      | Vercel rulează build de producție, mai strict ca `dev` | Rulează `npm run build` local și repară înainte de push        |
| Deploy reușit, dar aplicația arată versiunea veche | Push-ul a mers pe alt branch decât cel de producție    | Settings → Git → Production Branch (`main`)                    |
| Build-ul Vercel nu găsește `package.json`          | Root Directory nu e setat pe `frontend`                | Settings → General → Root Directory → `frontend`               |
| Aplicația arată „Backend neconfigurat”             | `NEXT_PUBLIC_API_BASE_URL` lipsește la momentul build-ului | Adaug-o în Production și Preview, apoi Redeploy                |
| Aplicația arată „Consilierul nu răspunde”          | URL greșit, backend oprit sau origine lipsă din CORS   | Verifică URL-ul, logurile Render și `CORS_ALLOWED_ORIGINS`     |
| O variabilă nouă nu se vede în aplicație           | Deployment-ul existent nu a primit noua configurație (`NEXT_PUBLIC_*` se aplică la build) | Redeploy după fiecare modificare |
