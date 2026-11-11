# Render (backend Spring Boot)

## Ce face

Render găzduiește backend-ul Java al CPA ca **serviciu web Docker**: construiește imaginea din `backend/Dockerfile` la fiecare push și o servește pe un URL public HTTPS (`https://<nume>.onrender.com`). Fără el, backend-ul rulează doar pe calculatorul tău, iar frontend-ul de pe Vercel nu are cu cine vorbi. Infrastructura este descrisă în [`render.yaml`](../../render.yaml) (Blueprint), ca setările să nu depindă de click-uri făcute din memorie.

**Adăugată la:** Faza 3 (walking skeleton). Alegerea gazdei: [ADR 0004](../adr/0004-gazduire-backend.md).
**Obligatorie / opțională:** obligatorie pentru un URL public; nu e nevoie pentru dezvoltarea locală.

## Cont & chei

- Cont: <https://dashboard.render.com/register> — autentificare cu GitHub. Planul de **serviciu** necesar este **Free** (se alege per serviciu, nu per cont).
- Render trebuie să poată citi repo-ul: la prima conectare acceptă instalarea aplicației GitHub „Render” pe repo-ul `cpa-car-purchase-advisor` (poți alege doar acest repo).
- **Nu există nicio cheie de API Render de configurat** pentru acest flux. Secretele aplicației (parola bazei, cheia OpenAI) se introduc în dashboard, nu în repo.

## Variabile de mediu

Toate se setează în **Render → serviciul `cpa-backend` → Environment**. Niciuna nu ajunge în browser.

| Variabilă                    | Valoare / de unde vine                                                                                  | În `render.yaml`        |
| ---------------------------- | ------------------------------------------------------------------------------------------------------- | ----------------------- |
| `SPRING_PROFILES_ACTIVE`     | `prod` (profilul cu setările pentru Neon, Swagger oprit)                                                | valoare fixă            |
| `SPRING_DATASOURCE_URL`      | `jdbc:postgresql://<host>/<baza>?sslmode=require` — din [Neon](../neon/README.md)                       | `sync: false` (fără valoare) |
| `SPRING_DATASOURCE_USERNAME` | rolul bazei din Neon                                                                                     | `sync: false`           |
| `SPRING_DATASOURCE_PASSWORD` | parola rolului din Neon                                                                                  | `sync: false`           |
| `CORS_ALLOWED_ORIGINS`       | originile frontend-ului, separate prin virgulă, fără `*` — vezi [`../vercel/README.md`](../vercel/README.md) | `sync: false`      |
| `OPENAI_API_KEY`             | **substituent** `your_openai_api_key` până în Faza 5/9; cheia reală se pune doar aici, în dashboard      | `sync: false`           |
| `CPA_LLM_MODEL`              | `gpt-5-mini` deocamdată (ADR 0007) — **de reconfirmat înainte de Faza 5**, vezi [`../openai/README.md`](../openai/README.md) | `sync: false` |
| `CPA_TRUSTED_PROXY_HOPS`     | opțională; implicit `1` în profilul `prod` (ultima intrare din `X-Forwarded-For`). Se setează doar dacă verificarea din Faza 9 arată altceva | nu apare |

`PORT` nu se setează: Render îl injectează (implicit `10000`), iar `Dockerfile` îl citește (`-Dserver.port=${PORT:-8080}`). Verificat local cu `PORT=10000`.

De ce `sync: false`: valoarea se cere o singură dată, la crearea Blueprint-ului, și **nu este rescrisă** la sincronizările următoare. Dacă ar fi în `render.yaml` cu `value:`, fiecare sincronizare ar suprascrie ce ai pus în dashboard (inclusiv cheia reală din Faza 9) și valoarea ar ajunge în Git.

Schimbarea unei variabile în dashboard repornește serviciul automat.

## Pași manuali

Ce nu poate face agentul:

1. **Împinge branch-ul pe GitHub** (Render construiește din GitHub, nu din folderul local) și, dacă lucrezi pe un branch de fază, ține-l la îndemână pentru pasul 3.
2. **Creează baza în Neon** și ai la îndemână host-ul, baza, utilizatorul și parola ([`../neon/README.md`](../neon/README.md)).
3. **Creează serviciul din Blueprint:** Render → **New → Blueprint** → alege repo-ul și branch-ul → Render citește `render.yaml` și arată serviciul `cpa-backend` (Docker, Free, Frankfurt). Completează valorile cerute pentru variabilele `sync: false` (tabelul de mai sus) → **Apply**.
   - Pentru `OPENAI_API_KEY` scrie substituentul `your_openai_api_key` (backend-ul îl tratează ca „fără cheie”). **Nu pune încă o cheie reală.**
