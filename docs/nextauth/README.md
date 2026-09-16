# Auth.js (NextAuth)

## Ce face

Auth.js identifică utilizatorul cu un cont GitHub existent, fără parole create în SkillForge. Sesiunea protejează ruta care apelează modelul, iar interfața poate afișa contul conectat. Integrarea intră în primul pas al autentificării, fără bază de date.

## Cont & chei

- Creează aplicația la [GitHub Developer Settings](https://github.com/settings/developers) → **OAuth Apps** → **New OAuth App**.
- Pentru dezvoltare, `Homepage URL` este `http://localhost:3000`, iar `Authorization callback URL` este `http://localhost:3000/api/auth/callback/github`.
- Pentru producție, folosește domeniul Production și callback-ul `https://DOMENIU/api/auth/callback/github`.
- Copiază **Client ID** în `AUTH_GITHUB_ID` și generează **Client Secret** în aplicația GitHub pentru `AUTH_GITHUB_SECRET`. Secretul se afișează o singură dată.
- Generează un secret de sesiune cu `openssl rand -base64 32` pentru `AUTH_SECRET`. Nu îl reutiliza în alt proiect.
- Client ID-ul este config public prin proiectare: identifică aplicația în URL-ul OAuth. Client Secret-ul și secretul de sesiune sunt secrete: primul dovedește identitatea aplicației, al doilea permite verificarea sesiunilor.

## Variabile de mediu

| Variabilă            | Fișier       | Marcaj | Ajunge în browser?                            |
| -------------------- | ------------ | ------ | --------------------------------------------- |
| `AUTH_GITHUB_ID`     | `.env.local` | config | Nu direct; este identificatorul public OAuth  |
| `AUTH_GITHUB_SECRET` | `.env.local` | secret | Nu                                            |
| `AUTH_SECRET`        | `.env.local` | secret | Nu                                            |
| `AUTH_URL`           | `.env.local` | config | Nu; localizează aplicația pentru callback-uri |

Valorile reale se pun numai în `.env.local`:

```dotenv
AUTH_GITHUB_ID="your_github_oauth_client_id"
AUTH_GITHUB_SECRET="your_github_oauth_client_secret"
AUTH_SECRET="your_random_session_secret"
AUTH_URL="http://localhost:3000"
```

În Vercel: **Project → Settings → Environment Variables**. Se adaugă în **Production** și **Preview**, iar opțiunea de secret se bifează numai pentru `AUTH_GITHUB_SECRET` și `AUTH_SECRET`. După modificare se face Redeploy.

## Pași manuali

1. Înregistrează separat callback-ul pentru localhost și domeniul Production în GitHub OAuth App.
2. Nu folosi callback-uri Preview instabile ca soluție permanentă: URL-ul se schimbă la deploy. Pentru Preview este nevoie de un domeniu stabil sau de configurarea manuală a URL-ului de test înaintea fiecărei verificări.
3. Completează variabilele în `.env.local` și repornește serverul de dezvoltare.
4. Dacă se schimbă domeniul Production, actualizează `Homepage URL`, `Authorization callback URL` și `AUTH_URL`.
5. Google este deja în registrul UI ca provider inactiv; activarea lui va necesita `AUTH_GOOGLE_ID`, `AUTH_GOOGLE_SECRET` și callback-ul Google într-un pas ulterior.

## Cost & limite

GitHub OAuth nu adaugă cost per întrebare în SkillForge. Limitele sunt cele ale GitHub OAuth și ale aplicației GitHub; autentificarea nu înlocuiește limita de cost a providerului LLM. Cheile LLM rămân independente și sunt documentate în integrările lor.

## Verificare

Cu variabilele completate, pornește `npm run dev`, deschide **Conectează-te**, alege GitHub și verifică rândul de utilizator din sidebar. Un request direct fără cookie de sesiune trebuie să fie refuzat de ruta de chat:

```powershell
Invoke-WebRequest -Method Post -Uri http://localhost:3000/api/chat -ContentType "application/json" -Body '{"messages":[]}'
```

Rezultatul așteptat este `401`. La o cerere autentificată, terminalul serverului afișează o singură linie cu identificatorul utilizatorului, fără email, nume sau token. La Cancel, callback-ul revine în aplicație cu mesaj în română, iar parametrii de eroare sunt eliminați după afișare.
