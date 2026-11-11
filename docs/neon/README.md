# Neon (PostgreSQL)

## Ce face

Neon găzduiește baza de date PostgreSQL de producție a CPA. Discul serviciului de pe Render este efemer (se pierde la fiecare deploy, repornire sau adormire), deci orice date care trebuie să supraviețuiască — sesiuni, profiluri, conversații, catalog — stau în Neon ([ADR 0005](../adr/0005-baza-de-date.md)). Local și în teste backend-ul folosește H2 în memorie; **aceleași migrări Flyway** rulează pe ambele.

**Adăugată la:** Faza 3 (conectare și migrarea de bază goală `V1__baseline.sql`); Faza 4 adaugă schema reală (`V2__domain_schema.sql`) și catalogul de pornire (`V3__seed_catalogue.sql`), aplicate automat pe Neon la primul deploy al backend-ului din Faza 4.
**Obligatorie / opțională:** obligatorie în producție (profilul `prod` refuză să pornească fără `SPRING_DATASOURCE_URL`); inutilă pentru dezvoltarea locală.

## Cont & chei

- Cont: <https://console.neon.tech/signup> — planul **Free** (fără card). Autentificarea cu GitHub e cea mai simplă.
- Creează un proiect: **Projects → New project**.
  - **Region:** `AWS Europe (Frankfurt)` — aceeași zonă geografică ca serviciul Render (`frankfurt` în `render.yaml`), ca latența JDBC să fie mică. Regiunea nu se poate schimba după creare.
  - **Postgres version:** implicită (verificat local cu 17).
  - Numele proiectului și al bazei de date sunt la alegere; branch-ul implicit se numește `main`.
- Parola rolului se afișează **o singură dată** la creare; dacă o pierzi, o resetezi din consola Neon (Branch → Roles → Reset password).
- Datele de conexiune: **Dashboard → Connect**. Alege branch-ul, baza și rolul, **dezactivează „Connection pooling”** (vezi mai jos de ce) și copiază host-ul, baza, utilizatorul și parola.

### De ce conexiune directă, nu „pooled”

Adresa „pooled” (`…-pooler…`) trece prin PgBouncer în mod tranzacție, care nu se împacă bine cu blocările consultative pe care Flyway le ia la migrare. Backend-ul are deja propriul pool (Hikari, 5 conexiuni), deci adresa **directă** (fără `-pooler` în host) este cea corectă.

## Variabile de mediu

Neon nu adaugă nicio variabilă în frontend. În backend se folosesc variabilele standard Spring Boot (profilul `prod` le cere):

| Variabilă                    | Fișier / loc                                      | Ajunge în browser? |
| ---------------------------- | ------------------------------------------------- | ------------------ |
| `SPRING_PROFILES_ACTIVE`     | dashboard Render (`render.yaml`: valoarea `prod`) | NU                 |
| `SPRING_DATASOURCE_URL`      | `backend/.env` (opțional), dashboard Render       | NU                 |
| `SPRING_DATASOURCE_USERNAME` | `backend/.env` (opțional), dashboard Render       | NU                 |
| `SPRING_DATASOURCE_PASSWORD` | `backend/.env` (opțional), dashboard Render       | NU                 |

Rândurile din `backend/.env.example` (comentate; fără valori reale):

```dotenv
# SPRING_PROFILES_ACTIVE=prod
# SPRING_DATASOURCE_URL=
# SPRING_DATASOURCE_USERNAME=
# SPRING_DATASOURCE_PASSWORD=
```

**Formatul URL-ului este JDBC**, nu cel din consola Neon, iar utilizatorul și parola se dau separat:

| Consola Neon (nu se folosește ca atare)                            | Ce se pune în `SPRING_DATASOURCE_URL`             |
| ------------------------------------------------------------------ | ------------------------------------------------- |
| `postgresql://<utilizator>:<parola>@<host>/<baza>?sslmode=require` | `jdbc:postgresql://<host>/<baza>?sslmode=require` |

- Nu folosim `DATABASE_URL`: formatul `postgresql://user:parola@…` nu e un URL JDBC, iar parola din el ar ajunge într-o variabilă care se copiază ușor în loguri sau tichete. Cele trei variabile Spring separă secretul (parola) de adresă.
- Păstrează `sslmode=require` (Neon refuză conexiunile necriptate). Parametrul `channel_binding` din șirul Neon se omite: nu este necesar.
- Parola nu se pune niciodată în URL și nu se scrie în repo sau în `docs/`.

## Pași manuali

Ce nu poate face agentul:

1. Creează contul și proiectul Neon (regiunea Frankfurt, conexiune directă) — vezi „Cont & chei”.
2. Copiază **host-ul**, **numele bazei**, **utilizatorul** și **parola** într-un loc temporar (nu în repo).
3. Pune cele trei variabile `SPRING_DATASOURCE_*` în dashboard-ul Render, în formatul de mai sus ([`../render/README.md`](../render/README.md), pasul „Creează serviciul din Blueprint”).
4. Nu rula manual nicio migrare: Flyway o aplică la pornirea backend-ului. După primul deploy verifică în consola Neon (**SQL Editor**) că există tabelul `flyway_schema_history` cu o linie pentru versiunea `1`.
5. Dacă expui parola din greșeală (chat, tichet, commit): resetează-o în consola Neon, apoi actualizează variabila în Render. Ștergerea liniei nu e suficientă.
6. Reinstalare pe alt cont: refă proiectul, rulează backend-ul o dată (Flyway recreează schema); datele nu se mută automat — vezi „Copie de siguranță”.

