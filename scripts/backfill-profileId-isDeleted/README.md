# Backfill: `profileId` / `isDeleted`

One-time ops script that writes explicit `profileId` and `isDeleted` fields onto
every Firestore document that currently lacks them, across **all users**, so both
the webapp and Android can filter on those fields **server-side** instead of
client-side.

## Why

Pre-multi-profile docs have no `profileId`, and some old docs predate
`isDeleted`. Firestore has no "field does not exist" query operator, so a doc
missing a field is invisible to any `where('profileId', ...)` /
`where('isDeleted', ...)` filter. Today both surfaces tolerate this by defaulting
the missing value **client-side**. Backfilling makes the docs indexable so the
webapp's paged read path can switch to server-side filtering. See
`BACKFILL_HANDOVER.md` / `CLAUDE.md`.

## What it does

Under `users/{uid}/`:

| Collection | Fields added (only when absent) |
| --- | --- |
| `poop_entries`, `food_entries`, `note_entries`, `poop_tags`, `note_tags` | `profileId: "default"`, `isDeleted: false` |
| `profiles` | `isDeleted: false` only — **never** `profileId` |

Guarantees:

- **Idempotent** — a field is written only when it is `undefined`. Re-running is a
  no-op for already-backfilled docs; existing values are never overwritten.
- **Does not touch `updatedAt`** — Android's SyncWorker pulls deltas by
  `updatedAt`, so bumping it would force every device to re-download everything.
- **Batched at 450 writes** (Firestore caps batches at 500).

## Prerequisites

A **service-account key** for the Firebase project (`logrhythm-207ac`):
Firebase console → Project settings → Service accounts → *Generate new private
key*. Save it as `service-account.json` in this directory.

> ⚠️ The key is full admin over Firestore. It is git-ignored — do not commit it,
> and rotate/delete it once the backfill is done.

## Run

```bash
cd scripts/backfill-profileId-isDeleted
npm install
export GOOGLE_APPLICATION_CREDENTIALS=./service-account.json

# 1. Dry run first — reports counts, writes nothing.
npm run backfill:dry

# 2. Real run.
npm run backfill

# Limit to a single user (handy for the verify step below):
node backfill.js --uid=<uid>
node backfill.js --uid=<uid> --dry-run
```

## Verify afterwards

Pick a user known to have old pre-multi-profile entries. Confirm those docs now
have `profileId: "default"` and `isDeleted: false` (Firestore console or
`node backfill.js --uid=<uid> --dry-run`, which should report `0` remaining), and
confirm the app still shows their old history.

## After the backfill (follow-up)

Once every doc has the fields, the webapp's paged read path can move to
server-side `where('profileId','==', pid)` + `where('isDeleted','==', false)` +
`orderBy('occurredAt','desc')` + `startAfter(cursor)`, which needs the matching
composite indexes in `firestore.indexes.json`. Keep Android's pull-side
`?: DEFAULT_PROFILE_ID` fallback regardless — cheap insurance for any doc written
by an old client after the backfill.
