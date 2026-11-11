# Documentația CPA

## Ce construim

[`requirements.md`](requirements.md) — **sursa de adevăr**: ce este aplicația și pentru cine, arhitectura pe scurt, fazele, cerințele non-funcționale și non-scopul.

[`plan.md`](plan.md) — planul de transformare și de execuție, pe faze (în engleză). [`adr/`](adr/) — câte o notă scurtă pentru fiecare decizie D1–D9.

Când se schimbă ceva pe drum, se actualizează în `requirements.md`, nu doar în conversație.

## Integrări externe

Fiecare integrare externă are propriul folder, cu pașii manuali pe care trebuie să-i facă omul: cont, generare de chei, click-uri în dashboard-ul lor, costuri. Codul îl scrie agentul; pașii manuali se uită imediat dacă nu-i notează nimeni — la reinstalare, pe alt calculator sau la deploy îi cauți de la zero.

Folderele `anthropic/`, `nextauth/` și `openai/` descriu încă vechea aplicație (SkillForge). Ele se rescriu sau se șterg în Faza 6 și Faza 9, odată cu codul server pe care îl documentează; `render/` și `neon/` apar în Faza 3.

| Integrare | La ce pas a intrat                                      | Link                                         |
| --------- | ------------------------------------------------------- | -------------------------------------------- |
| Vercel    | Faza 4.1 (veche) — publicare și Preview/Production      | [`vercel/README.md`](vercel/README.md)       |
| Anthropic | veche — se șterge în Faza 6                             | [`anthropic/README.md`](anthropic/README.md) |
| OpenAI    | veche — se rescrie în Faza 5 (un singur model, backend) | [`openai/README.md`](openai/README.md)       |
| Auth.js   | veche — se șterge în Faza 6 (identitate anonimă, D3)    | [`nextauth/README.md`](nextauth/README.md)   |

Formatul obligatoriu al unui astfel de fișier: [`_template/README.md`](_template/README.md).

Când se adaugă o integrare, în **același commit** intră și: fișierul `docs/<integrare>/README.md`, rândul din tabelul de mai sus, și variabilele noi în `.env.example`.

**În `docs/` nu se scriu niciodată chei reale** — doar numele variabilelor. Valorile stau exclusiv în `.env.local`, care e gitignorat.

## Convenții pentru agenți

[`../AGENTS.md`](../AGENTS.md) — regulile pe care le urmează agenții AI care lucrează în proiect (fișier canonic; `CLAUDE.md` și `.github/copilot-instructions.md` se generează din el).
