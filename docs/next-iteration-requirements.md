# CPA — Next iteration requirements (input for a future implementation plan)

> **Status: proposal / backlog, not yet accepted.** Source: the owner's draft list (`Progress.txt`), reconciled with the state of the repository on 2026-11-18 (Phase 9 of `plan.md` delivered; app live on Vercel + Render + Neon).
> This file is written in English (like `plan.md`) because it is agent input, not product documentation.
> It does **not** replace [`requirements.md`](requirements.md) (the single source of truth); `requirements.md` now lists this work as Phase 10. The project rules that used to contradict these items (Romanian docs/comments/UI-only, anonymous-only identity, "no personal data", "frontend has a single env variable", i18n and login in "Non-scop") were already updated in `AGENTS.md`, `requirements.md` and ADR 0001/0003, and the language change is recorded in [ADR 0011](adr/0011-english-language-and-i18n.md). Items still marked **[DECISION]** need an ADR before implementation.

## How an agent should use this file

1. Read `AGENTS.md`, `requirements.md`, `plan.md` and `architecture/{frontend,backend,communication}.md` first.
2. Produce a phased plan (small steps, one new concept per step, `./mvnw verify` and the frontend checks green at the end of each step, `requirements.md` updated per step).
3. Items tagged **[DECISION]** need an ADR and the owner's approval before implementation. Items tagged **[HUMAN]** need manual owner action (accounts, dashboards, visual review). Items tagged **[RESEARCH]** produce a document, not product code.
4. Do not implement anything here in advance of an approved plan.

## Current-state facts the plan must start from

| Area | Current state |
| --- | --- |
| Stack | Next.js 16 / React 19 / Tailwind v4 / shadcn/ui (`frontend/`, UI only, no Route Handlers, no secrets, only `NEXT_PUBLIC_API_BASE_URL`); Spring Boot 4.1 / Java 25 / Spring AI (`backend/`, package `ro.cpa.advisor`: `api`, `service`, `domain`, `repository`, `ai`, `config`; ArchUnit enforces layering). |
| Identity | Anonymous session token (`POST /api/v1/sessions`), stored hashed (SHA-256) server-side, sent as `Authorization: Bearer`; kept in the persisted Zustand store `cpa-app` (version 9, only `sessionToken` + `onboardingSeen`). ADR 0003 rejected login and removed Auth.js. `DELETE /api/v1/sessions/current` cascades profile, conversations, shortlist. Rate limits and the 60,000-token daily budget are per session/IP/global. |
| Language | Everything under `docs/` (except `plan.md`), `README.md`, all UI copy, **code comments**, backend prompts and many user-facing strings are Romanian. `AGENTS.md` §6 and ADR 0001 *mandate* Romanian docs/UI/comments. ~740 lines with Romanian diacritics across ~80 files in `frontend/src` and `backend/src` (heaviest: `lib/guide.ts`, `app-sidebar.tsx`, `lib/api/errors.ts`, `buyer-needs-form.tsx`, `app/layout.tsx`, `appearance-form.tsx`, `theme-provider.tsx`, `store/useAppStore.ts`, `ai/AdvisorTools.java`, backend `ai` tests). The advisor prompt (`ai.AdvisorPrompt`, the only place the persona is composed) instructs the model to answer in Romanian and to write rolling summaries in Romanian. `<html lang="ro">`. Backend seed catalogue (`V3__seed_catalogue.sql`) and tool outputs may contain Romanian labels. |
| Settings | `components/settings/settings-dialog.tsx` — sections registry (`general` → `AppearanceForm`, `profil`, `lista`), opened via `settingsOpen`/`settingsSection` in the store. "General" currently contains only appearance (theme). Theme is `ThemeProvider` + `localStorage["cpa-theme"]`, deliberately not in Zustand. |
| UI | Three zones (sidebar 15 rem, conversation, live side panel from 1280 px; below that a bottom sheet). Documented responsive floor: 360 px, no horizontal scroll at 360/390 px, Lighthouse a11y ≥ 90. Phase 9 still lists "verify on a real phone" as a manual TODO. |
| Tab metadata | `app/layout.tsx` `metadata.title = "CPA — Car Purchase Advisor"`, description in Romanian; `app/favicon.ico` is the default Next.js icon (~26 KB). |
| Vehicle data | Own catalogue: 14 models / 21 variants, figures **indicative and unverified**, "data as of" 2026-11-11, seeded by Flyway `V3`. Search is by type/powertrain/price, **not by name** (known limitation, see `demo-prompt.txt`). Energy prices in `cpa.pricing.*` are indicative. |
| CI / repo | `.github/workflows/backend.yml` (`./mvnw -B -ntp verify`, docker build) and `frontend.yml` (`npm ci`, `lint`, `test`, `format:check`, `build`), both on push to `main` and on `pull_request`. Commit/branch naming enforced by `scripts/check-naming.sh` via git hooks and the `naming` CI check (see R1.4). Local branches `cpa/phase-*` and older history use the previous `CPA-<kebab-title>` format. |
| Hosting | Vercel (frontend), Render Free Docker (backend, `render.yaml`), Neon Free (Postgres). CORS is an explicit origin list (production + one stable branch alias), no `*`. |

