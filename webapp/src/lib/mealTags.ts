import { MealTag } from '../types'

export const MEAL_TAGS: { id: MealTag; label: string; icon: string }[] = [
  { id: 'BREAKFAST', label: 'Breakfast', icon: 'sunrise' },
  { id: 'LUNCH', label: 'Lunch', icon: 'sun' },
  { id: 'DINNER', label: 'Dinner', icon: 'moon' },
  { id: 'SNACK', label: 'Snack', icon: 'cookie' },
  { id: 'DRINK', label: 'Drink', icon: 'cup-soda' },
]

export function mealTagLabel(tag: MealTag | null): string | null {
  return MEAL_TAGS.find((m) => m.id === tag)?.label ?? null
}
