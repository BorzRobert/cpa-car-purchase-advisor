# ADR 0004 — Găzduirea backend-ului

- **Status:** acceptat (2026-11-11)
- **Decizie:** Render (serviciu web Docker, plan gratuit) și Neon Postgres (plan gratuit).
- **Motiv:** nu cer card. Compromis: planul gratuit adoarme, deci există cold start; UI-ul arată starea „Se trezește consilierul…”.
- **Alternative respinse:** Fly.io, Railway, Google Cloud Run (mai bune, dar cer card).
