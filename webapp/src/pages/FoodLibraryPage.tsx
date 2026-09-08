import { useMemo, useState } from 'react'
import { Archive, Pencil, Plus, RotateCcw, X } from 'lucide-react'
import AppShell from '../components/AppShell'
import { useFoodCatalogContext } from '../contexts/FoodCatalogContext'
import { useEntriesContext } from '../contexts/EntriesContext'
import { FoodItem, TrackedComponent } from '../types'

type Tab = 'items' | 'components'

export default function FoodLibraryPage() {
  const catalog = useFoodCatalogContext()
  const { foods } = useEntriesContext()
  const [tab, setTab] = useState<Tab>('items')
  const [itemSearch, setItemSearch] = useState('')
  const [editingItem, setEditingItem] = useState<FoodItem | null | undefined>()
  const [editingComponent, setEditingComponent] = useState<TrackedComponent | null | undefined>()

  const usedComponentIds = useMemo(() => {
    const ids = new Set<string>()
    catalog.archivedFoodItems.concat(catalog.foodItems).forEach((item) => Object.keys(item.componentAmounts).forEach((id) => ids.add(id)))
    foods.forEach((entry) => entry.lines.forEach((line) => Object.keys(line.componentAmounts).forEach((id) => ids.add(id))))
    return ids
  }, [catalog.archivedFoodItems, catalog.foodItems, foods])
  const visibleFoodItems = useMemo(() => {
    const query = itemSearch.trim().toLocaleLowerCase()
    return query ? catalog.foodItems.filter((item) => item.name.toLocaleLowerCase().includes(query)) : catalog.foodItems
  }, [catalog.foodItems, itemSearch])

  return <AppShell title="Food library" subtitle="Reusable items and tracked components" showProfileSwitcher>
    <div className="max-w-2xl">
      <div className="flex bg-surface border border-DEFAULT rounded-xl p-[3px] mb-5 max-w-xs">
        {(['items', 'components'] as const).map((value) => <button key={value} onClick={() => setTab(value)}
          className={'flex-1 py-2 rounded-[9px] text-[13px] font-bold ' + (tab === value ? 'bg-accent text-accent-fg' : 'text-fg-muted')}>
          {value === 'items' ? 'Food items' : 'Components'}
        </button>)}
      </div>

      {tab === 'items' ? <>
        <button onClick={() => setEditingItem(null)} className="w-full mb-3 py-3 rounded-2xl bg-accent text-accent-fg font-bold inline-flex items-center justify-center gap-2">
          <Plus size={17} /> Add food item
        </button>
        <input value={itemSearch} onChange={(event) => setItemSearch(event.target.value)}
          placeholder="Search food items"
          className="w-full mb-3 bg-surface-raised border border-DEFAULT rounded-xl px-3.5 py-2.5 text-sm outline-none focus:border-accent placeholder:text-fg-faint" />
        <div className="space-y-2">
          {visibleFoodItems.map((item) => <CatalogRow key={item.id} title={item.name}
            subtitle={`${item.amount} ${item.unit}${componentLabel(item, catalog.componentsById)}`}
            onEdit={() => setEditingItem(item)} onArchive={() => catalog.setFoodItemArchived(item.id, true)} />)}
          {catalog.foodItems.length === 0
            ? <Empty text="No saved food items yet." />
            : visibleFoodItems.length === 0 && <Empty text="No matching food items." />}
        </div>
        {catalog.archivedFoodItems.length > 0 && <Archived title="Archived food items">
          {catalog.archivedFoodItems.map((item) => <ArchivedRow key={item.id} title={item.name} onRestore={() => catalog.setFoodItemArchived(item.id, false)} />)}
        </Archived>}
      </> : <>
        <button onClick={() => setEditingComponent(null)} className="w-full mb-3 py-3 rounded-2xl bg-accent text-accent-fg font-bold inline-flex items-center justify-center gap-2">
          <Plus size={17} /> Add component
        </button>
        <div className="space-y-2">
          {catalog.components.map((component) => <CatalogRow key={component.id} title={component.name} subtitle={component.unit}
            onEdit={() => setEditingComponent(component)} onArchive={() => catalog.setComponentArchived(component.id, true)} />)}
        </div>
        {catalog.archivedComponents.length > 0 && <Archived title="Archived components">
          {catalog.archivedComponents.map((component) => <ArchivedRow key={component.id} title={`${component.name} · ${component.unit}`} onRestore={() => catalog.setComponentArchived(component.id, false)} />)}
        </Archived>}
      </>}
    </div>

    {editingItem !== undefined && <FoodItemDialog item={editingItem} onClose={() => setEditingItem(undefined)} />}
    {editingComponent !== undefined && <ComponentDialog component={editingComponent}
      unitLocked={Boolean(editingComponent && usedComponentIds.has(editingComponent.id))}
      onClose={() => setEditingComponent(undefined)} />}
  </AppShell>
}

