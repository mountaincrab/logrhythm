import { useState } from 'react'
import Sheet, { Field } from '../Sheet'
import WhenField from '../WhenField'
import { MEAL_TAGS } from '../../lib/mealTags'
import { MealTag } from '../../types'
import { FoodInput } from '../../hooks/useEntries'

const RECENT = ['Banana', 'White rice', 'Plain toast', 'Buttered toast', 'Boiled chicken', 'Plain yoghurt', 'Apple', 'Coffee', 'Eggs']

interface Props {
  onClose: () => void
  onSave: (input: FoodInput) => Promise<void>
  onDelete?: () => void
  initial?: FoodInput
}

export default function AddFoodSheet({ onClose, onSave, onDelete, initial }: Props) {
  const [occurredAt, setOccurredAt] = useState(initial?.occurredAt ?? Date.now())
  const [items, setItems] = useState(initial?.items ?? '')
  const [mealTag, setMealTag] = useState<MealTag | null>(initial?.mealTag ?? null)
  const [saving, setSaving] = useState(false)

  const addRecent = (t: string) =>
    setItems((prev) => (prev.trim() ? `${prev}, ${t.toLowerCase()}` : t))

  const save = async () => {
    setSaving(true)
    try {
      await onSave({ occurredAt, items: items.trim(), mealTag })
      onClose()
    } finally {
      setSaving(false)
    }
  }

  return (
    <Sheet
      title={initial ? 'Edit food' : 'Log food'}
      onClose={onClose}
      onSave={save}
      onDelete={onDelete}
      saveLabel={initial ? 'Save' : 'Save food'}
      canSave={items.trim().length > 0}
      saving={saving}
    >
      <WhenField value={occurredAt} onChange={setOccurredAt} />

      <Field label="What you ate">
        <textarea
          value={items}
          onChange={(e) => setItems(e.target.value)}
          placeholder="Free text — be specific where it matters (e.g. 'spicy chicken', 'whole milk')."
          className="w-full bg-surface-raised border border-DEFAULT rounded-xl px-3.5 py-3 text-sm text-fg resize-none min-h-[120px] outline-none focus:border-accent placeholder:text-fg-faint transition-colors"
        />
      </Field>

      <Field label="Recent" hint="tap to add">
        <div className="flex flex-wrap gap-1.5">
          {RECENT.map((t) => (
            <button
              key={t}
              onClick={() => addRecent(t)}
              className="px-3 py-1.5 rounded-full bg-surface-raised border border-DEFAULT text-xs text-fg-muted hover:text-fg transition-colors"
            >
              {t}
            </button>
          ))}
        </div>
      </Field>

      <Field label="Tag" hint="optional">
        <div className="flex flex-wrap gap-1.5">
          {MEAL_TAGS.map((m) => {
            const on = mealTag === m.id
            return (
              <button
                key={m.id}
                onClick={() => setMealTag(on ? null : m.id)}
                className={
                  'px-3 py-2 rounded-xl text-xs font-semibold border transition-colors ' +
                  (on ? 'bg-accent-soft border-accent text-accent-text' : 'bg-surface-raised border-DEFAULT text-fg-muted')
                }
              >
                {m.label}
              </button>
            )
          })}
        </div>
      </Field>
    </Sheet>
  )
}
