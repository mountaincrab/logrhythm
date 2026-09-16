import { FoodEntry, FoodItem, TrackedComponent } from '../types'

export const DEFAULT_FOOD_ITEM_ICON = '🍴'

export const UNAVAILABLE_FOOD_ITEM = 'Unavailable food item'

export function firstFoodIcon(value: string): string {
  return Array.from(value.trimStart())[0] ?? ''
}

export function formatFoodNumber(value: number): string {
  return Number.isInteger(value) ? String(value) : String(Math.round(value * 100) / 100)
}

export function foodEntryLabel(entry: FoodEntry, itemsById: Map<string, FoodItem>): string {
  return [...entry.lines].sort((a, b) => a.position - b.position).map((line) => {
    if (!line.foodItemId) return line.customText ?? ''
    const item = itemsById.get(line.foodItemId)
    if (!item) return 'Unavailable food item'
    const quantity = line.quantity ?? 1
    return quantity === 1 ? item.name : `${formatFoodNumber(quantity)} × ${item.name}`
  }).filter(Boolean).join(', ')
}

export function foodEntryComponentTotals(entry: FoodEntry, itemsById: Map<string, FoodItem>): Record<string, number> {
  const totals: Record<string, number> = {}
  for (const line of entry.lines) {
    const amounts = line.foodItemId ? itemsById.get(line.foodItemId)?.componentAmounts ?? {} : line.componentAmounts
    const multiplier = line.foodItemId ? (line.quantity ?? 0) : 1
    for (const [componentId, amount] of Object.entries(amounts)) {
      totals[componentId] = (totals[componentId] ?? 0) + amount * multiplier
    }
  }
  return totals
}

/** One logging of a merged row's food: which entry said so, and when. */
export interface FoodOccurrence {
  entryId: string
  occurredAt: number
}

/** A merged row's running total for one tracked component, in that component's own unit. */
export interface MergedFoodComponentTotal {
  componentId: string
  name: string
  unit: string
  amount: number
}

/**
 * A day's loggings of one food, folded into a single row: how much of it in total, what
 * that adds up to per tracked component, and the time of each logging.
 *
 * Four cups of tea are four entries — deleting or editing one still happens on that entry,
 * which is why `occurrences` keeps the entry ids rather than just the times.
 */
export interface MergedFoodRow {
  key: string
  icon: string
  name: string
  totalQuantity: number
  /**
   * Catalogue foods always name their multiplier (one serving is "1 ×"), a custom line
   * only once it has repeated — it has no serving size to be a multiple of.
   */
  showQuantity: boolean
  componentTotals: MergedFoodComponentTotal[]
  occurrences: FoodOccurrence[]
}

/**
 * Fold a day's food entries into one row per food, for the grouped home timeline.
 *
 * Catalogue lines merge on their `foodItemId` — the item is a live reference, so two
 * loggings of it are the same food however it has been renamed since. Custom lines have
 * only their text to go on, so they merge case-insensitively on that. Component amounts
 * follow the same rule as a single row: a catalogue line's are per serving and scale with
 * the quantity, a custom line's are already the total it typed in.
 *
 * Rows come back newest-logged first, matching the feed around them, while the times
 * inside a row run forwards — the row is that food's day, read left to right.
 * Mirror of `mergeFoodEntries` in data/model/MergedFood.kt.
 */
export function mergeFoodEntries(
  entries: FoodEntry[],
  itemsById: Map<string, FoodItem>,
  componentsById: Map<string, TrackedComponent>,
): MergedFoodRow[] {
  interface Accumulator {
    key: string
    icon: string
    name: string
    isCatalogueItem: boolean
    totalQuantity: number
    componentTotals: Map<string, number>
    occurrences: Map<string, number>
  }
  const accumulators = new Map<string, Accumulator>()

  for (const entry of entries) {
    for (const line of [...entry.lines].sort((a, b) => a.position - b.position)) {
      const itemId = line.foodItemId
      const customText = (line.customText ?? '').trim()
      const key = itemId ? `item:${itemId}` : `custom:${customText.toLowerCase()}`
      const foodItem = itemId ? itemsById.get(itemId) : undefined
      const quantity = itemId ? line.quantity ?? 1 : 1
      let accumulator = accumulators.get(key)
      if (!accumulator) {
        accumulator = {
          key,
          icon: foodItem?.icon ?? DEFAULT_FOOD_ITEM_ICON,
          name: foodItem?.name ?? (customText || UNAVAILABLE_FOOD_ITEM),
          isCatalogueItem: itemId !== null,
          totalQuantity: 0,
          componentTotals: new Map(),
          occurrences: new Map(),
        }
        accumulators.set(key, accumulator)
      }
      accumulator.totalQuantity += quantity
      const amounts = itemId ? foodItem?.componentAmounts ?? {} : line.componentAmounts
      for (const [componentId, amount] of Object.entries(amounts)) {
        const contribution = itemId ? amount * quantity : amount
        accumulator.componentTotals.set(componentId, (accumulator.componentTotals.get(componentId) ?? 0) + contribution)
      }
      // One occurrence per entry: an entry listing the same food twice is still one
      // logging at one time, its quantities simply add up.
      if (!accumulator.occurrences.has(entry.id)) accumulator.occurrences.set(entry.id, entry.occurredAt)
    }
  }

  const sortOrder = (componentId: string) => componentsById.get(componentId)?.sortOrder ?? Number.MAX_SAFE_INTEGER
  return [...accumulators.values()]
    .map((accumulator): MergedFoodRow => ({
      key: accumulator.key,
      icon: accumulator.icon,
      name: accumulator.name,
      totalQuantity: accumulator.totalQuantity,
      showQuantity: accumulator.isCatalogueItem || accumulator.totalQuantity > 1,
      componentTotals: [...accumulator.componentTotals.entries()]
        .filter(([, amount]) => amount > 0)
        .flatMap(([componentId, amount]) => {
          const component = componentsById.get(componentId)
          return component ? [{ componentId, name: component.name, unit: component.unit, amount }] : []
        })
        .sort((a, b) => sortOrder(a.componentId) - sortOrder(b.componentId) || a.name.localeCompare(b.name)),
      occurrences: [...accumulator.occurrences.entries()]
        .map(([entryId, occurredAt]) => ({ entryId, occurredAt }))
        .sort((a, b) => a.occurredAt - b.occurredAt),
    }))
    .sort((a, b) => {
      const latest = (row: MergedFoodRow) => Math.max(...row.occurrences.map((o) => o.occurredAt))
      return latest(b) - latest(a) || a.name.localeCompare(b.name)
    })
}