function componentLabel(item: FoodItem, byId: Map<string, TrackedComponent>) {
  const values = Object.entries(item.componentAmounts).map(([id, amount]) => {
    const component = byId.get(id)
    return component ? `${amount} ${component.unit} ${component.name}` : null
  }).filter(Boolean)
  return values.length ? ` · ${values.join(' · ')}` : ''
}

function CatalogRow({ title, subtitle, onEdit, onArchive }: { title: string; subtitle: string; onEdit: () => void; onArchive: () => void }) {
  return <div className="flex items-center gap-2 bg-surface-raised border border-DEFAULT rounded-2xl px-4 py-3">
    <div className="flex-1 min-w-0"><div className="text-sm font-bold truncate">{title}</div><div className="text-xs text-fg-muted truncate">{subtitle}</div></div>
    <button onClick={onEdit} aria-label="Edit" className="w-9 h-9 rounded-xl bg-surface-high text-fg-muted inline-flex items-center justify-center"><Pencil size={16} /></button>
    <button onClick={onArchive} aria-label="Archive" className="w-9 h-9 rounded-xl bg-surface-high text-fg-muted inline-flex items-center justify-center"><Archive size={16} /></button>
  </div>
}
function ArchivedRow({ title, onRestore }: { title: string; onRestore: () => void }) {
  return <div className="flex items-center gap-2 py-2 border-t border-subtle first:border-t-0"><span className="flex-1 text-sm text-fg-muted">{title}</span><button onClick={onRestore} className="text-xs font-bold text-accent-text inline-flex items-center gap-1"><RotateCcw size={13} /> Restore</button></div>
}
function Archived({ title, children }: { title: string; children: React.ReactNode }) {
  return <div className="mt-6"><div className="ds-eyebrow mb-2">{title}</div><div className="bg-surface-raised border border-DEFAULT rounded-2xl px-4">{children}</div></div>
}
function Empty({ text }: { text: string }) { return <div className="py-10 text-center text-sm text-fg-muted">{text}</div> }

function DialogFrame({ title, onClose, onSave, canSave, children }: { title: string; onClose: () => void; onSave: () => void; canSave: boolean; children: React.ReactNode }) {
  return <div className="fixed inset-0 z-50 bg-black/60 flex items-end sm:items-center justify-center">
    <div className="w-full sm:max-w-md bg-bg rounded-t-3xl sm:rounded-3xl p-5 max-h-[92dvh] overflow-auto">
      <div className="flex items-center mb-5"><h2 className="text-xl font-extrabold flex-1">{title}</h2><button onClick={onClose}><X size={20} /></button></div>
      {children}
      <div className="flex gap-2 mt-5"><button onClick={onClose} className="flex-1 py-3 rounded-2xl bg-surface-high font-bold text-fg-muted">Cancel</button>
        <button onClick={onSave} disabled={!canSave} className="flex-1 py-3 rounded-2xl bg-accent text-accent-fg font-bold disabled:opacity-40">Save</button></div>
    </div>
  </div>
}
const inputClass = 'w-full bg-surface-raised border border-DEFAULT rounded-xl px-3.5 py-2.5 text-sm outline-none focus:border-accent'

