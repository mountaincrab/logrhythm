# Food components and structured food logging: implementation plan

## 1. Goal

Replace free-text-only food logging and the caffeine/alcohol flags on notes with a structured food catalogue that can represent reusable foods and drinks, their serving size, and any number of quantitative tracked components.

The first built-in components are:

- **Caffeine**, measured in `mg`.
- **Alcohol**, measured in `UK units`.

The model must remain generic so components such as fibre, sugar, water, sodium, or protein can be added later without a database migration. A food entry can contain multiple catalogue items and/or custom free-text lines. Trends calculate component totals from the current catalogue definitions, so editing a food item's name or component amounts changes historical display and totals, matching the existing medicine behaviour.

## 2. Agreed product decisions

1. Caffeine and alcohol are tracked components, not special columns and not note tags.
2. `TrackedComponent` is the domain/database term. The Android UI can use the friendlier label **Food components**.
3. A tracked component owns its canonical unit. For example, Caffeine owns `mg`; Alcohol owns `UK units`.
4. A food item has a serving amount and unit, analogous to medicine strength/units. Example: Bottle of beer, `500 ml`.
5. A food item has no food/drink type. The distinction does not affect storage, calculations, or navigation, so names and search are sufficient.
6. The food-item/component association stores the amount of that component in one food item. Example: one 500 ml bottle contains `2.5 UK units` of Alcohol.
7. A food-entry line stores quantity. Two bottles therefore contribute `2 × 2.5 = 5 UK units`.
8. Catalogue-backed entry lines retain a reference to the food item and do not snapshot its name, serving size, or components. Catalogue edits deliberately affect history.
9. Custom entry lines remain available. They contain free text and may optionally carry direct component totals.
10. Catalogue definitions that have been used are archived rather than deleted, preserving references and sync consistency.
11. The caffeine/alcohol controls and fields are removed from notes. Existing note tags remain unchanged.
12. There is no semantic migration of old free-text foods or note caffeine/alcohol flags. The schema upgrade remains safe, but old data is not inferred or transformed into the new catalogue.

## 3. Design diagrams

- [Current food/note model](./01-current-model.png)
- [Proposed Android Room ERD](./02-room-erd.png)
- [Proposed Firestore model](./03-firestore-model.png)

These diagrams are stored as PNG files so repository viewers and chat clients do not expose raw SVG markup.

Android screen concepts:

- [Add a food component](./04-add-food-component.png)
- [Add a food item](./05-add-food-item.png)
- [Log multiple food items](./06-log-multiple-food-items.png)
- [Food component trends](./07-food-component-trends.png)

## Implementation status (2026-09-08)

The first implementation is complete across Android, Room, sync, Firestore, and the web app:

- Room schema 15 contains the generic component catalogue, food items (including display icons), item/component joins,
  ordered entry lines, and custom-line component totals. The 13→14 migration deliberately
  discards legacy free-text food rows and preserves notes without the old booleans.
- Android has Food library management, component and food-item editors, multi-item/custom food
  logging, live catalogue resolution in history, and per-component trend cards.
- Firestore syncs definitions before their references and embeds owned component maps/entry lines.
  Firestore rules and index exemptions cover the new collections and payload bounds.
- The web app uses the same document contract and provides matching catalogue, logging, timeline,
  detail, and component-trend flows. Both surfaces offer `7d`, `30d`, `90d`, `6mo`, `1y`, and
  `All` trend ranges.
- Caffeine and alcohol have been removed from notes on both surfaces and are now seeded ordinary
  tracked components.

Verification completed: Room processing emitted schema 14, the web production build passed, the
JSON/index files parse, and the working-tree diff has no whitespace errors. An earlier constrained
Kotlin compile also passed, but it used a temporary generated `R` stub to avoid resource processing
and is not treated as a full Android build. Standard Android compilation/resource packaging and JVM
migration-test execution are not runnable on this ARM64 host because the Gradle dependency supplies
an x86-64-only AAPT2 executable; this is an environment limitation, not a test pass. No Firestore
rules-emulator setup exists in this repository, so rules have been reviewed but not emulator-tested.