## Cost & limite

Verificat la **2026-11-11** pe [neon.com/docs/introduction/plans](https://neon.com/docs/introduction/plans) (pagina era actualizată la 2026-11-04) și pe [Scale to zero](https://neon.com/docs/introduction/scale-to-zero):

| Resursă (planul Free) | Limită                                                                          |
| --------------------- | ------------------------------------------------------------------------------- |
| Cost                  | 0 USD/lună                                                                      |
| Stocare               | **1 GB pe proiect** (20 GB pe cont)                                             |
| Compute               | **100 CU-ore pe proiect** pe lună, autoscaling până la 2 CU (8 GB RAM)          |
| Scale to zero         | **după 5 minute** de inactivitate; setarea e fixă pe planul Free                |
| Trezire               | „în câteva sute de milisecunde” la prima interogare, conform documentației Neon |
| Transfer public       | 5 GB pe proiect                                                                 |
| Branch-uri            | 10 pe proiect                                                                   |
| Istoric / restaurare  | 6 ore (restaurare instantanee fără cost), 1 snapshot manual                     |
| SLA                   | niciunul                                                                        |

Ce contează pentru CPA:

- **Cold start-ul bazei e mic** (sute de ms) față de cel al serviciului Render (~1 minut + pornirea JVM). Backend-ul îl absoarbe prin pool: o conexiune mai veche de 4 minute nu se refolosește, o conexiune inactivă e verificată înainte de utilizare, iar la pornire Hikari așteaptă până la 60 s după bază, iar Flyway reîncearcă conectarea (`application-prod.yml`). Pornirea fără bază și recuperarea ei după revenire au fost verificate local (container Postgres oprit, apoi pornit, în timp ce backend-ul pornea).
- **Cota de 100 CU-ore** se consumă doar cât computul e treaz. De aceea `/actuator/health` **nu interoghează baza** în producție (`management.health.db.enabled=false`) și Hikari nu folosește `keepalive-time`: altfel health check-ul Render (la câteva secunde) ar ține Neon treaz non-stop și cota s-ar termina în câteva zile. Disponibilitatea bazei **nu** are încă un grup de readiness separat: era prevăzut pentru Faza 4, dar este amânat la Faza 9 (hardening), ca health check-ul Render să rămână fără interogări. Până atunci, o bază indisponibilă se vede ca `500` pe endpoint-urile cu date, nu pe `/actuator/health`.
- **1 GB** acoperă cu mult catalogul curat și conversațiile text ale unui proiect de curs; urmărește consumul în consola Neon.

Verifică cifrele actuale înainte să te bazezi pe ele — ofertele planurilor gratuite se schimbă.

### Copie de siguranță

Planul Free păstrează doar 6 ore de istoric. Un export manual (`pg_dump`) înainte de schimbări mari de schemă este suficient pentru acest proiect:

```bash
# parola se cere interactiv; nu o pune în comandă
pg_dump "postgresql://<utilizator>@<host>/<baza>?sslmode=require" --no-owner -f cpa-backup.sql
```

## Verificare

1. Local, cu un Postgres de probă (nu e nevoie de Neon pentru asta) — dovedește că Flyway rulează și pe PostgreSQL:
   ```bash
   docker run -d --name cpa-pg -p 5432:5432 -e POSTGRES_PASSWORD=local -e POSTGRES_DB=cpa postgres:17-alpine
   # din backend/, cu JDK 25 în JAVA_HOME:
   SPRING_PROFILES_ACTIVE=prod SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/cpa \
   SPRING_DATASOURCE_USERNAME=postgres SPRING_DATASOURCE_PASSWORD=local \
   CORS_ALLOWED_ORIGINS=http://localhost:3000 ./mvnw spring-boot:run
   docker exec cpa-pg psql -U postgres -d cpa -c "select version, description, success from flyway_schema_history;"
   ```
   Rezultat așteptat: trei linii, `1 | baseline`, `2 | domain schema`, `3 | seed catalogue`, toate cu `t` (în Faza 3 era doar prima). Cu aceeași pornire: `docker exec cpa-pg psql -U postgres -d cpa -c "select count(*) from vehicle_variant;"` → `21`.
2. Pe Neon, după primul deploy din Faza 4: în **SQL Editor** rulează aceeași interogare pe `flyway_schema_history` — trebuie să apară `V2` și `V3`. Migrările sunt aplicate la pornirea backend-ului; **nu se editează niciodată** după ce au rulat pe Neon (Flyway ar refuza pornirea din cauza sumei de control): orice schimbare de schemă sau de catalog este o migrare nouă.
3. Backend pornit pe Render: `GET <URL-backend>/api/v1/health` răspunde `200` (endpoint-ul nu atinge baza; pornirea cu succes dovedește că Flyway s-a conectat).

Erori tipice:

| Eroare în logurile Render                                   | Ce înseamnă                                                                         |
| ----------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| `Could not resolve placeholder 'SPRING_DATASOURCE_URL'`     | Variabila lipsește din Render; profilul `prod` o cere intenționat.                  |
| `password authentication failed`                            | Utilizator sau parolă greșite (sau parolă resetată în Neon, neschimbată în Render). |
| eroare SSL / „connection is insecure”                       | Lipsește `?sslmode=require` din URL.                                                |
| `Connection … refused` sau timeout după 60 s la pornire     | Host greșit, proiect șters sau suspendat; verifică starea proiectului în Neon.      |
| `Unable to obtain connection` după mult timp de inactivitate | Baza se trezește; cererea următoare reușește. Dacă persistă, vezi starea Neon.      |
