# Comunicare frontend ↔ backend

Acest document este **contractul** dintre `frontend/` (Next.js, doar UI) și `backend/` (Spring Boot). Contractul formal, citibil de mașină, este documentul OpenAPI din [`openapi.json`](openapi.json); aici stau regulile pe care el nu le poate exprima (identitate, erori, limite, streaming). Dacă documentul și codul diferă, testul `OpenApiSnapshotTest` pică în CI.

**Stadiul (Faza 5a):** REST/JSON complet și testat, iar endpoint-ul de streaming SSE (secțiunea de la final) este **implementat** pentru evenimentele `meta`, `token`, `error` și `done`; `tool`, `vehicles`, `profile` vin în 5b și `compare` în 5c.

```mermaid
sequenceDiagram
    participant UI as Frontend (Next.js)
    participant API as Backend (Spring Boot)
    UI->>API: GET /actuator/health (la încărcare; trezește backend-ul)
    UI->>API: POST /api/v1/sessions
    API-->>UI: 201 {token, createdAt}  (tokenul apare o singură dată)
    Note over UI: token în localStorage (cpa-app)
    UI->>API: GET/PUT/POST/DELETE ... + Authorization: Bearer token
    API->>API: IP limit → autentificare → limită pe sesiune → validare → serviciu
    API-->>UI: JSON sau application/problem+json
```

## Reguli generale

| Aspect          | Regulă                                                                                                                                                                              |
| --------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Prefix, versiune | Totul sub `/api/v1`. În interiorul unei versiuni se fac doar schimbări aditive (câmpuri noi, endpoint-uri noi); o schimbare incompatibilă înseamnă `/api/v2`.                       |
| Format          | JSON (`application/json`), câmpuri în `camelCase`, momente în ISO-8601 UTC (`2026-11-11T10:00:00Z`), enum-uri ca text `MAJUSCULE`.                                                  |
| Identitate      | Token de sesiune anonim, `Authorization: Bearer <token>`. Fără cookie-uri (frontend-ul și backend-ul sunt pe origini diferite).                                                     |
| Request id      | Clientul poate trimite `X-Request-Id` (`[A-Za-z0-9-]{8,64}`); backend-ul îl returnează în fiecare răspuns, îl pune în fiecare linie de log și în `requestId` din erori. Altfel generează unul. |
| CORS            | Doar originile din `CORS_ALLOWED_ORIGINS` (fără `*`). Metode: `GET, POST, PUT, PATCH, DELETE, OPTIONS`. Antete permise: `Authorization, Content-Type, X-Request-Id`. Antete expuse browserului: `X-Request-Id, Retry-After`. Fără credentials. |
| Cache           | `POST /sessions` răspunde `Cache-Control: no-store` (conține un secret).                                                                                                             |
| Limite de mărime | Text: mesaj 4000 caractere, titlu 120, notă 500, element de listă în profil 80 (max. 10 elemente). Un text prea lung se **refuză** (400), nu se taie.                                |

## Sesiune și autentificare

1. `POST /api/v1/sessions` (public) → `201 {"token": "cpa_…", "createdAt": "…"}`. Tokenul are 256 de biți aleatorii; **apare o singură dată** și nu poate fi recuperat.
2. Frontend-ul îl păstrează în `localStorage` și îl trimite ca `Authorization: Bearer cpa_…` la toate celelalte apeluri.
3. Serverul păstrează doar **hash-ul SHA-256** al tokenului (`visitor_session.token_hash`). Un token aleatoriu pe 256 de biți nu are nevoie de sare sau de hash lent; un „pepper” (HMAC cu un secret din mediu) rămâne o întărire opțională pentru Faza 9.
4. `GET /api/v1/sessions/current` verifică un token stocat (la pornirea aplicației). `401` → frontend-ul creează o sesiune nouă.
5. `DELETE /api/v1/sessions/current` = „șterge-mi datele”: șterge sesiunea cu profilul, conversațiile, mesajele și lista scurtă (în cascadă, în baza de date). Tokenul nu mai funcționează. Contorii de consum (`usage_log`) **rămân, fără legătură cu cineva**, ca „șterge + sesiune nouă” să nu poată reseta bugetul zilnic global.
6. Un id care aparține altei sesiuni dă **404**, la fel ca unul inexistent — nu 403.

