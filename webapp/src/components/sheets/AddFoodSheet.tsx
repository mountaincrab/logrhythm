import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronDown, ChevronUp, Plus, Trash2 } from 'lucide-react'
import Sheet, { Field } from '../Sheet'
import WhenField from '../WhenField'
import { MEAL_TAGS } from '../../lib/mealTags'
import { FoodEntryLine, MealTag } from '../../types'
import { FoodInput } from '../../hooks/useEntries'
import { useFoodCatalogContext } from '../../contexts/FoodCatalogContext'

interface Props {
  onClose: () => void
  onSave: (input: FoodInput) => Promise<void>
  onDelete?: () => void
  initial?: FoodInput
}

const numberText = (n: number) => Number.isInteger(n) ? String(n) : String(n)

export default function AddFoodSheet({ onClose, onSave, onDelete, initial }: Props) {
  const navigate = useNavigate()
  const { foodItems, foodItemsById, components, componentsById } = useFoodCatalogContext()
  const [occurredAt, setOccurredAt] = useState(initial?.occurredAt ?? Date.now())
  const [lines, setLines] = useState<FoodEntryLine[]>(initial?.lines ?? [])
  const [mealTag, setMealTag] = useState<MealTag | null>(initial?.mealTag ?? null)
  const [savedItemId, setSavedItemId] = useState(foodItems[0]?.id ?? '')
  const [savedSearch, setSavedSearch] = useState('')
  const [customText, setCustomText] = useState('')
  const [customAmounts, setCustomAmounts] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)

  const filteredFoodItems = useMemo(() => {
    const query = savedSearch.trim().toLocaleLowerCase()
    return query ? foodItems.filter((item) => item.name.toLocaleLowerCase().includes(query)) : foodItems
  }, [foodItems, savedSearch])
  const effectiveSavedItemId = filteredFoodItems.some((x) => x.id === savedItemId)
    ? savedItemId
    : (filteredFoodItems[0]?.id ?? '')
  const valid = lines.length > 0 && lines.every((line) => line.foodItemId
    ? (line.quantity ?? 0) > 0
    : Boolean(line.customText?.trim()))

  const addSaved = () => {
    if (!effectiveSavedItemId) return
    setLines((current) => current.concat({
      id: crypto.randomUUID(), position: current.length, foodItemId: effectiveSavedItemId,
      quantity: 1, customText: null, componentAmounts: {},
    }))
  }
  const addCustom = () => {
    const text = customText.trim()
    if (!text) return
    setLines((current) => current.concat({
      id: crypto.randomUUID(), position: current.length, foodItemId: null, quantity: null,
      customText: text,
      componentAmounts: Object.fromEntries(Object.entries(customAmounts)
        .map(([id, value]) => [id, Number(value)] as const)
        .filter(([, value]) => Number.isFinite(value) && value > 0)),
    }))
    setCustomText('')
    setCustomAmounts({})
  }
  const remove = (id: string) => setLines((current) => current.filter((x) => x.id !== id))
  const move = (index: number, delta: number) => setLines((current) => {
    const target = index + delta
    if (target < 0 || target >= current.length) return current
    const next = [...current]
    const [line] = next.splice(index, 1)
    next.splice(target, 0, line)
    return next
  })

  const save = async () => {
    if (!valid) return
    setSaving(true)
    try {
      await onSave({ occurredAt, lines: lines.map((line, position) => ({ ...line, position })), mealTag })
      onClose()
    } finally { setSaving(false) }
  }

  const componentSummary = (line: FoodEntryLine) => {
    const item = line.foodItemId ? foodItemsById.get(line.foodItemId) : undefined
    const amounts = item
      ? Object.fromEntries(Object.entries(item.componentAmounts).map(([id, n]) => [id, n * (line.quantity ?? 1)]))
      : line.componentAmounts
    return Object.entries(amounts).map(([id, n]) => {
      const component = componentsById.get(id)
      return component ? `${numberText(n)} ${component.unit} ${component.name}` : null
    }).filter(Boolean).join(' · ')
  }

  return (
    <Sheet title={initial ? 'Edit food' : 'Log food'} onClose={onClose} onSave={save}
      onDelete={onDelete} saveLabel={initial ? 'Save' : 'Save food'} canSave={valid} saving={saving}>
      <WhenField value={occurredAt} onChange={setOccurredAt} />

      <Field label="Items" hint="add more than one">
        <div className="space-y-2">
          {lines.length === 0 && (
            <div className="rounded-2xl border border-dashed border-strong px-4 py-5 text-center text-sm text-fg-muted">
              Add a saved item or a custom line below.
            </div>
          )}
          {lines.map((line, index) => {
            const item = line.foodItemId ? foodItemsById.get(line.foodItemId) : undefined
            const summary = componentSummary(line)
            return (
              <div key={line.id} className="rounded-2xl bg-surface-raised border border-DEFAULT p-3">
                <div className="flex items-center gap-2">
                  <div className="min-w-0 flex-1">
                    <div className="text-sm font-bold truncate"><span className="mr-2">{item?.icon ?? '🍴'}</span>{item?.name ?? line.customText ?? 'Unavailable food item'}</div>
                    {item && <div className="text-[11px] text-fg-muted">One item · {item.amount} {item.unit}</div>}
                  </div>
                  {item && (
                    <input type="number" min="0.01" step="any" value={line.quantity ?? ''}
                      aria-label={`Quantity for ${item.name}`}
                      onChange={(e) => setLines((all) => all.map((x) => x.id === line.id ? { ...x, quantity: Number(e.target.value) } : x))}
                      className="w-16 bg-surface border border-DEFAULT rounded-lg px-2 py-1.5 text-sm text-center outline-none focus:border-accent" />
                  )}
                  <button onClick={() => move(index, -1)} disabled={index === 0} aria-label="Move up" className="text-fg-muted disabled:opacity-20"><ChevronUp size={16} /></button>
                  <button onClick={() => move(index, 1)} disabled={index === lines.length - 1} aria-label="Move down" className="text-fg-muted disabled:opacity-20"><ChevronDown size={16} /></button>
                  <button onClick={() => remove(line.id)} aria-label="Remove" className="text-danger-text"><Trash2 size={16} /></button>
                </div>
                {summary && <div className="mt-2 text-[11px] font-semibold text-accent-text">{summary}</div>}
              </div>
            )
          })}
        </div>
      </Field>

      <Field label="Add saved item">
        <input value={savedSearch} onChange={(e) => setSavedSearch(e.target.value)}
          placeholder="Search saved items"
          className="w-full mb-2 bg-surface-raised border border-DEFAULT rounded-xl px-3.5 py-2.5 text-sm outline-none focus:border-accent placeholder:text-fg-faint" />
        <div className="flex gap-2">
          <select value={effectiveSavedItemId} onChange={(e) => setSavedItemId(e.target.value)}
            className="min-w-0 flex-1 bg-surface-raised border border-DEFAULT rounded-xl px-3 py-2.5 text-sm outline-none focus:border-accent">
            {filteredFoodItems.length === 0 && <option value="">{foodItems.length === 0 ? 'No saved food items yet' : 'No matching items'}</option>}
            {filteredFoodItems.map((item) => <option key={item.id} value={item.id}>{item.icon} {item.name} · {item.amount} {item.unit}</option>)}
          </select>
          <button onClick={addSaved} disabled={!effectiveSavedItemId}
            className="px-3 rounded-xl bg-accent-soft border border-accent text-accent-text disabled:opacity-40"><Plus size={18} /></button>
        </div>
        <button onClick={() => { onClose(); navigate('/food-library') }}
          className="mt-2 w-full py-2 text-xs font-bold text-accent-text">
          {foodItems.length === 0 ? 'Create a food item' : 'Manage food library'}
        </button>
      </Field>

      <Field label="Or add custom" hint="component totals optional">
        <input value={customText} onChange={(e) => setCustomText(e.target.value)} placeholder="e.g. homemade smoothie"
          className="w-full bg-surface-raised border border-DEFAULT rounded-xl px-3.5 py-2.5 text-sm outline-none focus:border-accent placeholder:text-fg-faint" />
        {components.length > 0 && <div className="grid grid-cols-2 gap-2 mt-2">
          {components.map((component) => (
            <label key={component.id} className="rounded-xl bg-surface-raised border border-DEFAULT px-3 py-2">
              <span className="block text-[10px] font-bold text-fg-muted mb-1">{component.name} ({component.unit})</span>
              <input type="number" min="0" step="any" value={customAmounts[component.id] ?? ''}
                onChange={(e) => setCustomAmounts((current) => ({ ...current, [component.id]: e.target.value }))}
                placeholder="0" className="w-full bg-transparent text-sm outline-none" />
            </label>
          ))}
        </div>}
        <button onClick={addCustom} disabled={!customText.trim()}
          className="mt-2 w-full py-2.5 rounded-xl border border-DEFAULT bg-surface-high text-sm font-bold disabled:opacity-40">
          Add custom line
        </button>
      </Field>

      <Field label="Tag" hint="optional">
        <div className="flex flex-wrap gap-1.5">
          {MEAL_TAGS.map((m) => {
            const on = mealTag === m.id
            return <button key={m.id} onClick={() => setMealTag(on ? null : m.id)}
              className={'px-3 py-2 rounded-xl text-xs font-semibold border transition-colors ' +
                (on ? 'bg-accent-soft border-accent text-accent-text' : 'bg-surface-raised border-DEFAULT text-fg-muted')}>{m.label}</button>
          })}
        </div>
      </Field>
    </Sheet>
  )
}
