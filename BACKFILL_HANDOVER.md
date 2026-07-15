# Handover: Firestore backfill of `profileId` / `isDeleted`

## Goal

Write explicit `profileId` and `isDeleted` fields onto every Firestore document
that currently lacks them, across **all users**, so both the webapp and Android
can filter on those fields **server-side** instead of client-side.

This is a standalone one-time ops task. It is **not** required for the paging
work — it's the enabler that lets the webapp's paged read path use server-side
`where(...)` filters cleanly. Do the backfill first if paging the webapp;
Android paging (local Room union + keyset) does not depend on it.

## Why this is needed (background)

- The app is multi-profile: every entry/tag doc should carry a `profileId`
  (default is the literal string `"default"`, matching Android's
  `DEFAULT_PROFILE_ID`). Soft-deletes use `isDeleted` (bool).
- **Pre-multi-profile documents exist** that have no `profileId` field, and some
  older docs predate `isDeleted`. Today both surfaces tolerate this by defaulting
  a **missing** value client-side: Android via `?: DEFAULT_PROFILE_ID` on pull;
  the webapp via `?? 'default'` / treating missing `isDeleted` as live, filtering
  **client-side** (see the "Web app" + data-model notes in `CLAUDE.md`).
- Firestore **cannot** query for these docs by that field: a filter like
  `where('profileId','==','default')` only matches docs that *have* the field
  (a doc absent from the field's index is invisible to any query mentioning it —
  there is no "field does not exist" operator, and `or()` doesn't help). So
  server-side filtering silently drops that older history until the field exists
  on every doc. Backfilling makes the docs indexable → server-side filtering safe.

## Collections to backfill

Under `users/{uid}/`:

- `poop_entries`  → `profileId`, `isDeleted`
- `food_entries`  → `profileId`, `isDeleted`
- `note_entries`  → `profileId`, `isDeleted`
- `poop_tags`     → `profileId`, `isDeleted`
- `note_tags`     → `profileId`, `isDeleted`
- `profiles`      → `isDeleted` only (profiles are keyed by their own id; do NOT
  add a `profileId` to a profile doc)

Defaults to write when the field is **absent**: `profileId = "default"`,
`isDeleted = false`.

## Approach: admin script (run once over all users)

Prefer an **admin script** using the Firebase Admin SDK + a service account,
iterating every user, over a per-client migration (a client migration only ever
fixes users who happen to log in). Run it once from a trusted environment.

Sketch (Node, Admin SDK):

```js
const admin = require('firebase-admin');
admin.initializeApp({ credential: admin.credential.cert(require('./service-account.json')) });
const db = admin.firestore();

const COLLECTIONS = ['poop_entries','food_entries','note_entries','poop_tags','note_tags'];

async function backfillUser(uid) {
  // entry/tag collections: profileId + isDeleted
  for (const col of COLLECTIONS) {
    const snap = await db.collection(`users/${uid}/${col}`).get();
    await commitPatches(snap, (data) => {
      const patch = {};
      if (data.profileId === undefined) patch.profileId = 'default';
      if (data.isDeleted === undefined) patch.isDeleted = false;
      return patch;
    });
  }
  // profiles: isDeleted only, never profileId
  const profiles = await db.collection(`users/${uid}/profiles`).get();
  await commitPatches(profiles, (data) =>
    data.isDeleted === undefined ? { isDeleted: false } : {});
}

async function commitPatches(snap, makePatch) {
  let batch = db.batch(), n = 0;
  for (const doc of snap.docs) {
    const patch = makePatch(doc.data());
    if (Object.keys(patch).length === 0) continue;
    batch.update(doc.ref, patch);
    if (++n === 450) { await batch.commit(); batch = db.batch(); n = 0; }
  }
  if (n > 0) await batch.commit();
}

async function main() {
  // iterate all users: list uids under the top-level `users` collection
  const users = await db.collection('users').listDocuments();
  for (const u of users) await backfillUser(u.id);
}
main().catch(e => { console.error(e); process.exit(1); });
```

## Non-negotiable constraints

- **Do NOT touch `updatedAt`.** The backfill must not set/bump `updatedAt`
  (no `serverTimestamp()`). Android's SyncWorker pulls deltas by `updatedAt`;
  bumping it would make every device re-download every entry. Write only the
  missing fields.
- **Idempotent.** Guard every write with `=== undefined` so the field is only
  added when absent. Re-running must be a no-op for already-backfilled docs.
  Never overwrite an existing `profileId`/`isDeleted` value.
- **Batch cap.** Firestore batches cap at 500 writes — chunk (script uses 450).
- **`profiles` collection:** add `isDeleted` only. Never add `profileId` to a
  profile doc (a profile is identified by its own doc id).
- **Verify a sample** after running: pick a user known to have old
  pre-multi-profile entries, confirm those docs now have `profileId: "default"`
  and `isDeleted: false`, and confirm the app still shows their old history.

## After the backfill (follow-up, not part of this task)

Once every doc has the fields, the webapp's paged read path can switch to
server-side `where('profileId','==', pid)` + `where('isDeleted','==', false)` +
`orderBy('occurredAt','desc')` + `startAfter(cursor)` per collection (three-cursor
merge), which requires the corresponding **composite indexes** (filter fields +
`occurredAt`) — add those to `firestore.indexes.json`. Keep Android's pull-side
`?: DEFAULT_PROFILE_ID` fallback regardless; it's cheap insurance for any doc
written by an old client after the backfill.

---

### Context: the paging work this unblocks (being done in a separate session)

- **Android (local Room):** keyset-paged `UNION ALL` across `poop_entries`,
  `food_entries`, `note_entries` (thin `id, occurredAt, kind` projection),
  `WHERE occurredAt < :lastSeen ORDER BY occurredAt DESC LIMIT :n`, then hydrate
  the page's ids. The currently-loaded range stays a `Flow` (live), older pages
  appended on scroll. Firestore is only backup/sync on Android — no Firestore
  paging there.
- **Webapp (Firestore direct, no local cache):** three-cursor merge — page each
  collection with `orderBy('occurredAt','desc')` + `limit` + `startAfter`, merge
  and trim to the page. Server-side filtering (post-backfill) removes the
  "short page after client-side filter" problem.
