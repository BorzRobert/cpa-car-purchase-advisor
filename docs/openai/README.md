# OpenAI

## Ce face

OpenAI este al doilea provider de răspunsuri pentru SkillForge. Utilizatorul poate cere o a doua perspectivă pentru mesajul curent, fără să schimbe providerul implicit Anthropic. Integrarea a intrat în Faza 5: comutatorul, streamingul și măsurarea consumului funcționează.

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

- La data verificării, 16 septembrie 2026, registry-ul SkillForge folosește pentru modelele activate **GPT-5: $1,25 / $10** și **GPT-5 mini: $0,25 / $2** USD per milion de tokeni (intrare / ieșire). Verifică [pagina oficială de prețuri](https://openai.com/api/pricing/) înainte de folosire, deoarece tarifele și modelele se schimbă.
- Nu există o promisiune de utilizare gratuită pentru API; disponibilitatea și limitele depind de cont, proiect și nivelul de utilizare.
- Rate limits sunt stabilite pe proiect și model și pot produce răspunsuri `429`. SkillForge are suplimentar o limită locală de **10 cereri pe minut per client**, cu mesaj și moment de reluare; contorul în memorie nu este o măsură de securitate distribuită.
- Fiecare mesaj trimite istoricul conversației și system prompt-ul către modelul ales, iar UI-ul arată tokenii de intrare/ieșire și costul. Cache-ul protejează doar retrimiterile identice; regenerarea ocolește cache-ul.

## Verificare

```bash
npm run dev
```

În composer, deschide selectorul, alege **OpenAI → GPT-5 mini** și trimite un mesaj. Răspunsul trebuie să apară în streaming. Dacă `OPENAI_API_KEY` lipsește, opțiunea rămâne vizibilă, dezactivată și arată motivul; cererea directă către API întoarce `400` cu un mesaj clar.
