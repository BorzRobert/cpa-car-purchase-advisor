# Backend — arhitectură (stadiul Fazei 3)

Serviciul din `backend/`: Java 25, Spring Boot, Maven Wrapper. Deține toată logica de produs; frontend-ul doar afișează. În Faza 3 scheletul este publicat (Render + Neon) și are o migrare Flyway de bază goală — fără domeniu și fără apeluri către model încă.

## Versiuni fixate (verificate la 2026-11-11)

| Componentă         | Versiune | De ce această versiune                                                                                                  |
| ------------------ | -------- | ----------------------------------------------------------------------------------------------------------------------- |
| Java               | 25 (LTS) | Cerută de D9/plan; Temurin 25.0.5. Spring AI 2.0.2 are baza Java 17, deci rulează pe 25.                                |
| Spring Boot        | 4.1.2    | Cea mai nouă linie stabilă; este exact `spring-boot.version` din Spring AI 2.0.2 (4.2 există doar ca RC).               |
| Spring AI          | 2.0.2    | Ultima versiune stabilă (2.1 este încă RC); Initializr o leagă de Boot `>=4.0.0 <4.2.0`.                                |
| springdoc-openapi  | 3.2.0    | Linia pentru Boot `>=4.1`, conform Initializr.                                                                          |
| swagger-core       | 2.2.55   | Fixat explicit — vezi „Capcane” mai jos.                                                                                |
| ArchUnit           | 1.5.1    | Ultima versiune; citește fișiere `.class` Java 25.                                                                      |
| Maven (wrapper)    | 3.10.0   | Din `.mvn/wrapper/maven-wrapper.properties` generat de Initializr; nu se instalează Maven global.                       |

Versiunile sunt în `backend/pom.xml` (părintele Boot și proprietățile `spring-ai.version`, `springdoc.version`, `swagger-core.version`, `archunit.version`). Un upgrade se face deliberat, după ce s-a verificat din nou compatibilitatea Spring AI ↔ Boot ↔ Java.

### Capcane întâlnite

- **springdoc vs Spring AI:** Spring AI aduce `swagger-annotations-jakarta` 2.2.38 și `swagger-annotations` 2.2.31 (prin `openai-java-core`), mai vechi decât 2.2.55 cerut de springdoc 3.2.0. Rezultat: `/v3/api-docs` răspundea 500 cu `NoSuchMethodError: Schema.$dynamicRef()`. Soluția din `pom.xml`: `swagger-annotations-jakarta` fixat la 2.2.55 în `dependencyManagement` și `swagger-annotations` exclus din starter-ul OpenAI. `CpaBackendApplicationTests` păzește acest caz.
- **Fără cheie:** Spring AI 2.0.2 nu oprește aplicația dacă `spring.ai.openai.api-key` este gol; disponibilitatea consilierului se verifică separat (`AdvisorAvailability`).
- **Model:** `gpt-5-mini` are dată de oprire (2026-12-11) — vezi [`../openai/README.md`](../openai/README.md).

## Straturi

Pachetul de bază este `ro.cpa.advisor` (D9):

| Pachet       | Rol (neschimbat în Faza 3)                                                                              |
| ------------ | ------------------------------------------------------------------------------------------------------- |
| `api`        | `HealthController` (`GET /api/v1/health`), `HealthResponse`, `ApiExceptionHandler` (`ProblemDetail`).    |
| `service`    | `StatusService` — singurul care vorbește cu `ai` pentru endpoint-ul de status.                           |
| `domain`     | `AdvisorNotConfiguredException`; nu depinde de nimic în afară de `java.*`.                               |
| `repository` | gol, rezervat pentru Faza 4.                                                                             |
| `ai`         | `AdvisorAvailability` — spune dacă există o cheie OpenAI utilizabilă; valoarea cheii nu iese de aici.    |
| `config`     | `CpaProperties`, `SecurityConfig` (securitate + CORS), `RequestIdFilter`, `OpenApiConfig`.               |

Regulile `api → service → repository`, `service → domain`, `ai` folosit doar de `service`, `domain` fără dependențe, controllere fără acces la repository sau Spring AI sunt verificate de `ArchitectureTest` (ArchUnit) la fiecare `./mvnw verify`.

## Configurare

`backend/src/main/resources/application.yml`; valorile secrete vin din mediu sau din `backend/.env` (citit automat, gitignorat). Șablonul este `backend/.env.example`.