---

## R1 — Move code and documentation from Romanian to English; add UI i18n (RO + EN)

**Goal:** all code, comments, documentation and agent/process artefacts in English; the user-visible UI stays Romanian by default **and** gains an English version, switchable from **Preferințe → General**. Observable application behaviour must not change.

### R1.1 Repository content → English
- Translate to English: code comments (including the mandatory per-file header comments), `docs/**` (including ADRs, integration READMEs, `architecture/*`, `requirements.md`, `docs/README.md`, `docs/_template/README.md`, `demo-prompt.txt`), root/`frontend`/`backend` `README.md`, `.env.example` comments, `docker-compose.yml`/`render.yaml` comments, scripts and hook messages (`scripts/hooks/commit-msg`, sync scripts), skill files (`.claude/skills/pre-deploy/SKILL.md`, mirrored to `.github/skills/...` via `sh scripts/sync-skills.sh`), test display names/fixtures, Flyway comments.
- Translate section names that other files reference by name (e.g. "Non-scop", template sections "Ce face, Cont & chei, Variabile de mediu, Pași manuali, Cost & limite, Verificare") consistently, and update every reference (`AGENTS.md` §2, `docs/_template/README.md`, `docs/README.md`, `pre-deploy` skill). Cross-document anchors (`render/README.md#observat-pe-render`, `openai/README.md#verificare`) change with the headings — fix all links.
- **Language ADR already written** ([ADR 0011](adr/0011-english-language-and-i18n.md)); `AGENTS.md` §2/§6, `requirements.md` §1 and ADR 0001 were updated and `CLAUDE.md` / `.github/copilot-instructions.md` regenerated. The remaining work is translating the existing content and keeping `sh scripts/sync-agent-instructions.sh --check` green.
- Do not translate: UI strings (they move to locale files, see R1.2), the Romanian **locale file** content, and Romanian-language user data/test inputs that deliberately exercise Romanian (e.g. demo prompt messages, golden eval scenarios — keep Romanian inputs, translate their comments).
- Cosmetic constraint: file/folder/identifier names are already English; only translate where Romanian leaks (e.g. `docs/adr/0001-piata-moneda-limba.md` file names are Romanian slugs — rename with `git mv` in a **separate commit with no content edits**, per `AGENTS.md` §4, and fix links).