function ComponentDialog({ component, unitLocked, onClose }: { component: TrackedComponent | null; unitLocked: boolean; onClose: () => void }) {
  const catalog = useFoodCatalogContext()
  const [name, setName] = useState(component?.name ?? '')
  const [unit, setUnit] = useState(component?.unit ?? '')
  const duplicateName = catalog.components.some((candidate) =>
    candidate.id !== component?.id && candidate.name.toLocaleLowerCase() === name.trim().toLocaleLowerCase(),
  )
  const save = async () => {
    if (component) await catalog.updateComponent(component.id, { name, unit })
    else await catalog.addComponent({ name, unit })
    onClose()
  }
  return <DialogFrame title={component ? 'Edit component' : 'Add component'} onClose={onClose} onSave={save} canSave={Boolean(name.trim() && unit.trim() && !duplicateName)}>
    <label className="ds-eyebrow block mb-2">Name</label><input autoFocus value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Caffeine" className={inputClass} />
    {duplicateName && <p className="text-xs text-danger-text mt-2">A component with this name already exists.</p>}
    <label className="ds-eyebrow block mt-4 mb-2">Unit</label><input value={unit} onChange={(e) => setUnit(e.target.value)} disabled={unitLocked} placeholder="e.g. mg" className={inputClass + ' disabled:opacity-50'} />
    {unitLocked && <p className="text-xs text-fg-muted mt-2">The unit is locked because this component is already used.</p>}
  </DialogFrame>
}

function FoodItemDialog({ item, onClose }: { item: FoodItem | null; onClose: () => void }) {
  const catalog = useFoodCatalogContext()
  const [name, setName] = useState(item?.name ?? '')
  const [amount, setAmount] = useState(item?.amount ?? '')
  const [unit, setUnit] = useState(item?.unit ?? '')
  const [values, setValues] = useState<Record<string, string>>(() => Object.fromEntries(Object.entries(item?.componentAmounts ?? {}).map(([id, n]) => [id, String(n)])))
  const save = async () => {
    const numericValues: [string, number][] = Object.entries(values).map(([id, value]) => [id, Number(value)])
    const input = { name, amount, unit, componentAmounts: Object.fromEntries(numericValues.filter(([, n]) => Number.isFinite(n) && n > 0)) }
    if (item) await catalog.updateFoodItem(item.id, input)
    else await catalog.addFoodItem(input)
    onClose()
  }
  return <DialogFrame title={item ? 'Edit food item' : 'Add food item'} onClose={onClose} onSave={save} canSave={Boolean(name.trim() && amount.trim() && unit.trim())}>
    <label className="ds-eyebrow block mb-2">Name</label><input autoFocus value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Bottle of beer" className={inputClass} />
    <div className="grid grid-cols-2 gap-2 mt-4"><div><label className="ds-eyebrow block mb-2">Amount</label><input value={amount} onChange={(e) => setAmount(e.target.value)} placeholder="500" className={inputClass} /></div>
      <div><label className="ds-eyebrow block mb-2">Unit</label><input value={unit} onChange={(e) => setUnit(e.target.value)} placeholder="ml" className={inputClass} /></div></div>
    <div className="ds-eyebrow mt-5 mb-2">Components in one item</div>
    <div className="space-y-2">{catalog.components.map((component) => <label key={component.id} className="flex items-center gap-2 bg-surface-raised border border-DEFAULT rounded-xl px-3 py-2">
      <span className="flex-1 text-sm font-semibold">{component.name}</span><input type="number" min="0" step="any" value={values[component.id] ?? ''} onChange={(e) => setValues((all) => ({ ...all, [component.id]: e.target.value }))} placeholder="0" className="w-20 bg-surface text-right border border-DEFAULT rounded-lg px-2 py-1.5 text-sm outline-none" /><span className="w-14 text-xs text-fg-muted">{component.unit}</span>
    </label>)}</div>
  </DialogFrame>
}
