# OpenAI

## Ce face

OpenAI este singurul furnizor de model al consilierului CPA. Backend-ul Java (Spring AI) este singurul loc care îl apelează, cu **un singur model** configurat într-o singură proprietate (`cpa.llm.model`, [ADR 0007](../adr/0007-model-openai.md)). Fără cheie aplicația pornește, dar consilierul răspunde `503` cu tipul `advisor-not-configured`, iar `GET /api/v1/health` raportează `advisorConfigured: false`.

A intrat în **Faza 2** (dependența și configurația); primul apel real către model este în **Faza 5a** (`POST /api/v1/conversations/{id}/messages`, în streaming). CI nu are cheie: testele folosesc un model înregistrat (`RecordedChatModel`), deci verificarea cu modelul real este manuală (vezi „Verificare”).

> Cheia nu are ce căuta în frontend: Next.js nu apelează niciun model (vechea integrare din frontend a fost ștearsă în Faza 6). Pentru o rulare locală cu modelul real, cheia se exportă în shell înainte de `docker compose up` (vezi comentariile din `docker-compose.yml`) sau stă în `backend/.env`.

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
| `CPA_LLM_MODEL`   | `backend/.env`, dashboard-ul gazdei  | NU (opțională; implicit `gpt-5.6-terra`) |

Rândurile din `backend/.env.example` (fără valori reale):

```dotenv
OPENAI_API_KEY=your_openai_api_key
CPA_LLM_MODEL=gpt-5.6-terra
```

`your_openai_api_key` este un substituent: backend-ul îl tratează ca „fără cheie”. În producție se configurează aceleași variabile în dashboard-ul Render ([`../render/README.md`](../render/README.md)); până în Faza 5/9 `OPENAI_API_KEY` rămâne substituentul, nu o cheie reală.

## Pași manuali

1. Creează cheia în dashboard și pune-o în `backend/.env` (copie din `.env.example`); repornește backend-ul.
2. Setează limita lunară de cheltuieli în contul OpenAI.
3. **Modelul:** valoarea implicită este `gpt-5.6-terra` ([ADR 0010](../adr/0010-model-openai-inlocuitor-gpt-5-mini.md)); `gpt-5-mini` se oprește la 2026-12-11. Dacă `CPA_LLM_MODEL` este setat în dashboard-ul gazdei la o valoare veche, schimb-o sau șterge-o.
4. În producție, adaugă variabilele în dashboard-ul gazdei și refă deployment-ul.

## Cost & limite

Verificat la **2026-11-11** pe [modele](https://developers.openai.com/api/docs/models), [prețuri](https://developers.openai.com/api/docs/pricing) și [deprecieri](https://developers.openai.com/api/docs/deprecations) (prețuri per milion de tokeni, intrare / intrare din cache / ieșire):

| Model (ID)       | Preț                  | Observație pentru CPA                                                                                          |
| ---------------- | --------------------- | -------------------------------------------------------------------------------------------------------------- |
| `gpt-5.6-terra`  | $2 / $0,20 / $12      | **Ales** ([ADR 0010](../adr/0010-model-openai-inlocuitor-gpt-5-mini.md)): Chat Completions + streaming + function calling |
| `gpt-6.1-terra`  | $1 / $0,05 / $5       | „Tool calling requires the Responses API” — Spring AI 2.0.2 apelează doar Chat Completions                     |
| `gpt-6-luna`     | $0,10 / $0,01 / $0,50 | În Chat Completions apelează unelte doar cu `reasoning_effort=none`                                            |
| `gpt-6.1-sol`    | $2 / $0,10 / $10      | Aceeași restricție Responses API ca la `gpt-6.1-terra`                                                         |
| `gpt-5-mini`     | $0,25 / $0,025 / $2   | **Deprecated, oprire 2026-12-11** (înlocuitor recomandat: `gpt-5.6-terra`)                                     |

- Un răspuns tipic (~6.000 de tokeni de intrare, ~600 de ieșire) costă ≈ $0,019 cu `gpt-5.6-terra`; bugetul zilnic pe sesiune (60.000 de tokeni) și cel global rămân plafonul de cheltuială. Costul real al unui răspuns este scris în jurnal din Faza 5.
- Compatibilitatea cu Spring AI 2.0.2 a fost **verificată pe producție la 2026-11-12** (vezi „Verificare”): `gpt-5.6-terra` prin Chat Completions funcționează cu streaming, raportarea consumului și apelarea de unelte. Schimbarea tehnică a modelului este o singură variabilă (`CPA_LLM_MODEL`); schimbarea deciziei cere un ADR nou.
- Nu există utilizare gratuită garantată; limitele (rate limits, `429`) depind de cont, proiect și nivel de utilizare.
- Ce umflă factura: istoricul conversației și promptul de sistem se retrimit la fiecare mesaj; de aceea backend-ul are limită de rată, buget zilnic cu întrerupător global, lungime maximă a promptului și `max_output_tokens`.

## Verificare

```bash
cd backend
./mvnw spring-boot:run
curl http://localhost:8080/api/v1/health
```

Fără cheie: `{"status":"UP","service":"cpa-backend","version":"...","advisorConfigured":false}`, iar `POST …/messages` răspunde `503 advisor-not-configured`. Cu cheia reală în `backend/.env`: `advisorConfigured` devine `true` (valoarea cheii nu apare niciodată în răspuns).

**Apelul real către model** (manual, o dată, cu cheia ta; CI nu are cheie). Cu backend-ul pornit și cheia în `backend/.env`:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/sessions | sed -E 's/.*"token":"([^"]+)".*/\1/')
CONV=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/conversations | sed -E 's/.*"id":"([^"]+)".*/\1/')
curl -N -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -H "Accept: text/event-stream" \
  -d '{"content":"Salut! Caut o masina pentru oras."}' http://localhost:8080/api/v1/conversations/$CONV/messages
```

Rezultat așteptat: evenimente `meta`, apoi `token` (text în română), apoi `done` cu `usage` nenul; în jurnalul backend-ului apare o linie `Advisor answer: model=gpt-5.6-terra … inputTokens=… outputTokens=… costUsd=…` cu `estimated=false`. Dacă în loc de asta apare `event: error`, cauza exactă nu este în răspuns (intenționat): vezi jurnalul (`Model call failed: <tip de excepție>`). Aceasta este și verificarea ipotezelor din [ADR 0010](../adr/0010-model-openai-inlocuitor-gpt-5-mini.md): `gpt-5.6-terra` prin Chat Completions, cu `max_completion_tokens` și raportarea consumului în flux.

**Rezultat pe producție (2026-11-12):** `advisorConfigured: true`; scenariul din [`../demo-prompt.txt`](../demo-prompt.txt) (profil, clasament, TCO, listă scurtă, finanțare) a rulat de două ori pe backend-ul de pe Render, cu `done` la fiecare mesaj și uneltele așteptate apelate. Un scenariu de 4 mesaje consumă ~60.000 de tokeni de intrare+ieșire, adică tot bugetul zilnic al unei sesiuni (`cpa.limits.daily-token-budget-per-session`); după el, sesiunea primește `429 budget-exceeded` până la ziua următoare sau până la „Șterge-mi datele”.
