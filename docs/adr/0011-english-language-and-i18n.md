# ADR 0011 — English for code and docs, Romanian + English UI

- **Status:** accepted (2026-11-18); supersedes the language part of [ADR 0001](0001-piata-moneda-limba.md)
- **Decision:** code, comments, commit messages, `docs/`, `README.md` and agent instructions are in English. UI copy lives in per-locale message catalogues (`ro` default, `en` supported); the user switches language in Preferințe → General. The advisor answers in the user's language.
- **Reason:** one working language for the repository and its tooling; the UI must stay Romanian for the target market while becoming usable in English.
- **Unchanged:** target market (Romania/EU), EUR prices with RON conversion.
- **Rejected alternatives:** English-only UI; keeping Romanian docs and comments.