Fără date personale: sesiunea nu conține nimic în afară de id, hash și momente.

## Endpoint-uri

Toate cer `Authorization: Bearer` în afară de cele marcate „public”.

| Metodă și cale                                   | Ce face                                                                                      | Răspunsuri notabile                           |
| ------------------------------------------------ | -------------------------------------------------------------------------------------------- | --------------------------------------------- |
| `GET /api/v1/health` (public)                    | Stare: `{status, service, version, advisorConfigured}`                                       | —                                             |
| `POST /api/v1/sessions` (public)                 | Creează sesiunea, întoarce tokenul                                                           | `201`, `429` (limită per IP)                  |
| `GET /api/v1/sessions/current`                   | `{createdAt, lastSeenAt}`; verifică tokenul                                                  | `401`                                         |
| `DELETE /api/v1/sessions/current`                | Șterge toate datele sesiunii                                                                 | `204`                                         |
| `GET /api/v1/profile`                            | Profilul cumpărătorului; **profil gol** (câmpuri `null`, liste `[]`) dacă nu există încă      | `200`                                         |
| `PUT /api/v1/profile`                            | Înlocuiește tot profilul (ce lipsește din corp se șterge)                                    | `200`, `400`                                  |
| `DELETE /api/v1/profile`                         | Șterge profilul                                                                              | `204`                                         |
| `GET /api/v1/conversations`                      | Conversațiile, cele mai recent actualizate primele                                           | `200`                                         |
| `POST /api/v1/conversations`                     | Creează o conversație; `{title?}`, titlul se deduce din primul mesaj dacă lipsește            | `201`, `409 limit-reached`                    |
| `GET /api/v1/conversations/{id}`                 | O conversație                                                                                | `200`, `404`                                  |
| `PATCH /api/v1/conversations/{id}`               | Redenumește: `{title}`                                                                       | `200`, `400`, `404`                           |
| `DELETE /api/v1/conversations/{id}`              | Șterge conversația cu mesajele ei                                                            | `204`, `404`                                  |
| `GET /api/v1/conversations/{id}/messages`        | Mesajele, cele mai vechi primele                                                             | `200`, `404`                                  |
| `POST /api/v1/conversations/{id}/messages`       | Salvează mesajul utilizatorului `{content}` și răspunde cu **`text/event-stream`**: răspunsul consilierului, în streaming (vezi „Streaming”) | `200` (flux), `400`, `404`, `409`, `429 budget-exceeded`, `503 advisor-not-configured` |
| `GET /api/v1/shortlist`                          | Lista scurtă, cele mai noi primele; fiecare element are `vehicle` cu doar varianta salvată    | `200`                                         |
| `POST /api/v1/shortlist`                         | Adaugă o variantă: `{variantId, note?}`                                                      | `201`, `404`, `409 already-in-shortlist` / `limit-reached` |
| `DELETE /api/v1/shortlist/{id}`                  | Scoate un element (id-ul elementului, nu al variantei)                                       | `204`, `404`                                  |
| `GET /api/v1/vehicles`                           | Catalogul: `{dataAsOf, vehicles[]}`; filtre opționale `bodyType`, `powertrain`, `maxPriceEur`, `minSeats` | `200`, `400`                       |
| `GET /api/v1/vehicles/{id}`                      | Un model cu toate variantele (slug, de exemplu `vw-golf`)                                    | `200`, `404`                                  |
| `GET /api/v1/usage`                              | Consumul sesiunii pe ziua UTC curentă față de buget                                          | `200`                                         |

Notă despre `POST …/messages`: până în Faza 4 răspundea cu **JSON** (mesajul salvat); din Faza 5a răspunde cu `text/event-stream`. A fost singura schimbare incompatibilă prevăzută, făcută înainte ca frontend-ul să-l folosească (Faza 6). Mesajele (al utilizatorului și al consilierului) se citesc oricând cu `GET …/messages`.

Datele din catalog sunt **orientative**: UI-ul trebuie să afișeze `dataAsOf` („date la”) lângă orice preț.

## Erori