The component-day drill-down, symptom overlay, recent/frequent ranking, and an inline component
editor inside the food-item dialog remain optional enhancements. They are not required for the
catalogue/reference model, multi-item logging, or component totals delivered here.

## 4. Domain model and invariants

### 4.1 TrackedComponent

| Field | Type | Notes |
| --- | --- | --- |
| `id` | String | UUID for user-created components; deterministic ID for defaults |
| `userId` | String | Owner |
| `profileId` | String | Profile scope |
| `name` | String | Display name, unique case-insensitively within active profile components |
| `unit` | String | Canonical display/calculation unit, e.g. `mg`, `g`, `UK units` |
| `sortOrder` | Int | Stable catalogue ordering |
| `isArchived` | Boolean | Hidden from new selection, retained for history |
| `createdAt` / `updatedAt` | Instant | Audit/sync timestamps |
| `syncStatus` | SyncStatus | Local sync state |

Rules:

- Name can change and history resolves the new name.
- Unit is editable until first use. After a component is referenced by a food item or custom entry line, the UI locks the unit. Changing units without conversion would silently reinterpret history.
- Archive is allowed after use; hard delete is allowed only when no references exist and the component has never synced, otherwise archive.
- Caffeine and Alcohol are created for every profile by a startup invariant, not only by a Room migration. Deterministic per-profile IDs make creation idempotent across devices.

### 4.2 FoodItem

| Field | Type | Notes |
| --- | --- | --- |
| `id` | String | Stable catalogue identity |
| `userId` / `profileId` | String | Ownership and scope |
| `name` | String | e.g. `Bottle of beer` |
| `icon` | String | One user-selected character, defaulting to `🍴` |
| `amount` | Decimal/String | Serving amount, stored losslessly |
| `unit` | String | Serving unit, e.g. `ml`, `g`, `bottle`, `cup` |
| `sortOrder` | Int | Stable catalogue ordering |
| `isArchived` | Boolean | Excluded from new pickers, retained for history |
| timestamps / `syncStatus` | standard fields | Same conventions as other synced entities |

The serving fields describe what one quantity means. `amount = 500`, `unit = ml`, `quantity = 2` renders as `2 × 500 ml`. Component amounts remain attached to one food item, not to one millilitre.

### 4.3 FoodItemComponent

This is the many-to-many join between `FoodItem` and `TrackedComponent`.

| Field | Type | Notes |
| --- | --- | --- |
| `foodItemId` | String | Composite primary key, FK to FoodItem |
| `componentId` | String | Composite primary key, FK to TrackedComponent |
| `amount` | Double | Amount of the component in one FoodItem |

Example: `(beerId, alcoholId, 2.5)` means one Bottle of beer contains 2.5 UK units.

The join row is owned by the food item for sync purposes. It does not need independent timestamps or sync status: editing the set marks the parent `FoodItem` pending and Firestore replaces the parent's component map atomically.

### 4.4 FoodEntry and FoodEntryLine

`FoodEntry` becomes the event header: identity, owner/profile, occurrence time, meal tag, timestamps, deletion state, and sync status. Its current `items: String` field is removed.

Each entry has one or more ordered `FoodEntryLine` children:

| Field | Type | Notes |
| --- | --- | --- |
| `id` | String | Stable line identity |
| `entryId` | String | FK to FoodEntry, cascade delete locally |
| `position` | Int | Explicit UI order |
| `foodItemId` | String? | Set for a catalogue line |
| `quantity` | Double? | Positive; required for a catalogue line |
| `customText` | String? | Nonblank for a custom line |

Exactly one mode is valid:

- Catalogue: `foodItemId != null`, `quantity > 0`, `customText == null`.
- Custom: `foodItemId == null`, `customText` is nonblank. Quantity is omitted because any direct component amounts are totals for that custom line.