### R1.2 UI i18n (RO default, EN supported)
- Introduce a typed message catalogue per locale (`ro`, `en`) as JSON files (e.g. `frontend/src/i18n/ro.json`, `en.json`; owner suggested "i18n.json files"). **[DECISION]** library vs. a tiny hand-rolled provider: must work with Next.js 16 App Router without Route Handlers and without a locale-prefixed routing change (single page `/`; do not add `/en` routes unless the plan justifies it). The Next.js docs in `frontend/node_modules/next/dist/docs/` must be read first (`AGENTS.md`: "This is NOT the Next.js you know").
- Every user-visible string moves to the catalogue, including registry-driven content: `lib/guide.ts` (guide sections, example prompts, onboarding steps), `lib/glossary.ts`, `lib/journey.ts` (stepper labels), `lib/profile-chips.ts`, `lib/profile-options.ts`, `lib/vehicle-badges.ts`, `lib/budget.ts` labels, `lib/api/errors.ts` (friendly error messages for 503 `advisor-not-configured`, 429 `budget-exceeded`/`rate-limited`, network/cold start, interrupted stream), `backend-status.tsx` ("Se trezește consilierul…", "Backend conectat"), toasts, `aria-label`s, tooltips, `app/layout.tsx` metadata, and `<html lang>`. Registries stay data-driven (`AGENTS.md` §5 "Registry, not `if` chains"): the registry stores message **keys**, not text.
- Number/currency/date formatting (EUR with RON conversion, "data as of" dates) must follow the active locale (`Intl`), via one helper — `Price` component already centralises price display.
- **Language switch:** new control in **Preferințe → General** (settings section `general`, `AppearanceForm` or a sibling form registered in the same section — keep "one component per file"). Do **not** place it in the header (center of the app stays minimal, `AGENTS.md` §5). Persist the choice in `localStorage` through its own provider, mirroring `ThemeProvider` (`createContext` + `useContext`, e.g. key `cpa-locale`); it is **not** added to the Zustand `cpa-app` store (if it were, `version` must be bumped with a `migrate`). Default: Romanian; first visit may fall back to `navigator.language` only if the plan decides so (owner to confirm). Switching applies immediately without reload and updates `<html lang>`.
- Settings navigation labels ("General", "Nevoile tale", "Lista scurtă", "Preferințe", "Ghid") are translated like everything else; the owner's wording "Preferinte → General" refers to the Romanian labels.

### R1.3 Backend / LLM language handling
- Backend prompts, rolling-summary instructions, tool descriptions, tool result labels and error `ProblemDetail` titles/details become English. The model must still answer in the **user's language**: pass the active UI locale (validated against an allow-list `ro|en`, treated as untrusted input) with the message request or on the session/profile, and have `ai.AdvisorPrompt` (still the only place the persona is composed) tell the model to answer in that language, defaulting to the buyer's own language. Bump the prompt version. Rolling summaries become English-only internal state (they are not shown to the user) — define what happens to existing Romanian summaries in the DB (acceptable: regenerate lazily; no data migration required, but state the decision).
- User-facing backend strings that the UI displays must be machine codes mapped to catalogue keys on the frontend (extend the existing `ProblemDetail` type codes) rather than localized server text. `docs/architecture/communication.md`, `openapi.json` and generated types (`npm run api:types`) must be regenerated if the contract changes (e.g. a `locale` field).
- Catalogue (`V3__seed_catalogue.sql` and `domain`): keep the data; if any text field is user-visible Romanian, decide between localizing on the frontend via keys or storing both languages — **[DECISION]**. Do not edit an applied Flyway migration; add a new `V5+` migration if data changes.
- Golden eval scenarios (`EvalScenarios`, `RealModelEvalTest`) must still pass with a recorded model; add at least one English-locale scenario.

### R1.4 Commit and branch naming — implemented
- Decided and implemented (owner request, 2026-11-18), **forward only; history is not rewritten**: branches `CPA-<issue number>-<kebab-message>`, commits and PR titles `CPA-<issue number> <message>`, same issue number across branch, commits and PR title. The unique id is the GitHub issue number.
- Enforcement: `scripts/check-naming.sh` (single source of the rules), `scripts/hooks/commit-msg` and `scripts/hooks/pre-push` (local, need `git config core.hooksPath scripts/hooks`), and the `naming` CI check in `.github/workflows/naming.yml`. Merge/Revert messages and `main` are exempt. `AGENTS.md` §4, `plan.md` §8 and `README.md` describe it.
- **[HUMAN]:** mark the `naming` check as required for `main` in the branch protection/ruleset (see R6). Existing branches `cpa/phase-*` and old `CPA-<kebab-title>` commits do not match and are left as history.
- Rewriting the existing ~40 commits would need a force-push of `main` and is **not** planned.
- Also respect: "Do not commit unless the user asks" (`AGENTS.md` §4).

