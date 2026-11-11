# OpenAI

## Ce face

OpenAI este singurul furnizor de model al consilierului CPA. Backend-ul Java (Spring AI) este singurul loc care îl apelează, cu **un singur model** configurat într-o singură proprietate (`cpa.llm.model`, [ADR 0007](../adr/0007-model-openai.md)). Fără cheie aplicația pornește, dar consilierul răspunde `503` cu tipul `advisor-not-configured`, iar `GET /api/v1/health` raportează `advisorConfigured: false`.

A intrat în **Faza 2** (dependența și configurația, fără apeluri reale); apelurile către model apar în Faza 5.

> Vechea integrare din frontend (`OPENAI_API_KEY` în `frontend/.env.example`, `src/lib/providers.server.ts`) aparține aplicației SkillForge și se șterge în Faza 6. Cheia nu are ce căuta în frontend.

## Cont & chei

- Creează contul la [platform.openai.com](https://platform.openai.com/); pentru apeluri API ai nevoie de un proiect cu billing configurat.
- În interfață: **Dashboard → API keys → Create new secret key**. Folosește o cheie dedicată proiectului CPA.
- Cheia are nevoie doar de acces la API-urile de modele; nu o pune în browser, în URL sau în loguri.
- Valoarea completă este afișată o singură dată. Dacă a fost expusă, revoc-o și generează alta (nu șterge doar linia din fișier).
- La crearea cheii, setează o **limită lunară de cheltuieli** în dashboard (Settings → Limits).

## Variabile de mediu

| Variabilă         | Fișier / loc                         | Ajunge în browser?                       |
| ----------------- | ------------------------------------ | ---------------------------------------- |
| `OPENAI_API_KEY`  | `backend/.env`, dashboard-ul gazdei  | NU (există doar în backend)              |
| `CPA_LLM_MODEL`   | `backend/.env`, dashboard-ul gazdei  | NU (opțională; implicit `gpt-5-mini`)    |

Rândurile din `backend/.env.example` (fără valori reale):

```dotenv
OPENAI_API_KEY=your_openai_api_key
CPA_LLM_MODEL=gpt-5-mini
```

`your_openai_api_key` este un substituent: backend-ul îl tratează ca „fără cheie”. În producție se configurează aceleași variabile în dashboard-ul Render ([`../render/README.md`](../render/README.md)); până în Faza 5/9 `OPENAI_API_KEY` rămâne substituentul, nu o cheie reală.

## Pași manuali

1. Creează cheia în dashboard și pune-o în `backend/.env` (copie din `.env.example`); repornește backend-ul.
2. Setează limita lunară de cheltuieli în contul OpenAI.
3. **Decide modelul înainte de Faza 5** — vezi „Cost & limite”: `gpt-5-mini` are dată de oprire.
4. În producție, adaugă variabilele în dashboard-ul gazdei și refă deployment-ul.

## Cost & limite

Verificat la **2026-11-11** pe [pagina modelului](https://platform.openai.com/docs/models/gpt-5-mini) și pe [pagina de deprecieri](https://platform.openai.com/docs/deprecations):

- `gpt-5-mini` (ID-ul din D7): intrare **$0,25**, intrare din cache $0,025, ieșire **$2** per milion de tokeni; suportă streaming, apelare de unelte și ieșire structurată.
- **Atenție: `gpt-5-mini` este marcat _deprecated_, cu oprire pe 11 decembrie 2026**; înlocuitorul recomandat de OpenAI este `gpt-5.6-terra`. Valoarea implicită rămâne `gpt-5-mini` ca să respecte D7, dar înainte de Faza 5 trebuie confirmat modelul final; o schimbare de decizie cere un ADR nou. Schimbarea tehnică e o singură variabilă (`CPA_LLM_MODEL`).
- Nu există utilizare gratuită garantată; limitele (rate limits, `429`) depind de cont, proiect și nivel de utilizare.
- Ce umflă factura: istoricul conversației și promptul de sistem se retrimit la fiecare mesaj; de aceea backend-ul are (Faza 4–5) limită de rată, buget zilnic cu întrerupător global, lungime maximă a promptului și `max_output_tokens`.

## Verificare

```bash
cd backend
./mvnw spring-boot:run
curl http://localhost:8080/api/v1/health
```

Fără cheie: `{"status":"UP","service":"cpa-backend","version":"...","advisorConfigured":false}`. Cu cheia reală în `backend/.env`: `advisorConfigured` devine `true` (valoarea cheii nu apare niciodată în răspuns). Apelul real către model se verifică de la Faza 5.
