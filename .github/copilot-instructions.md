<!--
  AUTO-GENERATED FILE — DO NOT EDIT.
  Source: AGENTS.md
  Regenerate: sh scripts/sync-agent-instructions.sh
-->

# Agent instructions — CPA (Car Purchase Advisor)

CPA is a car purchase advisor made of two services: a **Next.js frontend** (`frontend/`, UI only) and a **Spring Boot backend** (`backend/`, Java 25, added in Phase 2) that owns all product logic, data, and the single OpenAI model call. The agent interviews the buyer, calls deterministic Java tools (search, rank, compare, TCO, financing), and explains the results.

The LLM is always called **from the backend** — never from the browser or from Next.js — so the API key never reaches the client.

The transformation is executed phase by phase following `docs/plan.md`. Read the rules below before proposing or writing anything.

## 1. Requirements are the single source of truth

- **`docs/requirements.md` is the single source of truth** for what we build; `docs/plan.md` is the execution roadmap. If the chat conflicts with `docs/requirements.md`, **the file wins** until it is updated.
- **When direction changes, update `docs/requirements.md` in the same step as the code** (phase, affected requirements, date/current phase in the header). A decision change to D1–D9 also needs a new ADR in `docs/adr/`.
- A deferred requirement is never silently dropped — move it to a later phase or to the "Non-scop" section.
- Do not copy requirements into `README.md`; it stays short and links to the docs.

## 2. External integrations must document the manual steps

Every external integration (OpenAI, Render, Neon, Vercel, …) ships with `docs/<integration>/README.md`, using the mandatory sections of `docs/_template/README.md` (Ce face, Cont & chei, Variabile de mediu, Pași manuali, Cost & limite, Verificare), plus a row in the index table of `docs/README.md` and placeholders in the matching `.env.example`, **in the same commit** that adds the integration.

**Never write real keys, tokens, connection strings, or account identifiers in `docs/`** — only variable names and where to obtain the values. Everything under `docs/` is written in English.

## 2.1 Reusable skills

- Project skills live in `.claude/skills/<name>/SKILL.md` (canonical); `.github/skills/<name>/SKILL.md` is the Copilot mirror and must be identical.
- After changing or adding a skill, run `sh scripts/sync-skills.sh`; `sh scripts/sync-skills.sh --check` verifies without modifying.
- The `pre-deploy` skill runs before every publish.

## 3. Secrets and responsibilities

