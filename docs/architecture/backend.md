# Backend — arhitectură (stadiul Fazei 2)

Serviciul din `backend/`: Java 25, Spring Boot, Maven Wrapper. Deține toată logica de produs; frontend-ul doar afișează. În Faza 2 există doar scheletul — fără domeniu, fără persistență reală, fără apeluri către model.

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

| Pachet       | Rol în Faza 2                                                                                           |
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
| `CORS_ALLOWED_ORIGINS` → `cpa.cors.allowed-origins`  | `http://localhost:3000`   | Origini separate prin virgulă; `*` este refuzat la pornire.                          |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD`  | H2 în memorie (mod PostgreSQL) | Baza de date; în producție (Faza 3) Neon.                                       |

Alte setări: `spring.threads.virtual.enabled=true` (thread-uri virtuale), `spring.jpa.open-in-view=false`, Flyway activ (nu există încă migrări — primele apar în Faza 4), Actuator expune doar `health`.

## Securitate și erori

- **Fără stare:** fără sesiune, cookie sau login; identitatea va fi un token anonim în `Authorization: Bearer` (filtru în Faza 4). Până atunci sunt publice doar `/api/v1/health`, `/actuator/health`, `/v3/api-docs` și `/swagger-ui`; orice altă cale primește `403`.
- **CORS:** doar originile din listă, metodele `GET/POST/DELETE/OPTIONS`, headerele `Authorization`, `Content-Type`, `X-Request-Id`; fără credentials.
- **Id de cerere:** `RequestIdFilter` pune `X-Request-Id` în răspuns și în fiecare linie de log; un id primit de la client se acceptă doar dacă este inofensiv (`[A-Za-z0-9-]{8,64}`).
- **Erori:** toate ies ca `ProblemDetail` (RFC 9457) cu proprietatea `requestId`. `AdvisorNotConfiguredException` → `503`, tip `urn:cpa:problem:advisor-not-configured`; orice excepție neprevăzută → `500` generic, fără detalii interne.
- **Swagger UI și `/v3/api-docs`** sunt deschise deocamdată; se decide în Faza 9 dacă rămân publice în producție (`springdoc.*.enabled=false`).

## Teste

`./mvnw verify` (din `backend/`, cu JDK 25) rulează: teste unitare (`AdvisorAvailability`, `CpaProperties`, `RequestIdFilter`, `ApiExceptionHandler`), `@WebMvcTest` pentru endpoint-ul de status și CORS, pornirea completă a aplicației **fără** cheie OpenAI (H2 + Flyway + Security + springdoc) și testul ArchUnit de straturi. Testcontainers (Postgres) apare odată cu persistența, în Faza 4.

## Docker și CI

- `backend/Dockerfile`: build în două etape (`eclipse-temurin:25-jdk` → `eclipse-temurin:25-jre`), jar „layered”, utilizator fără drept de root, `-XX:MaxRAMPercentage=75 -XX:+UseSerialGC`, portul din `$PORT` (implicit 8080). Verificat la 2026-11-11: imaginea pornește fără cheie și `GET /api/v1/health` răspunde.
- `.github/workflows/backend.yml`: `./mvnw verify` cu Temurin 25, apoi `docker build`; rulează doar când se schimbă `backend/`.
- Compatibilitatea gazdei (Render cu Temurin 25) și limitele Neon se verifică în Faza 3.

## Rulare locală

```bash
cd backend
cp .env.example .env        # opțional; fără el aplicația pornește fără cheie
./mvnw spring-boot:run      # http://localhost:8080  (JAVA_HOME trebuie să indice JDK 25)
```

- Status: `http://localhost:8080/api/v1/health`
- OpenAPI: `http://localhost:8080/v3/api-docs` · Swagger UI: `http://localhost:8080/swagger-ui.html`
- Sănătate pentru gazdă: `http://localhost:8080/actuator/health`
