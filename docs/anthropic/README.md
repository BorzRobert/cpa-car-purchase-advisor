# Anthropic

## Ce face

Anthropic furnizează modelul de limbaj care răspunde în conversația deschisă din SkillForge. A intrat în Faza 3 prin Vercel AI SDK, iar apelul se face exclusiv din `src/app/api/chat/route.ts`, unde răspunsul este transmis treptat către interfață.

## Cont & chei

- Creează sau accesează contul la https://console.anthropic.com/.
- În consolă, deschide `Settings → API Keys → Create Key`, denumește cheia pentru acest proiect și confirmă crearea.
- Cheia trebuie să poată face apeluri la Messages API; nu adăuga cheia în browser sau într-un proiect public.
- Valoarea completă se afișează o singură dată. Păstreaz-o într-un manager de parole, nu în documentație sau în Git.

## Variabile de mediu

| Variabilă           | Fișier       | Ajunge în browser?              |
| ------------------- | ------------ | ------------------------------- |
| `ANTHROPIC_API_KEY` | `.env.local` | NU (fără prefix `NEXT_PUBLIC_`) |

Linia adăugată în `.env.example` este:

```dotenv
ANTHROPIC_API_KEY="your_anthropic_api_key"
```

În producție, adaugă aceeași variabilă din `Vercel → Project → Settings → Environment Variables`, pentru mediile în care rulează aplicația.

## Pași manuali

1. Creează contul Anthropic, adaugă metoda de plată sau creditele necesare și generează cheia din consolă.
2. Creează `.env.local` din `.env.example`, pune valoarea reală numai în `ANTHROPIC_API_KEY` și repornește `npm run dev`.
3. Configurează limite de cheltuială și alerte în consola Anthropic înainte de utilizarea regulată.
4. La mutarea pe alt calculator sau la deploy, adaugă din nou cheia în secret store-ul acelui mediu; nu copia `.env.local` în Git.

## Cost & limite

La data verificării, 7 septembrie 2026, Anthropic facturează separat tokenii de intrare și ieșire. Pentru modelul implicit Claude Opus 5 din registru, prețul standard este **$5 USD / milion de tokeni de intrare** și **$25 USD / milion de tokeni de ieșire**. Sursa este pagina oficială https://platform.claude.com/docs/en/about-claude/pricing; verific-o înainte de activare, deoarece prețurile și disponibilitatea modelelor se pot schimba.

Factura crește cu fiecare mesaj trimis ca istoric, cu răspunsurile lungi și cu reîncercările; în această fază nu există încă limitare proprie de tokeni sau rate limiting pe utilizator. Anthropic menționează credite mici pentru conturile noi, nu un plan API gratuit permanent. Prompt caching, nefolosit aici, are tarife diferite față de intrarea standard.

Anthropic aplică limite de cereri și tokeni în funcție de nivelul contului. Când sunt depășite, SkillForge afișează un mesaj generic, fără detalii despre cont.

## Verificare

1. Rulează `npm run dev` după ce ai configurat `.env.local`.
2. Deschide aplicația, scrie o întrebare și apasă Enter. Mesajul trebuie să apară imediat, iar răspunsul Claude trebuie să curgă treptat în aceeași pagină.
3. Cu cheia absentă, aceeași acțiune trebuie să afișeze în interfață `Cheia nu e configurată.`, iar serverul rămâne pornit.
4. Dacă apare mesajul generic despre model, verifică în consolă cheia, accesul la model și rate limit-ul; nu copia eroarea brută sau cheia în issue-uri ori loguri.
