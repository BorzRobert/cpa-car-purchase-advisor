# Vercel — pași manuali

## Ce face

Găzduiește frontend-ul CPA online: construiește proiectul Next.js din `frontend/` la fiecare push pe GitHub și îl servește pe un URL public. Fără ea, aplicația rulează doar pe calculatorul tău, iar „merge la mine" nu poate fi verificat de nimeni. Backend-ul Java se găzduiește separat (Render, din Faza 3).

**Adăugată la:** Faza 4.1 (publicare) — imediat după primul răspuns real de model, ca fiecare funcționalitate următoare să fie verificată pe o aplicație deja publică.
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

Frontend-ul nu are nevoie de nicio variabilă ca să facă build — deploy-ul trece fără să configurezi nimic. Variabilele din `frontend/.env.example` (`ANTHROPIC_API_KEY`, `OPENAI_API_KEY`, `AUTH_*`) aparțin codului server vechi și dispar în Faza 6. Singura variabilă a frontend-ului țintă va fi:

| Variabilă                  | Fișier (local)        | Ajunge în browser?                    |
| -------------------------- | --------------------- | ------------------------------------- |
| `NEXT_PUBLIC_API_BASE_URL` | `frontend/.env.local` | DA (prefixul `NEXT_PUBLIC_` = public) |

Ea se introduce în Faza 3, când frontend-ul începe să cheme backend-ul.

În producție se configurează în: **Vercel → Project → Settings → Environment Variables**, atât pentru `Production`, cât și pentru `Preview`. Valorile din `.env.example` sunt placeholdere; valorile reale nu intră în repo.

Cheile de LLM (`ANTHROPIC_API_KEY`) se adaugă **doar** acolo, niciodată în repo. Handler-ele citesc variabilele la request, dar deployment-ul existent nu primește automat modificarea: după ce adaugi sau schimbi o variabilă, e nevoie de un **Redeploy**.

## Pași manuali

Ce nu poate face agentul:

1. **Import proiect:** Vercel → `Add New… → Project` → alege repo-ul de pe GitHub. La **Root Directory** alege `frontend` (repo-ul conține și `backend/`). Framework-ul e detectat automat („Next.js"); comenzile implicite de build sunt corecte, nu le modifica.
2. **Deploy:** apasă `Deploy` și așteaptă. Primul build durează câteva minute (instalează dependințele de la zero).
3. **Branch de producție:** Settings → Git → `Production Branch` → `main`. Push-urile pe `main` devin versiunea publică; orice alt branch primește un URL de `Preview` pentru review.
4. **Variabile:** Settings → Environment Variables → adaugă fiecare variabilă din `.env.example` pe `Production` și `Preview`. Pentru `ANTHROPIC_API_KEY`, folosește cheia generată în dashboard-ul Anthropic.
5. **Redeploy:** după adăugarea sau modificarea unei variabile, deschide deployment-ul relevant → meniul `⋯` → `Redeploy`.
6. La fiecare push pe `main`, Vercel face automat deploy public. Un push pe orice alt branch produce un Preview URL diferit.

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
4. Fă push pe un branch de lucru și confirmă un Preview URL diferit de Production. După push pe `main`, confirmă URL-ul public.

## Erori frecvente

| Eroare                                             | Cauză                                                  | Rezolvare                                                      |
| -------------------------------------------------- | ------------------------------------------------------ | -------------------------------------------------------------- |
| Build-ul cade cu erori de TypeScript / ESLint      | Vercel rulează build de producție, mai strict ca `dev` | Rulează `npm run build` local și repară înainte de push        |
| Deploy reușit, dar aplicația arată versiunea veche | Push-ul a mers pe alt branch decât cel de producție    | Settings → Git → Production Branch (`main`)                    |
| Build-ul Vercel nu găsește `package.json`          | Root Directory nu e setat pe `frontend`                | Settings → General → Root Directory → `frontend`               |
| Aplicația arată „provider neconfigurat”            | Variabila de mediu nu e configurată încă               | Configurează variabila în Production și Preview, apoi Redeploy |
| O variabilă nouă nu se vede în aplicație           | Deployment-ul existent nu a primit noua configurație   | Redeploy după fiecare modificare                               |