| Proprietate / variabilă                              | Implicit                  | Rol                                                                                  |
| ---------------------------------------------------- | ------------------------- | ------------------------------------------------------------------------------------ |
| `OPENAI_API_KEY` → `spring.ai.openai.api-key`        | gol                       | Cheia OpenAI; doar în backend. Lipsa ei nu oprește aplicația.                        |
| `CPA_LLM_MODEL` → `cpa.llm.model`                    | `gpt-5-mini`              | Singurul model folosit (ADR 0007).                                                   |
| `CORS_ALLOWED_ORIGINS` → `cpa.cors.allowed-origins`  | `http://localhost:3000` (în `prod`: fără implicit) | Origini separate prin virgulă; `*` este refuzat la pornire.                 |
| `SPRING_PROFILES_ACTIVE`                             | niciunul (dezvoltare)     | `prod` pe Render: activează `application-prod.yml` (vezi mai jos).                   |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD`  | H2 în memorie (mod PostgreSQL); în `prod` URL-ul e obligatoriu | Baza de date; în producție Neon (URL JDBC, vezi [`../neon/README.md`](../neon/README.md)). |

Alte setări: `spring.threads.virtual.enabled=true` (thread-uri virtuale), `spring.jpa.open-in-view=false`, Actuator expune doar `health`.

### Baza de date și Flyway

- Flyway rulează la pornire pe orice bază configurată; migrările sunt în `backend/src/main/resources/db/migration/` (`V<număr>__<descriere>.sql`). Acum există doar `V1__baseline.sql`, **goală intenționat** — dovedește că Flyway rulează și că `flyway_schema_history` se creează; schema reală vine în Faza 4. Regula: aceeași migrare trebuie să treacă pe H2 (mod PostgreSQL) și pe PostgreSQL, deci SQL standard.
- Dovada: `CpaBackendApplicationTests.flywayAppliesTheBaselineMigrationOnBoot` (H2) și o rulare pe PostgreSQL 17 într-un container (2026-11-11): `1 | baseline | t`. Testcontainers în CI vine cu persistența (Faza 4).
- `ddl-auto: validate`: Hibernate nu creează tabele; doar schema din migrări există.

### Profilul `prod` (`application-prod.yml`)

Activat pe Render prin `SPRING_PROFILES_ACTIVE=prod`. Alegerile specifice gazdei stau aici, ca dezvoltarea locală să rămână simplă:

| Setare                                                          | De ce                                                                                                             |
| --------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| `spring.datasource.url: ${SPRING_DATASOURCE_URL}` (fără implicit) | Fără URL aplicația nu pornește; un H2 în memorie ar „funcționa" și ar pierde datele la fiecare repornire.         |
| `cpa.cors.allowed-origins: ${CORS_ALLOWED_ORIGINS}` (fără implicit) | Lista exactă de origini se setează explicit în producție; fără `localhost` moștenit.                            |
| Hikari: `max-lifetime` 240 s, `idle-timeout` 60 s, `minimum-idle` 0, `maximum-pool-size` 5, `connection-timeout` 30 s, `validation-timeout` 5 s | Neon Free adoarme după 5 min și închide conexiunile; o conexiune veche nu se refolosește, una inactivă e validată înainte de folosire. Pool mic: 512 MB RAM pe Render. |
| Hikari `initialization-fail-timeout: 60000`                     | La pornire așteptăm până la 60 s trezirea bazei, în loc să cădem la prima încercare.                             |
| Flyway `connect-retries: 5`, `connect-retries-interval: 3s`     | Reîncearcă conectarea la migrare dacă baza încă se trezește.                                                     |
| Fără `keepalive-time` (intenționat)                             | Ar ține Neon treaz non-stop și ar consuma cele 100 CU-ore gratuite.                                              |
| `management.health.db.enabled: false`                           | `/actuator/health` e apelat des (health check Render, ping-ul frontend-ului); nu trebuie să trezească Neon. Disponibilitatea bazei va avea un grup de readiness în Faza 4. |
| `springdoc.api-docs.enabled: false`, `springdoc.swagger-ui.enabled: false` | Swagger UI și `/v3/api-docs` nu sunt publice în producție (404). Rămân active local și în CI.            |

Verificat la 2026-11-11 cu imaginea Docker a backend-ului pe un container PostgreSQL 17 (profil `prod`, `PORT=10000`, 512 MB): Flyway aplică `V1`, `/api/v1/health` răspunde, `/v3/api-docs` dă `404`, CORS acceptă doar originea configurată, iar pornirea cu baza oprită așteaptă și își revine când baza revine. `ProductionProfileTest` păzește partea fără bază reală (Swagger oprit, fără `dbHealthIndicator`).

## Securitate și erori

- **Fără stare:** fără sesiune, cookie sau login; identitatea va fi un token anonim în `Authorization: Bearer` (filtru în Faza 4). Până atunci sunt publice doar `/api/v1/health`, `/actuator/health`, `/v3/api-docs` și `/swagger-ui`; orice altă cale primește `403`.
- **CORS:** doar originile din listă, metodele `GET/POST/DELETE/OPTIONS`, headerele `Authorization`, `Content-Type`, `X-Request-Id`; fără credentials. Strategia pentru Vercel (Production exact, Preview doar prin aliasul stabil al branch-ului, fără wildcard) este în [`../vercel/README.md`](../vercel/README.md#cors-production-vs-preview).
- **Id de cerere:** `RequestIdFilter` pune `X-Request-Id` în răspuns și în fiecare linie de log; un id primit de la client se acceptă doar dacă este inofensiv (`[A-Za-z0-9-]{8,64}`).
- **Erori:** toate ies ca `ProblemDetail` (RFC 9457) cu proprietatea `requestId`. `AdvisorNotConfiguredException` → `503`, tip `urn:cpa:problem:advisor-not-configured`; orice excepție neprevăzută → `500` generic, fără detalii interne.
- **Swagger UI și `/v3/api-docs`** sunt deschise local și în CI, dar **oprite în producție** (profilul `prod`, `404`) — decis în Faza 3; se reconsideră în Faza 9 doar dacă există un motiv.

## Teste

`./mvnw verify` (din `backend/`, cu JDK 25) rulează: teste unitare (`AdvisorAvailability`, `CpaProperties`, `RequestIdFilter`, `ApiExceptionHandler`), `@WebMvcTest` pentru endpoint-ul de status și CORS, pornirea completă a aplicației **fără** cheie OpenAI (H2 + Flyway cu migrarea de bază + Security + springdoc), pornirea cu profilul `prod` pe H2 (`ProductionProfileTest`) și testul ArchUnit de straturi. Testcontainers (Postgres) apare odată cu persistența, în Faza 4.

## Docker și CI

- `backend/Dockerfile`: build în două etape (`eclipse-temurin:25-jdk` → `eclipse-temurin:25-jre`), jar „layered”, utilizator fără drept de root, `-XX:MaxRAMPercentage=60 -XX:+UseSerialGC` (60% fiindcă planul Render Free are 512 MB), portul din `$PORT` (implicit 8080; Render injectează `10000`). Verificat la 2026-11-11: imaginea pornește fără cheie, cu `PORT=10000` și limită de 512 MB (~270 MiB folosiți), iar `GET /api/v1/health` răspunde.
- `render.yaml` (rădăcina repo-ului): Blueprint-ul serviciului Render — Docker, `rootDir: backend`, plan `free`, regiunea `frankfurt`, health check `/actuator/health`, variabilele secrete declarate cu `sync: false`. Detalii: [`../render/README.md`](../render/README.md).
- `.github/workflows/backend.yml`: `./mvnw verify` cu Temurin 25, apoi `docker build`; rulează doar când se schimbă `backend/`. `.github/workflows/frontend.yml` face același lucru pentru `frontend/` (`npm ci`, `lint`, `format:check`, `build`).
- Compatibilitatea gazdei (verificată 2026-11-11 din documentația Render; confirmarea finală e primul deploy) și limitele Neon sunt în [`../render/README.md`](../render/README.md) și [`../neon/README.md`](../neon/README.md).

## Rulare locală

```bash
cd backend
cp .env.example .env        # opțional; fără el aplicația pornește fără cheie
./mvnw spring-boot:run      # http://localhost:8080  (JAVA_HOME trebuie să indice JDK 25)
```

- Status: `http://localhost:8080/api/v1/health`
- OpenAPI: `http://localhost:8080/v3/api-docs` · Swagger UI: `http://localhost:8080/swagger-ui.html`
- Sănătate pentru gazdă: `http://localhost:8080/actuator/health`
