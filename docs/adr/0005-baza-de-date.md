# ADR 0005 — Baza de date

- **Status:** acceptat (2026-11-11)
- **Decizie:** JPA + Flyway; H2 pentru dezvoltare locală și teste simple, PostgreSQL în producție. Aceleași migrări rulează pe ambele, iar Testcontainers acoperă particularitățile Postgres în CI.
- **Motiv:** discurile gazdelor gratuite sunt efemere, deci un fișier H2/SQLite s-ar pierde la fiecare deploy.
- **Alternative respinse:** doar H2; SQLite.