Orice eroare este `application/problem+json` (RFC 9457), cu aceeași formă, ca UI-ul să aibă un singur mod de a le citi:

```json
{
  "type": "urn:cpa:problem:budget-exceeded",
  "title": "Too many requests",
  "status": 429,
  "detail": "The daily token budget of this session is used up.",
  "code": "budget-exceeded",
  "requestId": "5b0f3c1e-…"
}
```

`code` este identificatorul stabil pe care UI-ul îl traduce în text românesc; `detail` este în engleză, pentru jurnale și dezvoltatori, **nu** pentru afișare. Niciodată stack trace, nume de clase, chei sau valori de configurare — o eroare neprevăzută este `500 internal-error` cu mesaj generic, iar detaliile rămân doar în jurnalul serverului.

| `code`                   | Status | Când                                                                                          |
| ------------------------ | ------ | --------------------------------------------------------------------------------------------- |
| `unauthorized`           | 401    | Lipsește sau nu este valid tokenul (răspunsul are `WWW-Authenticate: Bearer`)                 |
| `forbidden`              | 403    | Cale neprevăzută pentru nimeni (regula implicită „refuză tot ce nu e listat”)                 |
| `not-found`              | 404    | Resursa nu există **sau aparține altei sesiuni**                                              |
| `validation-error`       | 400    | Corpul nu respectă formatul; `errors: [{field, message}]` listează câmpurile greșite          |
| `invalid-input`          | 400    | Date valide ca format, refuzate de regulile de domeniu (de exemplu `budgetAmount` fără `budgetKind`, text gol după curățare, text prea lung) |
| `bad-request` și alte coduri deduse din status (`method-not-allowed`, `unsupported-media-type`, …) | 4xx | Erori standard Spring (JSON invalid, enum necunoscut într-un parametru) |
| `already-in-shortlist`   | 409    | Varianta este deja salvată                                                                    |
| `limit-reached`          | 409    | Prea multe conversații, mesaje într-o conversație sau elemente în lista scurtă                |
| `conflict`               | 409    | Cereri simultane au încălcat o constrângere unică                                             |
| `rate-limited`           | 429    | Prea multe cereri (per sesiune, per IP sau prea multe sesiuni noi per IP); `Retry-After` în secunde |
| `budget-exceeded`        | 429    | Bugetul zilnic de tokeni al sesiunii sau cel global este epuizat; `Retry-After` = până la miezul nopții UTC |
| `advisor-not-configured` | 503    | Lipsește cheia OpenAI; aplicația funcționează, consilierul nu. Verificat **după** toate celelalte (400, 404, 409, 429) și înainte de a salva mesajul |
| `advisor-unavailable`    | (502)  | Apare **doar** ca eveniment SSE `error`, dacă apelul către model eșuează după începerea fluxului; mesajul nu conține detalii de la furnizor |
| `internal-error`         | 500    | Orice neprevăzut                                                                              |

## Limite și buget

