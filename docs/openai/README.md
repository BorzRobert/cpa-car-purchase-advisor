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
- Compatibilitatea cu Spring AI 2.0.2 este dedusă din documentația OpenAI, **nu încă testată cu o cheie reală**; se verifică la prima rulare (vezi „Verificare”). Schimbarea tehnică a modelului este o singură variabilă (`CPA_LLM_MODEL`); schimbarea deciziei cere un ADR nou.
- Nu există utilizare gratuită garantată; limitele (rate limits, `429`) depind de cont, proiect și nivel de utilizare.
- Ce umflă factura: istoricul conversației și promptul de sistem se retrimit la fiecare mesaj; de aceea backend-ul are limită de rată, buget zilnic cu întrerupător global, lungime maximă a promptului și `max_output_tokens`.

## Verificare

```bash
cd backend
./mvnw spring-boot:run
curl http://localhost:8080/api/v1/health
```

Fără cheie: `{"status":"UP","service":"cpa-backend","version":"...","advisorConfigured":false}`. Cu cheia reală în `backend/.env`: `advisorConfigured` devine `true` (valoarea cheii nu apare niciodată în răspuns). Apelul real către model se verifică de la Faza 5.