`FoodEntryLineComponent(lineId, componentId, amount)` is the optional many-to-many join for a custom line. Its `amount` is the total contributed by that line, not a per-serving value.

### 4.5 Calculation

For a selected component and time range:

```text
catalogue contribution = FoodEntryLine.quantity × FoodItemComponent.amount
custom contribution    = FoodEntryLineComponent.amount
daily total            = sum(all contributions for the local calendar day)
```

No copied catalogue values are stored on a catalogue-backed line. A rename changes historical labels and changing `2.0` to `2.5` changes every historical contribution immediately.

## 5. Android Room implementation

### 5.1 Entities and relationships

Add:

- `TrackedComponentEntity`
- `FoodItemEntity`
- `FoodItemComponentEntity`
- `FoodEntryLineEntity`
- `FoodEntryLineComponentEntity`

Modify:

- Remove `items` from `FoodEntryEntity`.
- Remove `caffeine` and `alcohol` from `NoteEntryEntity`.
- Add new entities to `AppDatabase` and bump the actual database version from 13 to 14.
- Correct the stale `AppDatabase.CURRENT_VERSION` constant so it also reports 14.

Use Room foreign keys for owned rows only:

- Food item component → food item (`CASCADE`).
- Food entry line → food entry (`CASCADE`).
- Food entry line component → line (`CASCADE`).

References to independently synced catalogue rows (`componentId` and `foodItemId`) are indexed but deliberately do not have SQLite foreign-key constraints. This permits the documented temporary unresolved state when a remote entry arrives before, or outlives, a referenced catalogue document. Repository rules and archiving preserve referential integrity during normal local writes.

Use these indexes:

- `tracked_components(profileId, isArchived, sortOrder)`
- `food_items(profileId, isArchived, sortOrder)`
- `food_item_components(foodItemId, componentId)` as primary key and reverse index on `componentId`
- `food_entries(profileId, isDeleted, occurredAt)`
- `food_entry_lines(entryId, position)` and `food_entry_lines(foodItemId)`
- `food_entry_line_components(lineId, componentId)` as primary key and reverse index on `componentId`

### 5.2 Aggregate models

Add Room relation/domain models rather than exposing raw joins to Compose:

- `FoodItemWithComponents(item, componentAmounts)`
- `FoodEntryLineResolved(line, foodItem?, customComponents)`
- `FoodEntryWithLines(entry, orderedLines)`
- `ComponentContribution(occurredAt, componentId, amount)`

The UI must tolerate a temporarily unresolved catalogue reference during sync by displaying `Unavailable food item` and retaining the line controls. Once the catalogue document arrives, Room flows resolve it automatically.

### 5.3 DAOs

Add dedicated DAOs for component and food-item catalogue CRUD, plus transactional replacement methods for owned child rows. Repository writes should use `@Transaction` to:

1. upsert the parent;
2. replace its component association rows or entry lines;
3. mark the parent pending exactly once.

For trends, expose contribution rows with a `UNION ALL` query:

```sql
SELECT e.occurredAt, fic.componentId,
       (l.quantity * fic.amount) AS amount
FROM food_entries e
JOIN food_entry_lines l ON l.entryId = e.id
JOIN food_item_components fic ON fic.foodItemId = l.foodItemId
WHERE e.profileId = :profileId
  AND e.isDeleted = 0
  AND e.occurredAt >= :from AND e.occurredAt < :to

UNION ALL

SELECT e.occurredAt, flic.componentId, flic.amount
FROM food_entries e
JOIN food_entry_lines l ON l.entryId = e.id
JOIN food_entry_line_components flic ON flic.lineId = l.id
WHERE e.profileId = :profileId
  AND e.isDeleted = 0
  AND e.occurredAt >= :from AND e.occurredAt < :to
```

Bucket returned rows by local calendar day in Kotlin. This avoids SQLite timezone/date ambiguities and matches the existing History range behaviour.

