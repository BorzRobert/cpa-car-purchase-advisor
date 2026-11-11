---
name: pre-deploy
description: Verifică proiectul CPA înainte de publicare în Vercel.
---

# Pre-deploy

Rulează această listă înainte de fiecare publicare. Comenzile npm se rulează din folderul `frontend/`.

1. Mută temporar `frontend/.env.local` în afara proiectului și rulează `npm run build`. Build-ul trebuie să treacă fără nicio cheie.
2. Rulează `npm run format:check`.
3. Verifică faptul că `docs/requirements.md` descrie starea curentă.
4. Pentru fiecare variabilă din `frontend/.env.example`, verifică în Vercel → Project → Settings → Environment Variables că există atât pe `Production`, cât și pe `Preview`. Directorul rădăcină al proiectului Vercel este `frontend`.
5. Dacă ai adăugat sau modificat o variabilă, fă Redeploy pentru deployment-ul relevant.
6. Verifică URL-ul Preview pe telefon, apoi URL-ul Production după push-ul pe `main`.

Nu urca niciodată `.env.local` și nu scrie valori reale în `docs/`, `.env.example` sau în loguri.
