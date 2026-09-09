# OpenAI

## Ce face

OpenAI este al doilea provider de răspunsuri pentru SkillForge. Utilizatorul poate cere o a doua perspectivă pentru mesajul curent, fără să schimbe providerul implicit Anthropic. Integrarea a intrat în Faza 5, parțial: comutatorul și apelul funcționează; costurile și număratul tokenilor rămân pentru un pas separat.

## Cont & chei

- Creează contul la [platform.openai.com](https://platform.openai.com/); pentru apeluri API ai nevoie de un proiect cu billing configurat.
- În interfață: **Dashboard → API keys → Create new secret key**.
- Cheia are nevoie doar de acces pentru API-urile de modele ale proiectului; nu o folosi în browser și nu o pune în URL.
- Valoarea completă este afișată o singură dată. Dacă a fost expusă, revoc-o și generează una nouă.

## Variabile de mediu

| Variabilă        | Fișier       | Ajunge în browser?              |
| ---------------- | ------------ | ------------------------------- |
| `OPENAI_API_KEY` | `.env.local` | NU (fără prefix `NEXT_PUBLIC_`) |

În `.env.example` există linia:

```dotenv
OPENAI_API_KEY="your_openai_api_key"
```

În producție: configurează aceeași variabilă în **Vercel → Project → Settings → Environment Variables**, pentru **Production** și **Preview**, apoi fă **Redeploy**.

## Pași manuali

1. Creează cheia în dashboard și pune-o în `.env.local`; repornește serverul de dezvoltare.
2. Adaugă variabila în Vercel pentru ambele medii și refă deployment-ul.
3. Verifică în dashboard limitele de utilizare și billing-ul proiectului.

## Cost & limite

- OpenAI facturează API-ul în funcție de model și de tokenii de intrare și ieșire; prețurile se schimbă, deci verifică [pagina oficială de prețuri](https://openai.com/api/pricing/) la data folosirii.
- Nu există o promisiune de utilizare gratuită pentru API; disponibilitatea și limitele depind de cont, proiect și nivelul de utilizare.
- Rate limits sunt stabilite pe proiect și model și pot produce răspunsuri `429`.
- La acest pas nu măsurăm tokenii și nu comparăm costurile; fiecare mesaj trimite istoricul conversației și system prompt-ul către modelul ales.

## Verificare

```bash
npm run dev
```

În composer, deschide selectorul, alege **OpenAI → GPT-5 mini** și trimite un mesaj. Răspunsul trebuie să apară în streaming. Dacă `OPENAI_API_KEY` lipsește, opțiunea rămâne vizibilă, dezactivată și arată motivul; cererea directă către API întoarce `400` cu un mesaj clar.