### 5.4 Migration 13 → 14

Create `Migration(13, 14)` using the repository's required modern Room API: override `migrate(SQLiteConnection)` and execute statements with `androidx.sqlite.execSQL`.

The migration will:

1. Create the five new tables and indexes.
2. Recreate `food_entries` without `items`.
3. Recreate `note_entries` without `caffeine` and `alcohol`, retaining normal note content and metadata.
4. Not create catalogue items, lines, or component associations from legacy fields.
5. Register the migration in `ALL_MIGRATIONS`.
6. Update exported Room schema 14 and verify it against the migration.

Although legacy food text and note flags are intentionally not converted, an upgrade migration is still required so existing installations open safely. Do not add an unconditional destructive fallback.

### 5.5 Default components

Add a startup invariant similar to other self-healing seed logic. On profile creation and app startup, ensure the following documents/rows exist:

- `Caffeine` / `mg`
- `Alcohol` / `UK units`

Use deterministic IDs derived from profile ID plus a stable built-in key. Make the operation idempotent and sync them like ordinary components. Defaults may be renamed, archived, or have their unit relabelled like ordinary components; changing a unit does not convert existing numeric amounts.

## 6. Firestore representation

Use three top-level collections because components and food items are independently editable shared definitions, while lines are owned by one entry.

### 6.1 `tracked_components/{componentId}`

