# ADR 0007 — Modelul OpenAI

- **Status:** acceptat (2026-11-11); **partea de model (`gpt-5-mini`) este înlocuită de [ADR 0010](0010-model-openai-inlocuitor-gpt-5-mini.md)** (deprecat, oprire 2026-12-11). Principiul „un singur model dintr-o singură proprietate” rămâne valabil.
- **Decizie:** `gpt-5-mini`, un singur model dintr-o singură proprietate (`cpa.llm.model`); nu există selector de model nicăieri. Alegerea finală se confirmă la cumpărarea cheii.
- **Motiv:** raport bun între calitatea apelării de unelte și cost.
- **Alternative respinse:** `gpt-5` (mai scump).