### Acceptance (R1)
- `git grep -nP '[ăîșțâĂÎȘȚÂşţ]'` (diacritics) over `backend/src/main`, `frontend/src` (excluding the `ro` locale file and deliberately Romanian test inputs) and `docs/` returns nothing unexpected; a script or CI check enforcing this is welcome (the allow-list must be explicit).
- UI in Romanian is **pixel/text-identical** to today's (snapshot the strings before moving them); UI in English shows no Romanian string anywhere (guide, onboarding, errors, tooltips, empty states, backend-status, aria-labels, metadata).
- Switching language in Preferințe → General updates the whole UI immediately, survives reload, and the advisor replies in the selected language in a new message.
- Backend `./mvnw verify` and frontend `build`, `lint`, `test`, `format:check` pass; added Vitest tests for the locale provider, the key-completeness of `ro.json` vs `en.json` (no missing/extra keys), and the formatting helper.
- `requirements.md` §5 "Non-scop" already no longer lists "i18n RO/EN" or optional login; keep the docs index and ADR list consistent.

---

## R2 — Third-party login with per-user conversation history

**Goal:** a user can sign in with an external identity provider (Google, GitHub, …) and sees only **their own** conversations, profile and shortlist, across devices. The project must **not** implement credential storage, password handling, token issuance or any security-sensitive auth logic itself; it delegates that to a third party.

### Constraints inherited from the project (the plan must respect these)
- Frontend has **no Route Handlers and no secrets** (`AGENTS.md` §3). The previously used Auth.js (needs Route Handlers + `AUTH_SECRET`/client secrets) was removed in Phase 6 — do not reintroduce it. Any provider client secret lives only in the provider's dashboard or in the backend environment, never in `frontend/` or any `NEXT_PUBLIC_*` variable.
- Frontend (`*.vercel.app`) and backend (`*.onrender.com`) are different sites: third-party cookies are blocked (the reason for ADR 0003). Prefer a bearer-token model (the frontend already sends `Authorization: Bearer`), with the backend validating tokens as a resource server (JWKS/OIDC issuer validation), over cross-site cookies.
- The backend owns data and authorization; layering `api → service → repository` and ArchUnit still apply. Frontend "owns rendering and input collection" only.
- Logs must not contain tokens or personal data. `AGENTS.md` §3 and `requirements.md` §4 already allow the minimum stored for accounts: the provider's stable subject identifier (hashed or opaque) and, optionally, display name/avatar for the sidebar (the sidebar already uses an avatar). Do not store emails unless justified. Update the privacy text in the in-app guide (`lib/guide.ts` privacy section) and "Șterge-mi datele" semantics when implementing.
- Every new variable → matching `.env.example` with placeholder + one-line comment, and `docs/<integration>/README.md` (mandatory template sections) + index row in `docs/README.md` in the **same commit**; never real client IDs/secrets/accounts in docs.

### Requirements
1. **[DECISION] Provider approach** (ADR superseding/extending ADR 0003; the plan must compare and recommend one):
   - *A. Managed auth service* that handles the social login UI/OAuth dance and issues a JWT the backend verifies (candidates: Neon Auth / Managed Better Auth since Neon is already in the stack; Clerk; Auth0; Firebase/Supabase Auth). Verify free-tier limits, social providers supported, and whether its browser SDK works without Next.js Route Handlers.
   - *B. Backend-driven OAuth2 login* (Spring Security `oauth2Login` with Google/GitHub as IdPs) — backend holds client secrets and issues its own session/JWT. More custom code and security surface; conflicts with "I do not want to handle the security". Only choose if A is infeasible.
