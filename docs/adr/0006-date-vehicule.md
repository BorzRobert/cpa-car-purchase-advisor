# ADR 0006 — Date despre vehicule

- **Status:** acceptat (2026-11-11)
- **Decizie:** catalog curatoriat (~100–200 de variante ale modelelor populare), ținut în repo și încărcat prin Flyway/CSV, cu data „date la” afișată în UI.
- **Motiv:** modelul nu are voie să răspundă din memorie cu specificații sau prețuri; răspunsurile vin din unelte deterministe peste catalog.
- **Alternative respinse:** API live (rămâne opțional pentru rechemări), memoria LLM.
