# ADR 0010 — Înlocuitorul lui `gpt-5-mini` (modelul OpenAI)

- **Status:** acceptat (2026-11-11, confirmat de proprietar). Înlocuiește **doar partea de model** din [ADR 0007](0007-model-openai.md); restul (un singur model, dintr-o singură proprietate `cpa.llm.model`, fără selector) rămâne valabil.
- **Context:** `gpt-5-mini` este _deprecated_ la OpenAI, cu oprire pe **2026-12-11** ([pagina de deprecieri](https://developers.openai.com/api/docs/deprecations)); înlocuitorul recomandat de OpenAI este `gpt-5.6-terra`. Backend-ul apelează modelul prin Spring AI 2.0.2 ([ADR 0008](0008-spring-ai.md)), al cărui `OpenAiChatModel` folosește **doar Chat Completions** (`v1/chat/completions`), nu Responses API. Sursa tuturor cifrelor: paginile oficiale de [modele](https://developers.openai.com/api/docs/models) și de [prețuri](https://developers.openai.com/api/docs/pricing), verificate la 2026-11-11.
- **Decizie:** `gpt-5.6-terra` (valoarea implicită a `cpa.llm.model`, adică a variabilei `CPA_LLM_MODEL`). Reasoning effort rămâne implicit (`medium`) până când măsurătorile din Faza 5 arată că `low` ajunge; dacă se schimbă, se face prin configurație, nu în cod.
- **Motiv:**
  - Este înlocuitorul oficial al lui `gpt-5-mini` (aceeași treaptă „mini”), fără dată de oprire anunțată.
  - Pagina lui declară suport pentru **Chat Completions, streaming și function calling** fără restricții, deci apelarea de unelte (inima consilierului) funcționează cu Spring AI 2.0.2 așa cum este.
  - Cost: $2 / $12 per milion de tokeni (intrare / ieșire; $0,20 intrare din cache). Un răspuns tipic, cu ~6.000 de tokeni de intrare și ~600 de ieșire, costă ≈ $0,019 (față de ≈ $0,003 la `gpt-5-mini`); bugetul zilnic pe sesiune (60.000 de tokeni) rămâne plafonul real de cheltuială.
- **Alternative respinse (prețuri per milion de tokeni, intrare / ieșire):**

| Model                | Preț          | De ce nu                                                                                                                                                                                  |
| -------------------- | ------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `gpt-6.1-terra`      | $1 / $5       | Mai ieftin și mai nou, dar pagina lui spune „**Tool calling requires the Responses API**”, iar Spring AI 2.0.2 nu apelează Responses API: apelarea de unelte nu poate fi garantată prin Chat Completions. |
| `gpt-6-luna`         | $0,10 / $0,50 | Cel mai ieftin, dar în Chat Completions apelează unelte doar cu `reasoning_effort=none`; calitatea apelării de unelte pe 8–10 unelte și multe ture este exact riscul pe care îl evităm.    |
| `gpt-6.1-sol`        | $2 / $10      | Aceeași restricție Responses API ca la `gpt-6.1-terra`, iar pentru un consilier cu unelte deterministe capacitatea suplimentară nu justifică prețul.                                      |
| `gpt-5.6-luna`       | $0,20 / $1,20 | Treapta „nano”: prea slab pentru un agent care alege între mai multe unelte.                                                                                                              |
| `gpt-5.6-sol`        | $4 / $20      | De 2 ori mai scump fără câștig necesar.                                                                                                                                                  |

- **Riscuri și plan de ieșire:**
  - Nicio cheie reală nu a fost folosită încă: compatibilitatea `gpt-5.6-terra` cu Spring AI 2.0.2 este dedusă din documentația OpenAI și se confirmă la prima rulare cu o cheie reală (comanda de verificare este în [`../openai/README.md`](../openai/README.md)); testele din CI folosesc un model înregistrat, fără cheie.
  - Dacă se ajunge la `gpt-6.1-terra` (mai ieftin), calea este un Spring AI cu suport Responses API (la 2026-11-11 există doar `2.1.0-RC1`/`RC2`, nepublicate ca stabile) și un ADR nou.
  - Schimbarea tehnică a modelului este o singură variabilă (`CPA_LLM_MODEL`); schimbarea deciziei cere un ADR nou.