4. Așteaptă primul build (câteva minute: descarcă dependențele Maven de la zero) și pornirea. Starea trebuie să devină **Live**.
5. Copiază **URL-ul public** al serviciului (sub numele serviciului, `https://….onrender.com`) — se pune în Vercel ca `NEXT_PUBLIC_API_BASE_URL`.
6. După ce ai URL-ul Vercel (producție și, dacă e cazul, aliasul stabil al branch-ului de test), pune-l în `CORS_ALLOWED_ORIGINS` (Environment → editează → Save; serviciul repornește).
7. **Branch-ul de producție:** după merge în `main`, în **Settings → Build & Deploy → Branch** setează `main` (dacă Blueprint-ul a fost creat dintr-un branch de fază), ca deploy-urile automate să urmărească `main`.
8. Dacă schimbi `render.yaml` ulterior: **Blueprints → (blueprint-ul tău) → Sync**.

## Cost & limite

Verificat la **2026-11-11** pe [render.com/docs/free](https://render.com/docs/free) și [Blueprint spec](https://render.com/docs/blueprint-spec):

| Aspect (serviciu web Free) | Valoare                                                                                         |
| -------------------------- | ----------------------------------------------------------------------------------------------- |
| Resurse                    | **0,1 CPU, 512 MB RAM**                                                                         |
| Adormire                   | după **15 minute** fără trafic de intrare                                                       |
| Trezire                    | „în jur de un minut”; Render afișează o pagină de încărcare browserelor în acest timp           |
| Ore gratuite               | **750 de ore de instanță pe lună** pe workspace (un serviciu adormit nu consumă ore)            |
| Disc                       | efemer; se pierde la redeploy, repornire și adormire (de aceea datele stau în Neon)             |
| Repornire                  | Render poate reporni serviciul oricând                                                          |
| Pre-deploy command, SSH, scalare | indisponibile pe Free (nu avem nevoie: migrările rulează la pornire, prin Flyway)         |
| Port                       | `PORT` injectat de Render, implicit `10000`                                                     |

Ce contează pentru CPA:

- **Imaginea de bază:** `eclipse-temurin:25-jre` (și `25-jdk` pentru build) există pe Docker Hub (verificat 2026-11-11). Render acceptă Dockerfile multi-stage cu imagini publice (documentația Docker Render, 2026-11-11), deci `backend/Dockerfile` se construiește neschimbat. Confirmarea definitivă este primul build — vezi „Observat pe Render”.
- **Memorie:** `-XX:MaxRAMPercentage=60` (heap ≈ 300 MB din 512 MB) lasă loc pentru metaspace și memoria nativă. Un container Docker cu limita de 512 MB a folosit ~270 MiB după pornire (verificat local). Dacă Render raportează „out of memory”, coboară procentul sau treci la un plan plătit.
- **Calea Dockerfile și contextul:** `render.yaml` nu folosește `rootDir`; setează `dockerfilePath: ./backend/Dockerfile` și `dockerContext: ./backend` (relative la rădăcina repo-ului), iar `buildFilter` limitează deploy-urile la `backend/**`. Motiv: documentația Render e contradictorie despre cum se rezolvă calea Dockerfile-ului când `rootDir` e setat. Același lucru la crearea manuală a serviciului: **Root Directory** gol, **Dockerfile Path** `./backend/Dockerfile`, **Docker Build Context Directory** `./backend`.
- **Health check:** `/actuator/health` (2xx în 5 secunde). Nu interoghează baza, ca să nu țină Neon treaz (vezi [`../neon/README.md`](../neon/README.md)). Un deploy nou este considerat reușit abia când health check-ul trece; dacă nu trece în 15 minute, deploy-ul se anulează.
- **Pornire lentă:** cu 0,1 CPU, Spring Boot pornește greu. Pe un container Docker local limitat la 0,25 CPU / 512 MB, aplicația a pornit în ~59 s; pe Render (0,1 CPU) e de așteptat mai mult. Cold start total la prima cerere după adormire = trezirea serviciului (~1 minut) + pornirea JVM. De aceea frontend-ul arată „Se trezește consilierul…" și reîncearcă ping-ul în loc să afișeze o eroare.
- **Cost:** 0 USD pe Free. Depășirea orelor sau a lățimii de bandă suspendă serviciile Free până în luna următoare dacă nu ai card atașat; nu există cheltuială surpriză fără card. Un plan plătit elimină adormirea (se discută în Faza 9 dacă demo-ul o cere).

### Observat pe Render

> Măsurătoare cerută de Faza 3: timpul de la prima cerere până la `200` pe `/actuator/health` după ≥ 15 minute fără trafic. **Încă nu există o valoare măsurată.** La 2026-11-11, trei cereri făcute la 17 și 20 de minute după ultima cerere proprie au primit `200` în ~0,25 s, adică serviciul era deja treaz: ceva din afara măsurătorii îl ține treaz (de exemplu un tab cu frontend-ul deschis, care face ping, sau un monitor de uptime). O astfel de cerere **nu** este un cold start și nu se trece în tabel. Pași pentru o măsurătoare validă: vezi mai jos „Verificare”.

| Dată | Build (min) | Cold start (s) | Observații |
| ---- | ----------- | -------------- | ---------- |
| —    | —           | —              | Netrecută: nu s-a putut obține un serviciu adormit. Imaginea `eclipse-temurin:25-jre` din `backend/Dockerfile` nu a fost modificată pentru Render, iar serviciul răspunde `200` (`version` `0.0.1-SNAPSHOT`); **jurnalul de build din Render nu a fost văzut**, deci confirmarea explicită rămâne de făcut |

## Verificare

Cu URL-ul public al serviciului (înlocuiește `<URL-backend>`):

```bash
curl -s <URL-backend>/actuator/health        # {"groups":["liveness","readiness"],"status":"UP"}
curl -s <URL-backend>/api/v1/health          # {"status":"UP","service":"cpa-backend","version":"…","advisorConfigured":false}
curl -s -o /dev/null -w "%{http_code}\n" <URL-backend>/v3/api-docs   # 404 — Swagger/OpenAPI sunt oprite în producție
```

CORS, de la o origine permisă (ar trebui să vezi `access-control-allow-origin` în răspuns) și de la una nepermisă (`403`):

```bash
curl -s -i -H "Origin: https://<origine-permisa>" <URL-backend>/api/v1/health | grep -i access-control-allow-origin
curl -s -o /dev/null -w "%{http_code}\n" -H "Origin: https://exemplu-nepermis.invalid" <URL-backend>/api/v1/health
```

Cold start: întâi te asiguri că nimic nu mai ține serviciul treaz (închide tab-urile cu frontend-ul, oprește orice monitor de uptime), apoi confirmi în Render → serviciul → **Events/Logs** că a apărut „Service spinning down due to inactivity" (sau starea e „Suspended/idle"). Abia atunci `time curl -s <URL-backend>/actuator/health` — prima cerere durează (pagina de trezire / ~1 minut + JVM), următoarele sunt rapide. O cerere rapidă (< 2 s) după o pauză înseamnă că serviciul nu dormea.

Erori tipice:

| Simptom                                                         | Cauză probabilă                                                                              |
| --------------------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| Build: `Dockerfile` negăsit                                     | `dockerfilePath` / `dockerContext` din `render.yaml` (sau din formular) — vezi nota de mai sus |
| Deploy anulat după 15 minute, „health check failed”             | Aplicația nu a pornit: citește logurile (variabilă lipsă, baza de date greșită)              |
| `Could not resolve placeholder 'SPRING_DATASOURCE_URL'` / `CORS_ALLOWED_ORIGINS` | Variabila `sync: false` nu a fost completată în dashboard                       |
| Procesul dispare fără stack trace Java („Killed”)               | Memorie (OOM) pe 512 MB — vezi nota despre `MaxRAMPercentage`                                |
| Frontend: „Consilierul nu răspunde” deși serviciul e Live       | CORS: originea frontend-ului lipsește din `CORS_ALLOWED_ORIGINS`, sau `NEXT_PUBLIC_API_BASE_URL` e greșit (necesită Redeploy în Vercel) |
| Prima cerere durează ~1–2 minute                                | Cold start normal pe Free; serviciul se trezea                                               |