Valori implicite în `application.yml` (`cpa.limits.*`), documentate în [`backend.md`](backend.md#limite-de-rată-și-buget):

| Limită                              | Implicit        | Mecanism                                                       |
| ----------------------------------- | --------------- | -------------------------------------------------------------- |
| Cereri pe minut, per sesiune        | 60              | Bucket4j, în memorie, per instanță                             |
| Cereri pe minut, per IP             | 120             | idem                                                           |
| Sesiuni noi pe oră, per IP          | 10              | idem                                                           |
| Conversații per sesiune             | 50              | număr în baza de date → `409 limit-reached`                    |
| Mesaje per conversație              | 200             | idem                                                           |
| Elemente în lista scurtă            | 20              | idem                                                           |
| Tokeni pe zi (UTC), per sesiune     | 60 000          | `usage_log` → `429 budget-exceeded`                            |
| Tokeni pe zi (UTC), global          | 1 000 000       | idem; include și sesiunile șterse                              |

Cheia „per IP” este adresa clientului: în producție se ia din `X-Forwarded-For`, **de la dreapta** (intrarea adăugată de proxy-ul gazdei; ce scrie clientul stă mai la stânga și este ignorat). Frontend-ul trebuie să citească `Retry-After` (antet expus prin CORS) și să afișeze starea „limită atinsă”, nu o eroare generică.

## Contractul OpenAPI și tipurile TypeScript

- Springdoc generează documentul la `/v3/api-docs` (și Swagger UI la `/swagger-ui.html`) **doar local și în CI**; în producție sunt oprite (404).
- Instantaneul comis este [`openapi.json`](openapi.json) (chei sortate, fără `servers`). `OpenApiSnapshotTest` îl compară cu documentul generat la fiecare `./mvnw verify`, deci și în CI (`backend.yml`): un endpoint sau un DTO schimbat fără regenerare face build-ul roșu.
- Regenerare, din `backend/` (JDK 25): `./mvnw test -Dtest=OpenApiSnapshotTest -Dopenapi.update=true`, apoi se comite fișierul împreună cu codul.
- **Faza 6:** frontend-ul generează `lib/api/schema.d.ts` din acest fișier cu `openapi-typescript`; tipurile nu se scriu de mână.
- Swagger UI: butonul **Authorize** primește tokenul de la `POST /api/v1/sessions` și îl trimite pe fiecare cerere — așa se poate parcurge tot CRUD-ul local.

## Streaming SSE (Faza 5a: `meta`, `token`, `error`, `done`)

`POST /api/v1/conversations/{id}/messages` răspunde cu `text/event-stream;charset=UTF-8`, cu antetele `Cache-Control: no-cache` și `X-Accel-Buffering: no`, și un comentariu SSE de heartbeat (`: keep-alive`) la ~15 secunde (`cpa.stream.heartbeat-seconds`), ca proxy-urile să nu închidă sau să nu buferizeze fluxul. Clientul folosește `fetch` + `ReadableStream` (nu `EventSource`, care nu poate trimite POST sau antete) și poate trimite `Accept: text/event-stream, application/problem+json`; oprirea se face cu `AbortController`: serverul oprește apelul către model, iar consumul răspunsului parțial se înregistrează estimat. Verificările (proprietar, buget, text, limită de mesaje, cheie) rămân înainte de orice apel către model.

Un cadru arată așa (fiecare `data:` este o singură linie JSON):

```text
event: meta
data: {"messageId":"…","conversationId":"…"}

event: token
data: {"delta":"Salut! "}

: keep-alive

event: done
data: {"usage":{"input":1523,"output":212}}
```

Ordinea este mereu `meta`, apoi zero sau mai multe `token`, apoi **fie** `done`, **fie** `error`. `messageId` din `meta` este id-ul sub care răspunsul apare în `GET …/messages`. Dacă ultimul eveniment nu este `done` sau `error` (conexiune întreruptă), UI-ul tratează răspunsul ca incomplet.

| Eveniment  | Conținut                                                  | Rol                                              | Stare      |
| ---------- | --------------------------------------------------------- | ------------------------------------------------ | ---------- |
| `meta`     | `{messageId, conversationId}`                             | Începutul răspunsului                            | 5a         |
| `token`    | `{delta}`                                                 | Text în streaming                                | 5a         |
| `tool`     | `{name, status: started\|finished, summary}`              | „Se calculează costul total…” în UI              | 5b         |
| `vehicles` | `{cards: VehicleCard[]}`                                  | Carduri în loc de tabele Markdown                | 5b         |
| `compare`  | `{vehicles, criteria, scores}`                            | Widget de comparație                             | 5c         |
| `profile`  | `{patch}`                                                 | Profil actualizat de agent (panoul lateral)      | 5b         |
| `error`    | `ProblemDetail` (aceeași formă ca mai sus; `code: advisor-unavailable`) | Eroare în mijlocul fluxului        | 5a         |
| `done`     | `{usage: {input, output}}`                                | Sfârșitul fluxului; backend-ul a înregistrat consumul | 5a    |

Eroarea dinaintea fluxului (401, 404, 429 etc.) este un răspuns HTTP obișnuit `problem+json`; doar o eroare apărută după începerea fluxului ajunge ca eveniment `error` (statusul HTTP este deja `200`). Mesajul utilizatorului rămâne salvat și când răspunsul eșuează, dar nu se salvează un răspuns gol.
