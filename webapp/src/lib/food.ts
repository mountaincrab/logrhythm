import { FoodEntry, FoodItem } from '../types'

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
