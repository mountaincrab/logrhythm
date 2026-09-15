/**
 * Home timeline layout preferences.
 *
 * Mirror of the Android `UserPreferencesRepository` keys for the same settings. Like the
 * entry-type filter these live in `localStorage`, not Firestore: they say how this device
 * lays the feed out, not what is in it, so they are deliberately not synced or part of the
 * profile document.
 */

const GROUP_BY_ENTRY_TYPE_KEY = 'logrhythm:groupHomeByEntryType'

export function groupHomeByEntryType(): boolean {
  try {
    return localStorage.getItem(GROUP_BY_ENTRY_TYPE_KEY) === 'true'
  } catch {
    // Storage can be unavailable in restricted browser contexts; fall back to ungrouped.
    return false
  }
}

export function setGroupHomeByEntryType(value: boolean) {
  try {
    localStorage.setItem(GROUP_BY_ENTRY_TYPE_KEY, String(value))
  } catch {
    // Same as above — the setting simply doesn't persist.
  }
}
