#!/usr/bin/env node
/**
 * One-time Firestore backfill: add explicit `profileId` and `isDeleted` fields
 * to every document that currently lacks them, across ALL users.
 *
 * Why: pre-multi-profile docs have no `profileId`, and some old docs predate
 * `isDeleted`. Firestore cannot query by a field a doc doesn't have (there is no
 * "field does not exist" operator), so those docs are invisible to any
 * server-side `where('profileId', ...)` / `where('isDeleted', ...)` filter until
 * the field physically exists. Backfilling makes them indexable so the webapp's
 * paged read path can filter server-side. See BACKFILL_HANDOVER / CLAUDE.md.
 *
 * Guarantees:
 *   - Idempotent: only writes a field when it is `undefined`. Re-running is a
 *     no-op for already-backfilled docs. Never overwrites an existing value.
 *   - Does NOT touch `updatedAt`. Android's SyncWorker pulls deltas by
 *     `updatedAt`; bumping it would make every device re-download everything.
 *   - Batched at 450 writes (Firestore caps batches at 500).
 *   - `profiles` collection gets `isDeleted` only, never `profileId`
 *     (a profile is identified by its own doc id).
 *
 * Usage:
 *   export GOOGLE_APPLICATION_CREDENTIALS=./service-account.json
 *   node backfill.js --dry-run     # report what WOULD change, write nothing
 *   node backfill.js               # perform the backfill
 *   node backfill.js --uid=<uid>   # limit to a single user (handy for verify)
 *
 * The service-account key is full admin over Firestore — keep it out of git
 * (see .gitignore) and rotate/delete it when you're done.
 */

'use strict';

const admin = require('firebase-admin');

const DRY_RUN = process.argv.includes('--dry-run');
const UID_ARG = (process.argv.find((a) => a.startsWith('--uid=')) || '').split('=')[1];

const DEFAULT_PROFILE_ID = 'default'; // matches Android's DEFAULT_PROFILE_ID
const BATCH_LIMIT = 450; // Firestore hard cap is 500; leave headroom.

// Collections that get BOTH profileId and isDeleted.
const ENTRY_COLLECTIONS = [
  'poop_entries',
  'food_entries',
  'note_entries',
  'poop_tags',
  'note_tags',
];

admin.initializeApp({ credential: admin.credential.applicationDefault() });
const db = admin.firestore();

/** Patch for entry/tag docs: add profileId + isDeleted only when absent. */
function entryPatch(data) {
  const patch = {};
  if (data.profileId === undefined) patch.profileId = DEFAULT_PROFILE_ID;
  if (data.isDeleted === undefined) patch.isDeleted = false;
  return patch;
}

/** Patch for profile docs: add isDeleted only, never profileId. */
function profilePatch(data) {
  return data.isDeleted === undefined ? { isDeleted: false } : {};
}

/**
 * Walk a collection snapshot, apply `makePatch`, and commit in <=BATCH_LIMIT
 * chunks. Returns { scanned, patched }.
 */
async function commitPatches(snap, makePatch) {
  let batch = db.batch();
  let inBatch = 0;
  let patched = 0;

  for (const doc of snap.docs) {
    const patch = makePatch(doc.data());
    if (Object.keys(patch).length === 0) continue;
    patched++;
    if (DRY_RUN) continue;

    batch.update(doc.ref, patch);
    if (++inBatch === BATCH_LIMIT) {
      await batch.commit();
      batch = db.batch();
      inBatch = 0;
    }
  }
  if (!DRY_RUN && inBatch > 0) await batch.commit();

  return { scanned: snap.size, patched };
}

async function backfillCollection(uid, col, makePatch) {
  const snap = await db.collection(`users/${uid}/${col}`).get();
  const { scanned, patched } = await commitPatches(snap, makePatch);
  if (patched > 0 || scanned > 0) {
    console.log(`  ${col}: ${patched}/${scanned} ${DRY_RUN ? 'would be ' : ''}patched`);
  }
  return patched;
}

async function backfillUser(uid) {
  console.log(`user ${uid}`);
  let total = 0;
  for (const col of ENTRY_COLLECTIONS) {
    total += await backfillCollection(uid, col, entryPatch);
  }
  total += await backfillCollection(uid, 'profiles', profilePatch);
  return total;
}

async function main() {
  if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    console.error(
      'Set GOOGLE_APPLICATION_CREDENTIALS to your service-account.json path.',
    );
    process.exit(1);
  }
  console.log(
    `Firestore backfill (profileId / isDeleted)${DRY_RUN ? ' — DRY RUN, no writes' : ''}`,
  );

  const uids = UID_ARG
    ? [UID_ARG]
    : (await db.collection('users').listDocuments()).map((d) => d.id);

  console.log(`${uids.length} user(s) to process\n`);

  let grandTotal = 0;
  for (const uid of uids) grandTotal += await backfillUser(uid);

  console.log(
    `\nDone. ${grandTotal} doc(s) ${DRY_RUN ? 'would be ' : ''}patched across ${uids.length} user(s).`,
  );
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
