# Adaugă un provider LLM

Folosește această rețetă când adaugi un provider nou în SkillForge. Abstracția este justificată de al doilea provider: adăugarea trebuie să atingă doar registrul, adaptorul serverului și documentația, nu composer-ul sau preferințele.

## Ordinea obligatorie

1. Instalează pachetul SDK al providerului și verifică versiunea compatibilă cu Vercel AI SDK.
2. Citește documentația curentă a providerului și notează id-urile reale ale modelelor; nu inventa id-uri.
3. Adaugă o intrare în `src/lib/providers.ts`. Registrul nu conține chei, `process.env` sau apeluri SDK.
4. Adaugă ramura providerului în `src/lib/providers.server.ts`, în `getModel`; acesta este singurul loc care instanțiază SDK-uri.
5. Adaugă variabila server-only în `.env.example` și configureaz-o manual în Vercel pentru Preview și Production, apoi fă Redeploy.
6. Creează `docs/<provider>/README.md` după `docs/_template/README.md` și adaugă rândul în `docs/README.md`.
7. Verifică faptul că selectorul din composer și orice listă din preferințe s-au actualizat singure din registru.
8. Rulează `sh scripts/sync-skills.sh`, `npm run format`, `npm run build` și testul manual cu cheia prezentă și absentă.

## Semne că abstracția a curs

- A fost nevoie de cod nou în composer pentru a afișa providerul.
- A fost nevoie de o a doua listă de provideri în preferințe.
- A apărut `if (provider === ...)` în ruta de chat, în composer sau în preferințe.
- O cheie ajunge în `src/lib/providers.ts`, în client sau într-un răspuns API.

În aceste cazuri, repară registrul/adaptorul serverului și datele de disponibilitate, nu ocoli registrul cu cod special în interfață.
