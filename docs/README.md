# Documentația CPA

## Ce construim

[`requirements.md`](requirements.md) — **sursa de adevăr**: ce este aplicația și pentru cine, arhitectura pe scurt, fazele, cerințele non-funcționale și non-scopul.

[`plan.md`](plan.md) — planul de transformare și de execuție, pe faze (în engleză). [`adr/`](adr/) — câte o notă scurtă pentru fiecare decizie D1–D9 (plus ADR 0010, care înlocuiește modelul din D7).

[`architecture/backend.md`](architecture/backend.md) — cum este construit backend-ul: versiuni fixate, straturi, configurare (inclusiv profilul `prod`), baza de date și migrări, limite, securitate, teste, Docker și CI.

[`architecture/communication.md`](architecture/communication.md) — contractul frontend ↔ backend: sesiune, endpoint-uri, erori, limite, SSE (implementat, consumat de frontend din Faza 6). [`architecture/openapi.json`](architecture/openapi.json) — instantaneul OpenAPI, verificat în CI; din el se generează tipurile frontend-ului (`npm run api:types`).

Când se schimbă ceva pe drum, se actualizează în `requirements.md`, nu doar în conversație.

## Integrări externe

Fiecare integrare externă are propriul folder, cu pașii manuali pe care trebuie să-i facă omul: cont, generare de chei, click-uri în dashboard-ul lor, costuri. Codul îl scrie agentul; pașii manuali se uită imediat dacă nu-i notează nimeni — la reinstalare, pe alt calculator sau la deploy îi cauți de la zero.

| Integrare | La ce pas a intrat                                                   | Link                                         |
| --------- | -------------------------------------------------------------------- | -------------------------------------------- |
| Vercel    | Faza 4.1 (veche) — publicare; Faza 3 — `NEXT_PUBLIC_API_BASE_URL`, CORS | [`vercel/README.md`](vercel/README.md)    |
| Render    | Faza 3 — backend Docker (`render.yaml`)                              | [`render/README.md`](render/README.md)       |
| Neon      | Faza 3 — PostgreSQL de producție, Flyway                             | [`neon/README.md`](neon/README.md)           |
| OpenAI    | Faza 2 — dependență + configurație backend (apeluri din Faza 5)      | [`openai/README.md`](openai/README.md)       |

Formatul obligatoriu al unui astfel de fișier: [`_template/README.md`](_template/README.md).

Când se adaugă o integrare, în **același commit** intră și: fișierul `docs/<integrare>/README.md`, rândul din tabelul de mai sus, și variabilele noi în `.env.example`.

**În `docs/` nu se scriu niciodată chei reale** — doar numele variabilelor. Valorile stau exclusiv în `.env.local`, care e gitignorat.

## Convenții pentru agenți

[`../AGENTS.md`](../AGENTS.md) — regulile pe care le urmează agenții AI care lucrează în proiect (fișier canonic; `CLAUDE.md` și `.github/copilot-instructions.md` se generează din el).
