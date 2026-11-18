# ADR 0003 — Identitate: sesiune anonimă

- **Status:** acceptat (2026-11-11)
- **Decizie:** `POST /api/v1/sessions` întoarce un token opac; backend-ul îl păstrează doar hash-uit, iar frontend-ul îl trimite ca `Authorization: Bearer`. Auth.js (GitHub) se elimină.
- **Motiv:** fără date personale de scurs, fricțiune mică; cookie-urile de terți sunt blocate între `*.vercel.app` și backend. Abuzul de cost se oprește prin rate limit și buget zilnic.
- **Alternative respinse:** păstrarea autentificării GitHub.
- **Extins în Faza 10:** login terț (autentificare delegată) cu istoric per utilizator; sesiunea anonimă rămâne pentru invitați. Alegerea furnizorului primește un ADR propriu înainte de implementare.