2. **Backend:** authenticate requests with the provider's token (resource-server validation); map the provider subject to an internal `user` record (new Flyway migration `V5+`; never edit applied ones); ownership columns on conversations/profile/shortlist (decide how existing anonymous-session data is modelled — see open questions); every repository query for conversations, messages, profile, shortlist must be scoped to the authenticated owner (add negative tests: user A cannot read/modify user B's data → 404/403, including via the SSE `POST /conversations/{id}/messages` endpoint). Update `openapi.json` + `communication.md` + `architecture/backend.md`; regenerate frontend types.
3. **Cost controls:** rate limits and daily token budget are currently per anonymous session; with accounts, budget must be keyed by user (otherwise logout/login resets it) while the global budget remains the kill switch. Keep the anonymous-session abuse protections intact if guests remain.
4. **Frontend:** sign-in / sign-out entry point in the sidebar (user menu with avatar/name; no header button, per the minimal-center rule); sign-in buttons for the chosen providers; signed-in state drives which token is sent; conversation list and history come from the backend per user via the existing TanStack Query hooks (`use-conversations`, `use-profile`, `use-shortlist`) — invalidate caches on login/logout; handle token expiry/401 gracefully (currently a 401 means "create new anonymous session" — this behaviour must be revisited so a signed-in user's 401 triggers re-login, not silent data loss). If `cpa-app` persisted shape changes, bump `version` and add `migrate` (+ test).
5. **Sign-out and data deletion:** sign-out clears local auth state; "Șterge-mi datele" must also delete the user's account data (profile, conversations, shortlist, user row) and tell the user how to revoke the app at the provider. Consumption counters stay anonymous/non-resettable as today.
6. **Guest mode [DECISION]:** whether anonymous sessions remain (a try-before-login path) and whether guest data is claimed on first login. Default proposal: keep guests (it protects the demo flow and the cold-start UX), no automatic merge in the first iteration, with the limitation stated in the UI.
7. **Environments:** local dev must work without real provider credentials where practical (document the manual provider setup, redirect/callback URLs for production, the stable Vercel alias, and localhost); CI must stay keyless (use test tokens/mocked JWKS in backend tests).
8. **Docs:** `docs/<provider>/README.md` (mandatory sections), `docs/README.md` index row, `.env.example` placeholders (frontend only `NEXT_PUBLIC_*` public identifiers if the provider requires them; secrets only backend/host), new ADR, `requirements.md` updates (privacy and identity sections once implemented), `pre-deploy` skill checks if env variables change.

### Acceptance (R2)
- Two different provider accounts each see only their own history on two different browsers/devices; neither can fetch the other's resources by ID (tested).
- No password or provider secret exists in `frontend/`, repo, or logs; backend validates issuer/audience/expiry of tokens.
- Signed-out/guest behaviour matches the decision in (6); 401 handling is covered by a test.
- `./mvnw verify` (including ArchUnit) and all frontend checks pass; docs and ADR updated.
- **[HUMAN]:** create OAuth apps/accounts in the provider dashboards and set host env variables (steps in the integration README).

---

## R3 — Desktop visual quality pass

**Goal:** everything looks good on desktop: scrolling, sidebars, chat content, dialogs, side panel, tables/cards.

- **[HUMAN] first step:** the owner investigates the running app on desktop (typical widths 1280, 1440, 1920, plus 1024–1279 where the side panel is a bottom sheet) and **flags** the sections that look wrong (screenshots + short note per issue). The agent must not guess the list; the plan starts with an issue inventory file or checklist produced from the owner's findings. Suggested areas to inspect: message list scroll and auto-scroll, long messages and tables inside Markdown, vehicle cards/comparison table/TCO stacked bar with long names, sidebar with many conversations, side panel (needs, budget slider, shortlist, running TCO), Preferințe dialog height (`h-[min(36rem,90vh)]`, own scroll container), Ghid dialog tabs, onboarding dialog, empty state, scrollbars in dark and light themes.
- Fixes must use Tailwind + shadcn only (no hand-written CSS; `components/ui/` not edited by hand beyond the documented exceptions), tokens in `globals.css` stay the only theme source, and the minimal-center rule stays.
- Acceptance: each flagged item closed with a before/after screenshot; no horizontal scroll or clipped content at the inspected widths in light and dark; axe 0 violations and Lighthouse accessibility ≥ 90 preserved; no regression of keyboard navigation or `role="log"` behaviour.

## R4 — Small screens (phones, tablets)

**Goal:** the app is comfortably usable on phones and tablets (portrait and landscape).

- Baseline already documented: responsive to 360 px, no horizontal scroll at 360/390 px, side panel becomes a bottom sheet below 1280 px opened by the header button, guide tabs become a 2-column grid. The remaining Phase 9 manual item "verify on a real phone" is **part of this requirement** — **[HUMAN]** test on a real phone and a tablet (iOS Safari + Android Chrome minimum) and record findings.
- Cover: viewport/safe-area handling (notch, `100dvh` and the on-screen keyboard vs. the chat input), touch target sizes (≥ 44 px), sidebar as off-canvas on small screens, Preferințe dialog layout (nav becomes a top row already), tooltips that rely on hover (glossary) need a touch alternative, tablet widths 768–1279 (two-pane vs. sheet), landscape phones, text zoom/large fonts, and the new language switch and login controls from R1/R2.
- Constraints: same as R3 (Tailwind/shadcn, no new CSS files, minimal center). Add Playwright-or-equivalent viewport checks only if the plan justifies adding tooling; otherwise manual checklist + axe/Lighthouse mobile.
- Acceptance: checklist run at 360×640, 390×844, 768×1024, 1024×768 and a landscape phone; Lighthouse mobile accessibility ≥ 90 retained; all primary flows (send message, open panel, change preferences/language, sign in/out, delete data) work with touch only.

## R5 — Browser tab title and icon

- `metadata.title` in `frontend/src/app/layout.tsx` becomes exactly **"Car Purchase Advisor"** (currently "CPA — Car Purchase Advisor"); the `description` is localized/English per R1 (note: `metadata` is static — if the title/description must follow the UI locale, the plan must handle that; the title itself is the same in both locales).
- Replace the default `src/app/favicon.ico` with a generated icon that **contains a car** and uses the app's palette (warm graphite background, amber primary, teal `--signal` accent — take the exact values from `:root`/`.dark` in `src/app/globals.css`). Provide the formats Next.js App Router supports via file conventions in `src/app/` (`favicon.ico`, `icon.svg`/`icon.png`, `apple-icon.png`) and make sure the icon is legible at 16×16 and 32×32 and on both light and dark browser chrome. No new image dependency unless justified; the SVG source should be kept in the repo.
- Acceptance: tab shows the new title and icon on desktop and mobile (including the iOS home-screen icon); `npm run build` passes; the icon is not the Next.js default.

## R6 — Repository branch rules for CPA identical to AGT

- **[HUMAN] / unknown input:** the AGT repository's rules are not in this repo. The owner (or an agent with access) must export them (GitHub → Settings → Branches/Rules, or `gh api repos/<owner>/<AGT>/rulesets` and `.../branches/main/protection`) and provide the list. Do not guess them.
- Then replicate on `BorzRobert/cpa-car-purchase-advisor` (typical items: protected default branch `main`, required PR before merge, required approvals count, dismiss stale approvals, required status checks, up-to-date branch requirement, conversation resolution, linear history, block force pushes and deletions, admin bypass, CODEOWNERS, merge-method restrictions, auto-delete head branches).
- Repo-specific adaptation: required status checks must use the **job names that actually exist** in `.github/workflows/backend.yml` and `frontend.yml` (verify the exact check names on a real PR; AGT's names will differ). If CI uses path filters in future, required checks must still report. Merge-method/linear-history settings interact with R1.4 (commit/branch naming is enforced by the `naming` CI check, R1.4 — include it in the required checks).
- Prefer a reproducible form (a committed ruleset JSON or documented `gh api` commands in `docs/`), without any tokens in the docs. Acceptance: a diff of the AGT and CPA rules shows only intentional differences (e.g. status-check names), and a test PR confirms the rules are enforced.

---

## R7 — Research deliverables (documents only, under `docs/research/`, no product code)

Each deliverable: a short options comparison, a recommendation, cost/limits (free tiers matter — Render Free, Neon Free), legal/ToS risks, an effort estimate, and a proposed requirement/phase for implementation. Written in English; any resulting decision gets an ADR and a `requirements.md` update.

### R7.1 Keeping vehicle information as up to date as possible
Context: static seeded catalogue (14 models/21 variants), unverified figures, "data as of" 2026-11-11, and energy prices in `cpa.pricing.*`. The advisor must never answer prices/specs/costs from model memory — freshness has to come from deterministic sources with a visible "data as of" date. Investigate: licensed/open vehicle-spec and price data sources and APIs (EU/Romania coverage; e.g. manufacturer configurators, aggregator APIs, open datasets such as fuel-economy/WLTP, fuel price sources, depreciation/residual data), refresh strategies (scheduled import job + Flyway-independent data tables, admin/CSV import, per-field "as of" and source), validation of imported data, caching, how freshness is surfaced in tool results/UI, failure behaviour when a source is stale, and how `ResponseAudit` grounding keeps working. Also address the known gap that search cannot find vehicles **by name**.

### R7.2 Listings from trusted sources, selectable by the user
Context: today the advisor has no live listings. Investigate how it could return links to listings from sources such as Autovit, Mobile.de (and others in RO/EU) — official partner APIs/feeds vs. deep links built from search parameters vs. scraping (check each site's ToS/robots and legal risk; scraping is likely not acceptable), affiliate programmes, rate limits and costs. Design questions for the report: a source registry (data, not `if` chains) with an enabled flag per source; a **user-selectable list of sources** persisted per user (profile field or user preference; backend-owned and validated; interacts with R2 accounts and with the profile schema/`updateBuyerProfile` tool); a new deterministic tool (e.g. `findListings`) so the model only emits links from tool results (never from memory), with "data as of"; link-safety (allow-listed domains only, `rel="noopener noreferrer"`, rendered through the single Markdown component/no raw HTML); the existing listing-analysis prompt for pasted ads; and cost/latency effects on the token budget.

### R7.3 "Preview" deployment per pull request
Context: frontend on Vercel, backend on Render Free (Docker, `render.yaml`), DB on Neon, CORS is an explicit origin list (no `*`), CI exists for both services. Investigate: Vercel Preview Deployments for PRs (automatic; where `NEXT_PUBLIC_API_BASE_URL` points per preview), backend options (Render PR previews/preview environments — verify availability and cost on the current plan; shared staging backend; ephemeral container elsewhere), Neon branching per PR (Neon branches can give each PR an isolated copy of the database; check free-tier limits and Flyway behaviour on a fresh branch), how to handle CORS for dynamic preview origins safely without `*` (pattern-based allow-list limited to this project's preview domains), secrets handling for previews (no production keys; separate OpenAI key/budget or the recorded model, tight budget caps), OAuth redirect URIs for previews if R2 lands first (many providers disallow wildcard callbacks), cleanup on PR close, and how reviewers get the preview URL (PR comment/bot). Output: a recommended minimal setup plus the manual steps to document in `docs/vercel`, `docs/render`, `docs/neon`.

---

## Suggested ordering and dependencies (for the plan author, not binding)

1. R1.1 prerequisite (language ADR + `AGENTS.md` change) is done; start with R1.2 or the bulk translation.
2. R5 (small, independent), R6 (needs owner input) — can run anytime in parallel with the rest.
3. R1.2 → R1.3 (i18n catalogue before the backend locale contract; one `openapi.json` regeneration if possible), then bulk translation R1.1 in reviewable chunks (frontend, backend, docs separately; `git mv` renames in their own commit).
4. R3 and R4 start with the **[HUMAN]** investigation in parallel with R1; do them **after** R1.2 so English strings (usually shorter/longer than Romanian) are covered by the visual pass.
5. R2 after the language work (new UI strings must be born in the catalogue) and after the R7.3 preview findings if previews must support login callbacks.
6. R7 research items can start any time; R7.2 benefits from R2's user model.

## Open questions for the owner (the plan must resolve or ask before implementing)

1. Default UI language and whether first visit follows the browser language (R1.2).
2. Login provider list (Google, GitHub, others?) and preferred approach (managed service vs. backend-driven OAuth) (R2).
3. Do anonymous guest sessions stay, and is guest data claimed on login (R2).
4. The AGT repository's branch/ruleset configuration (R6).
5. The desktop issues list from the manual investigation (R3) and devices available for the phone/tablet test (R4).
6. Should the English UI also change the advisor's "market" (still Romania/EUR) — assumed **no**: market, currency and catalogue stay as in ADR 0001 (only the language changes).

## Definition of done reminders (from `AGENTS.md` §8)

Per step: frontend `npm run build`, `npm run lint`, `npm test`, `npm run format:check` (`npx prettier --end-of-line auto --check .` on Windows) and backend `./mvnw verify` pass; the new behaviour is visible when running the services; `docs/requirements.md` reflects reality; every new integration has its `docs/<integration>/README.md`, index row and `.env.example` placeholders; the step is explained in plain language; the `pre-deploy` skill runs before publishing.
