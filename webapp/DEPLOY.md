# LogRhythm web app — deployment plan

Everything you need to get `webapp/` live on Firebase Hosting, in order.
Firebase project: **`logrhythm-207ac`** (the same one the Android app uses — do **not** create a new project).
Target URL once deployed: **https://logrhythm-207ac.web.app**

Prereqs already in place: Firebase CLI v15 installed, hosting config (`firebase.json` + `.firebaserc`) scaffolded
at the repo root, and the app builds locally.

---

## 1. Create a Web app + get its config

The Android `google-services.json` does **not** work for web — you need a separate Web app registration
(same project, so the data is shared).

1. Firebase Console → project **logrhythm-207ac** → ⚙️ **Project settings**.
2. Scroll to **Your apps** → if there's no Web app, click **Add app → Web (`</>`)**. Name it e.g. "LogRhythm Web".
   (You do **not** need to enable Firebase Hosting in that wizard — we configure it separately below.)
3. Copy the `firebaseConfig` values shown.

Then create the local env file:

```bash
cd webapp
cp .env.local.example .env.local
```

Fill in `.env.local` from the console values:

```
VITE_FIREBASE_API_KEY=...
VITE_FIREBASE_AUTH_DOMAIN=logrhythm-207ac.firebaseapp.com
VITE_FIREBASE_PROJECT_ID=logrhythm-207ac
VITE_FIREBASE_STORAGE_BUCKET=logrhythm-207ac.firebasestorage.app
VITE_FIREBASE_MESSAGING_SENDER_ID=...
VITE_FIREBASE_APP_ID=...
```

> `.env.local` is gitignored — it stays on your machine and never gets committed.

---

## 2. Enable Google sign-in

The web app signs in with a Google popup.

- Console → **Authentication → Sign-in method** → ensure **Google** is **Enabled**.
  (It likely already is for the Android app; if so, nothing to do.)

---

## 3. Confirm Firestore security rules

The web app reads/writes the **same** `users/{uid}/…` paths as Android, so if Android already syncs, your rules
already cover the web app — **no change needed**.

Sanity-check (Console → **Firestore → Rules**) that access is owner-scoped, roughly:

```
match /users/{uid}/{document=**} {
  allow read, write: if request.auth != null && request.auth.uid == uid;
}
```

> Optional: ask Claude to add a version-controlled `firestore.rules` + deploy it with
> `firebase deploy --only firestore:rules`. Currently rules live only in the console.

---

## 4. Test locally first

```bash
cd webapp
npm install        # first time only
npm run dev        # http://localhost:5173
```

Open http://localhost:5173, sign in with Google, and verify:

- [ ] Sign-in popup works (localhost is an authorized domain by default).
- [ ] A default profile appears; you can add/switch profiles in Settings.
- [ ] Logging a poop / food / note shows it on the Home timeline.
- [ ] Entries you created on the **Android app** show up here (and vice-versa) — confirms the sync contract.
- [ ] Calendar + Trends render; entry detail edit/delete works.
- [ ] Theme switch in Settings persists (and reflects on the phone for that profile).

---

## 5. Build & deploy

Run from the **repo root** (where `firebase.json` lives):

```bash
cd webapp && npm run build && cd ..   # produces webapp/dist
firebase login                        # once, if not already logged in (run as `!firebase login`)
firebase deploy --only hosting
```

Deploy output prints the live URL: **https://logrhythm-207ac.web.app**
(Firebase auto-adds `*.web.app` / `*.firebaseapp.com` to Authentication → Authorized domains, so Google sign-in
works on the deployed site with no extra step.)

---

## 6. Post-deploy verification

- [ ] Visit https://logrhythm-207ac.web.app and sign in.
- [ ] Confirm your existing data loads (same account as Android).
- [ ] Create an entry on the web app → confirm it appears on the phone after its next sync.

### Rollback if needed
Firebase keeps previous hosting releases:

```bash
firebase hosting:releases:list
firebase hosting:rollback
```

---

## Open decisions (let Claude know)

- [ ] Commit `firebase.json` + `.firebaserc` (currently untracked at repo root)?
- [ ] Add a version-controlled `firestore.rules`?
- [ ] Open a PR for the `webapp` branch into `main`?
- [ ] (Optional) Custom domain instead of `*.web.app` — set up in Console → Hosting.

---

## Quick reference

| Item | Value |
| --- | --- |
| Firebase project | `logrhythm-207ac` |
| Live URL | https://logrhythm-207ac.web.app |
| Env file | `webapp/.env.local` (from `.env.local.example`) |
| Build output | `webapp/dist` |
| Hosting config | `firebase.json`, `.firebaserc` (repo root) |
| Local dev | `cd webapp && npm run dev` → http://localhost:5173 |
| Deploy | `firebase deploy --only hosting` |