```json
{
  "userId": "…",
  "profileId": "…",
  "name": "Alcohol",
  "unit": "UK units",
  "sortOrder": 1,
  "isArchived": false,
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

### 6.2 `food_items/{foodItemId}`

```json
{
  "userId": "…",
  "profileId": "…",
  "name": "Bottle of beer",
  "icon": "🍺",
  "amount": "500",
  "unit": "ml",
  "sortOrder": 0,
  "isArchived": false,
  "componentAmounts": {
    "alcoholComponentId": 2.5
  },
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

### 6.3 `food_entries/{entryId}`

```json
{
  "userId": "…",
  "profileId": "…",
  "occurredAt": 1788892200000,
  "mealTag": "DINNER",
  "lines": [
    {
      "id": "line-1",
      "foodItemId": "beerId",
      "quantity": 2
    },
    {
      "id": "line-2",
      "customText": "Piece of birthday cake",
      "componentAmounts": {
        "caffeineComponentId": 8
      }
    }
  ],
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

Embed `componentAmounts` inside a food item and `lines` inside an entry because these values have one owner, are always edited together, and are small. This avoids N+1 reads and independent conflict states. Keep components and food items as separate documents because changing them must affect all references.

Clients load/cold-cache the profile's `componentsById` and `foodItemsById`, then resolve entry lines locally. Firestore cannot aggregate a quantity in one document against a component amount in another, so the trends calculation intentionally runs from these cached definitions and the fetched range of entries.

`occurredAt` and `createdAt` remain epoch-millisecond integers, matching Android `Long` and the
existing web contract; `updatedAt` is the Firestore server timestamp used for incremental sync.

Add single-field index exemptions for unqueried `componentAmounts` maps and `lines` arrays/maps to
avoid unnecessary index fan-out. Existing query indexes on `profileId`/`occurredAt` remain. Security
rules validate ownership/profile consistency, top-level field types, and bounded line/component
counts. Both clients additionally validate every quantity and component amount before writing.

Archive catalogue documents instead of deleting them. Existing entries continue resolving archived definitions; pickers filter them out for new entries.

## 7. Sync design

Sync parents in dependency order:

```text
profiles → tracked components → food items → food entries
```

Apply the same ordering on pull so an entry normally resolves immediately. Temporary missing references remain a supported state.

- `FoodItemComponentEntity` is serialized into/deserialized from `FoodItem.componentAmounts`.
- `FoodEntryLineEntity` and custom line components are serialized into/deserialized from `FoodEntry.lines`.
- Replacing child rows locally and updating the parent sync status is one Room transaction.
- Remote conflict resolution continues to use parent `updatedAt`. Owned embedded children follow the parent winner.
- A catalogue edit queues that catalogue document only. Historical entries are not rewritten.
- Deleting an entry uses the existing soft-delete/tombstone policy; its local child lines can remain until the parent tombstone is fully processed or be recreated from a retained payload according to current sync conventions.
- Remove note caffeine/alcohol fields from Android and web serializers. Readers should tolerate those fields on old Firestore documents and ignore them.
- New food readers should tolerate old documents without `lines`; they are not converted into structured entries.

## 8. Android application architecture

Create a focused `FoodRepository`, analogous to the medicine catalogue boundary, rather than adding all catalogue responsibilities to `EntryRepository`.

Responsibilities:

- observe/create/update/archive components;
- observe/create/update/archive food items;
- validate and transactionally save multi-line food entries;
- expose catalogue items for the searchable picker;
- expose resolved food entries and component contribution flows.

`EntryRepository` continues to combine resolved food entries into the home/timeline stream. Update DI/Koin bindings and sync dependencies for the new DAOs/repository.

Use decimal-safe input strings in forms, parse with the existing locale-aware numeric conventions, and reject non-finite, zero, or negative values. Keep domain totals as `Double` to match Room/Firestore numeric interoperability, with display rounding based on the component unit.

## 9. Android screens and interaction design

### 9.1 Catalogue entry point

Add **Food library** to Settings; do not add a fifth bottom-navigation destination. The Food library screen has two tabs:

- **Items**: active food/drink catalogue, search, edit, archive, add.
- **Components**: active tracked components, unit, edit/archive, add.

Archived definitions are available through an overflow action and can be restored.

### 9.2 Add/edit food component

Fields:

- Name (required)
- Unit (required; examples offered but free text allowed)

Show helper text explaining that the unit is used consistently in food items and trends. On edit, explain that changing the unit relabels existing amounts without converting their numeric values. Prevent duplicate active names within the profile.

### 9.3 Add/edit food item

Fields:

- Icon (required; one typed character such as an emoji)
- Name
- Serving amount
- Serving unit
- Repeating **Tracked components** rows: component selector + amount per one item

Allow adding/removing component values. A separate component editor remains available from the
Food library; an inline shortcut can be added later without changing the model. Show a live
sentence such as: “One 500 ml Bottle of beer contains 2.5 UK units Alcohol.”

### 9.4 Add/edit food entry

Replace the single free-text box with an ordered line builder:

1. Entry time.
2. Zero or more line cards, each showing item name, serving, quantity control, component summary, reorder, and remove.
3. **Add item** opens a picker with Saved and Custom tabs.
4. Saved supports search and opens quantity at 1.
5. Custom accepts free text plus optional component totals.
6. Meal tag.
7. Save is enabled only when at least one valid line exists.

Editing restores line IDs and order. Adding the same food item twice is allowed; the UI may offer “increase existing quantity” but must not silently merge lines because separate lines can communicate separate consumption moments within one entry.

Timeline cards resolve current catalogue names and icons, with one row per item, and display, for example:

```text
Dinner · 19:30
🍺 2 × Bottle of beer · Alcohol 5 UK units
🍛 1 × Vegetable curry
```

### 9.5 Notes

Remove the caffeine and alcohol switches/chips from Add/Edit Note and note timeline rendering. Preserve ordinary note tags.

## 10. Trends and graphs

Keep the existing History range controls (`7d`, `30d`, `90d`, `6mo`, `1y`, `All`). Add a **Components** section generated from active or historically used components.

For each component expose:

- total in selected range;
- average per active day and/or calendar day (label explicitly);
- peak day;
- daily/weekly bars in the component's own unit;
- a future tap-through detail can show entry contributions for the selected day.

Do not place unlike units on one shared numeric axis. The overview uses small multiples/cards. A component detail can optionally overlay a normalized symptom series or show a dual-axis comparison, but the UI must clearly label that it is correlation exploration, not causation.

Introduce a generic `ComponentSeries` model:

```kotlin
data class ComponentSeries(
    val componentId: String,
    val name: String,
    val unit: String,
    val buckets: List<ComponentBucket>,
    val total: Double,
    val averagePerDay: Double,
    val peak: ComponentBucket?
)
```

Update food “suspects”/term analysis to tokenize resolved catalogue names and custom text rather than the removed `FoodEntry.items` string.

## 11. Web parity

Android and web ship against the same Firestore schema. Update the web application in the same feature branch/release:

- TypeScript types and Firestore codecs for tracked components, food items, and embedded entry lines.
- Profile-scoped catalogue hooks/context with loading/error states.
- Food library management UI.
- Multi-item food entry sheet with Saved/Custom picker.
- Resolved timeline rendering.
- Removal of note caffeine/alcohol controls and fields.
- Generic component trend aggregation matching Android range boundaries and timezone rules.
- Graceful reads of legacy note fields and old food documents without treating them as new structured data.

Run the web production build before completion to catch type/schema drift.

## 12. Security rules and indexes

Update Firestore rules to require:

- authenticated ownership (`userId` matches caller);
- a profile the caller owns;
- bounded strings and arrays/maps;
- bounded embedded component maps and line lists;
- structured food entries with the current schema version and epoch-millisecond occurrence time;
- immutable document ownership fields after creation.

Dynamic component IDs make strict iteration over every embedded map/list value impractical in
Firestore rules. Positive finite amounts and the exact catalogue/custom-line union are enforced by
the Android repository and web codecs. If hostile-client validation becomes a requirement, model
owned lines/amounts as constrained child documents or add a trusted validation service instead of
claiming the embedded shape is fully server-validated.

Add rules emulator tests when rules-test infrastructure is introduced to the repository, covering
cross-user writes, malformed top-level payloads, oversized collections, and valid
catalogue/custom writes.

Update `firestore.indexes.json` with exemptions for `food_items.componentAmounts`, `food_entries.lines`, and nested component maps because no server query targets those fields.

## 13. Testing plan

### Database and calculations

- Room migration 13→14 produces the exported schema exactly.
- Migration test uses `SQLiteConnection`, preventing accidental use of the obsolete SupportSQLite API.
- Fresh-install schema contains all tables, indexes, and foreign keys.
- Default components self-heal on fresh install, upgrade, new profile, and repeated startup.
- One catalogue line: component amount × quantity.
- Multiple items and multiple components aggregate correctly.
- Custom line totals combine with catalogue contributions.
- Archived definitions continue to resolve history.
- Editing a food-item name updates historical rendering.
- Editing a component amount updates historical trends without rewriting entries.
- Deleted food entries do not contribute.
- Range boundaries and local-day bucketing handle DST and timezone changes.

### Repository and sync

- Parent/child replacement is atomic and marks only the parent pending.
- Firestore mapper round-trips maps, embedded lines, decimal quantities, and line order.
- Push/pull dependency ordering resolves components before items before entries.
- Missing catalogue references render safely and later self-resolve.
- Multi-device edits follow the documented parent-level last-write policy.
- Legacy documents with old fields do not crash readers.

### UI

- Component create/edit/archive/restore and locked-unit states.
- Food item validation and repeated component rows.
- Returning from Food library preserves an in-progress food entry.
- Add, edit, remove, reorder, and duplicate food-entry lines.
- Saved and Custom picker flows.
- Process recreation restores draft state where current entry forms do.
- Accessibility labels, keyboard actions, focus order, large font, and dark theme.
- Trend cards for empty, single-point, dense, archived-component, and mixed-unit data.

### Required verification

Run the relevant focused tests during implementation, then at minimum:

```bash
./gradlew :app:compileDebugKotlinAndroid
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
cd webapp && npm run build
```

Also run Firestore rule/emulator tests if configured in the repository.

## 14. Implementation sequence

### Phase 1 — Shared contract and persistence

1. Add domain IDs, entity/DTO models, and validation helpers.
2. Add Room entities, DAOs, relations, indexes, migration 13→14, and exported schema.
3. Add startup default-component invariant.
4. Add DAO calculation and migration tests.

Exit criterion: local catalogue and multi-line entries persist correctly; totals are verified without UI or sync.

### Phase 2 — Repository and sync

1. Add `FoodRepository` and transactional saves.
2. Update timeline aggregation to consume resolved food entries.
3. Add Firestore codecs and dependency-ordered sync.
4. Update security rules/index exemptions and tests.

Exit criterion: two devices/web can exchange catalogue definitions and structured food entries, including edits that alter historical rendering/totals.

### Phase 3 — Android catalogue and logging UI

1. Add Food library navigation and Items/Components management.
2. Implement component editor.
3. Implement food-item editor with repeatable component rows.
4. Replace Add/Edit Food with the multi-line builder and picker.
5. Update timeline/detail surfaces and remove note flags.

Exit criterion: all four logging/catalogue flows are usable with accessibility and state-restoration tests.

### Phase 4 — Trends

1. Add contribution query and Kotlin bucketing.
2. Add generic component summary cards and charts.
3. Update suspects analysis to use resolved names; retain drill-down/symptom comparison as optional follow-up.

Exit criterion: totals match entry detail for every supported range and unit.

### Phase 5 — Web parity and hardening

1. Implement web catalogue, logging, timeline, and trends against the same schema.
2. Complete the checks supported by the repository; record environment/infrastructure limitations explicitly.
3. Run Android and web release verification.

Exit criterion: both clients read/write the same data without compatibility gaps.

## 15. Performance expectations

Room performs well with the proposed joins because queries start from a selective profile/time-range index and then use indexed foreign keys. The joins are appropriate normalization, not a performance smell. Measure with a synthetic profile containing at least 10,000 food entries and 100 catalogue items; target responsive range changes without main-thread work.

Firestore uses a small fixed set of queries rather than per-entry joins:

1. active/all-needed tracked components for the profile;
2. active/all-needed food items for the profile;
3. food entries for the selected time range.

Resolution and aggregation occur in memory. Cache catalogues and use snapshot listeners already consistent with the app's sync architecture. If very large histories later make client aggregation slow or expensive, add derived daily-total documents as a cache only; do not make them the source of truth because catalogue edits would require invalidation/rebuild.

## 16. Likely code areas

The exact filenames should follow current package conventions, but the implementation will touch:

- Room entities, DAOs, `AppDatabase`, migrations, and exported schemas.
- Firestore DTO/mappers, `SyncWorker`, security rules, and indexes.
- `EntryRepository` plus a new `FoodRepository` and DI bindings.
- Android food/component screens, navigation, view models, timeline models/cards, and History trends.
- Note editor/model serialization to remove caffeine/alcohol.
- Web TypeScript models, Firestore data layer, food/note forms, timeline, and trends.
- Unit, migration, rules, repository, ViewModel, and web tests.

## 17. Acceptance criteria

- A user can create `Alcohol / UK units` or any other component without schema changes.
- A user can create `Bottle of beer / 500 ml` containing `2.5 UK units` Alcohol.
- A food entry can contain two bottles plus other saved or custom lines.
- That entry contributes exactly `5 UK units` Alcohol.
- Changing the beer to `2.0 UK units` updates the same historical entry to `4 UK units` without rewriting it.
- Renaming the beer updates historical display.
- Archived items/components remain readable historically and are absent from default pickers.
- Component trends work for all existing date ranges and never mix unlike units on one axis.
- Notes no longer contain caffeine/alcohol controls or persisted flags.
- Android Room and Firestore/web representations remain behaviorally equivalent.
- Upgrade, fresh install, sync, rules, Android build/tests, and web build all pass.