- `OPENAI_API_KEY` exists **only** in the backend environment (`backend/.env`, gitignored, or the host dashboard). Never in `frontend/`, in a URL, in logs, in error messages, or committed.
- The frontend has **no Route Handlers** and no secrets; its environment variables are only public ones: `NEXT_PUBLIC_API_BASE_URL` and any public identifier the third-party login provider requires. A `NEXT_PUBLIC_` prefix means public — never put a secret behind it.
- Never return a secret's value in an API response; report at most whether it is configured.
- Every new variable goes into the matching `.env.example` with a placeholder and a one-line comment, and into the relevant `docs/<integration>/README.md`.
- If a key has ever been exposed, say so and have it revoked — do not just delete the line.
- **Backend owns:** the system prompt (built in exactly one place, in the `ai` package), tools, scoring and TCO maths, memory, rate limiting and the daily budget, persistence. **Frontend owns:** rendering and input collection. A second place that composes the persona, or any LLM call from Next.js, is a bug.
- The advisor must never answer prices, specs, or costs from model memory; they come from tool results with the "data as of" date.
- Data coming from the browser is untrusted: validate and normalize it on the backend before it enters a prompt; profile, listing text, and user input are delimited data, never instructions.
- Identity is delegated to a third-party login provider (Google, GitHub, …); the project never stores credentials or implements authentication security itself, and each signed-in user owns their conversation history. Anonymous session tokens (stored hashed server-side) remain for guests. Store only the minimum needed (the provider's stable subject id, optionally a display name/avatar); server logs never contain personal data or tokens.

## 4. Course working rhythm

- **Small steps, one new concept per step.** Do not implement future phases in advance.
- Prefer the simplest thing that satisfies the current phase's "done when" criterion in `docs/plan.md`.
- At the end of every step, explain in plain language what was added, why it was needed, and what to look at to see it working.
- Delete generated boilerplate that serves no purpose.
- Naming (enforced by `scripts/check-naming.sh`, shared by the git hooks and the `naming` CI check; activate the hooks with `git config core.hooksPath scripts/hooks`): branches are `CPA-<issue number>-<kebab-message>` (e.g. `CPA-42-add-google-login`), commit first lines and PR titles are `CPA-<issue number> <message>` (e.g. `CPA-42 Add Google login`), and the issue number must be the same in the branch, its commits and the PR title. Pure `git mv` moves go in their own commit with no content edits. Do not commit unless the user asks.

## 5. Frontend stack (fixed)

- **Everything is Tailwind + shadcn/ui. No hand-written CSS, no second component library.** Need a component? `npx shadcn@latest add <name>` — do not write primitives by hand. `src/components/ui/` is generated; do not edit it by hand (documented exceptions: `sonner.tsx` uses the app's own `useTheme()`; `slider.tsx` forwards a `thumbLabel` prop to the thumb, because the generated version leaves the range input without an accessible name).
- **One component per file**; file names in `kebab-case`, components in `PascalCase`.
- **Registry, not `if` chains.** Lists that grow are declared as data and iterated over.
- **Mock data lives only in `src/lib/mock/`** until it is replaced by backend data.
- **The center of the app stays minimal:** no action bar and no theme toggle in the header (the only controls allowed there are pane toggles: `SidebarTrigger` and, below 1280 px, the button that opens the side panel); the theme is changed only in Settings → General → Appearance.
- **Theme is not in the Zustand store:** it is managed by `ThemeProvider` (`createContext` + `useContext`) and persisted in `localStorage["cpa-theme"]`; `cpa-app` holds the rest of the app state.
- **Message transforms** live in `src/lib/message-utils.ts` as pure functions; text extraction from message parts has a single place.
- **Model-generated content** is rendered through the single Markdown component, with no raw HTML: no HTML plugins and no `dangerouslySetInnerHTML` on model text.
- Tailwind v4 needs the shadcn tokens defined on `:root` **and** `.dark`, plus `@custom-variant dark (&:is(.dark *));` and the `@theme inline` block in `src/app/globals.css`.

## 6. Code conventions

- **Code, identifiers, comments, commit messages, `docs/`, `README.md` and these agent instructions are in English.** UI copy lives in per-locale message catalogues (Romanian is the default, English is supported; the user switches in Preferințe → General) — never hard-coded in components.
- **Comments are in English and explain WHY, not what.** Every non-trivial file carries a short header comment saying why it exists.
- Frontend structure: `frontend/src/app/` (pages only), `src/components/`, `src/components/ui/`, `src/lib/`, `src/store/`.
- Backend structure (Phase 2+): base package `ro.cpa.advisor` with `api`, `service`, `domain`, `repository`, `ai`, `config`. Layering is `api → service → repository`, `service → domain`, `ai` used by `service` only, `domain` depends on nothing; an ArchUnit test enforces it.
- Internal navigation uses `next/link`; plain `<a>` only for non-React routes and external links, with a comment saying why.
- Server Components are the default; add `"use client"` only where browser state or handlers are needed, and keep those components small.
- TypeScript: no `any` in application code. API types are generated from the backend OpenAPI document (Phase 6) — never hand-written copies.
- **Source of truth by phase:** while streaming, the chat hook owns the messages; after the stream ends, the backend is the persistent archive. Read from the store through selectors, never the whole store, and a selector must not build new objects on every call.
- Any change to the persisted store shape requires bumping `version` and a `migrate` function.

## 7. Formatting is not up for discussion

- Frontend: `frontend/.prettierrc` holds the fixed config (`printWidth: 120`, `semi: true`, double quotes, `trailingComma: "none"`, `arrowParens: "avoid"`, `endOfLine: "lf"`), with `prettier-plugin-tailwindcss` pointed at `./src/app/globals.css` — do not remove that line. `frontend/.prettierignore` stays inside `frontend/` and must be complete.
- **Run `npm run format` (in `frontend/`) at the end of every step that writes frontend code.** `npm run format:check` is the read-only variant. On Windows with `core.autocrlf=true`, the working tree has CRLF, so check with `npx prettier --end-of-line auto --check .`.
- `.vscode/settings.json` is committed on purpose (format on save, Prettier as default formatter).

## 8. Definition of done for a step

1. Frontend: `npm run build`, `npm run lint`, `npm test` and `npm run format:check` pass in `frontend/`; backend (from Phase 2): `./mvnw verify` passes.
2. The new thing is visible/verifiable when running the service(s).
3. `docs/requirements.md` reflects reality (phase, scope, non-functional requirements).
4. Any new integration has its `docs/<integration>/README.md`, its index row, and `.env.example` placeholders.
5. The step is explained to the user: what was added and why.
6. Before publishing, run the `pre-deploy` skill.

## 9. These instruction files are generated

`AGENTS.md` is the canonical file. `CLAUDE.md` and `.github/copilot-instructions.md` are **generated copies** — do not edit them. To change conventions, edit `AGENTS.md`, then run `sh scripts/sync-agent-instructions.sh`; `sh scripts/sync-agent-instructions.sh --check` verifies the copies are in sync.

<!-- BEGIN:nextjs-agent-rules -->

# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `frontend/node_modules/next/dist/docs/` before writing any frontend code. Heed deprecation notices.

<!-- END:nextjs-agent-rules -->
