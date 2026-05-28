import { useState } from 'react'
import { Droplet } from 'lucide-react'
import Sheet, { Field } from '../Sheet'
import { BRISTOL_TYPES, bristol } from '../../lib/bristol'
import { ratingColor } from '../../lib/ratings'
import WhenField from '../WhenField'
import { PoopInput } from '../../hooks/useEntries'

interface Props {
  onClose: () => void
  onSave: (input: PoopInput) => Promise<void>
  onDelete?: () => void
  initial?: PoopInput
}

export default function AddPoopSheet({ onClose, onSave, onDelete, initial }: Props) {
  const [occurredAt, setOccurredAt] = useState(initial?.occurredAt ?? Date.now())
  const [types, setTypes] = useState<number[]>(initial?.bristolTypes ?? [])
  const [blood, setBlood] = useState(initial?.blood ?? 1)
  const [notes, setNotes] = useState(initial?.notes ?? '')
  const [saving, setSaving] = useState(false)

  const toggleType = (n: number) =>
    setTypes((prev) => (prev.includes(n) ? prev.filter((t) => t !== n) : [...prev, n].sort((a, b) => a - b)))

  const selectedDesc = types.length === 1 ? bristol(types[0]) : undefined
  const c = ratingColor(blood)

  const save = async () => {
    setSaving(true)
    try {
      await onSave({ occurredAt, bristolTypes: types, blood, notes: notes.trim() || null })
      onClose()
    } finally {
      setSaving(false)
    }
  }

  return (
    <Sheet
      title={initial ? 'Edit poop' : 'Log a poop'}
      onClose={onClose}
      onSave={save}
      onDelete={onDelete}
      saveLabel={initial ? 'Save' : 'Save poop'}
      canSave={types.length > 0}
      saving={saving}
    >
      <WhenField value={occurredAt} onChange={setOccurredAt} />

      <Field label="Type" hint="Bristol scale">
        <div className="grid grid-cols-7 gap-1.5">
          {BRISTOL_TYPES.map((b) => {
            const on = types.includes(b.n)
            return (
              <button
                key={b.n}
                onClick={() => toggleType(b.n)}
                className={
                  'aspect-[1/1.1] rounded-xl flex flex-col items-center justify-center gap-0.5 border transition-colors ' +
                  (on ? 'bg-accent-soft border-accent' : 'bg-surface-raised border-DEFAULT')
                }
              >
                <span className={'text-lg font-extrabold ' + (on ? 'text-accent-text' : 'text-fg')}>{b.n}</span>
                <span className="text-[9px] font-semibold text-fg-muted leading-none">{b.plain.split(' ')[0]}</span>
              </button>
            )
          })}
        </div>
        <p className="mt-2 text-xs text-fg-muted leading-snug">
          {selectedDesc ? (
            <>
              <b className="text-fg font-semibold">Type {selectedDesc.n} · {selectedDesc.plain}.</b> {selectedDesc.description}.
            </>
          ) : types.length > 1 ? (
            'Multiple types selected.'
          ) : (
            'Pick one or more types that match.'
          )}
        </p>
      </Field>

      <Field label="Blood rating" hint="1–5">
        <div className="grid grid-cols-5 gap-1.5">
          {[1, 2, 3, 4, 5].map((n) => {
            const rc = ratingColor(n)
            const on = n === blood
            return (
              <button
                key={n}
                onClick={() => setBlood(n)}
                className="aspect-square rounded-2xl flex items-center justify-center relative border-[1.5px] transition-colors"
                style={{
                  background: on ? rc.bg : 'var(--surface-raised)',
                  borderColor: on ? rc.bg : 'var(--border)',
                }}
              >
                <span className="text-[22px] font-extrabold" style={{ color: on ? rc.fg : 'var(--fg)' }}>{n}</span>
                {!on && (
                  <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full" style={{ background: rc.bg }} />
                )}
              </button>
            )
          })}
        </div>
        <div className="mt-2 text-[13px] flex items-center gap-1.5">
          <Droplet size={14} style={{ color: c.bg }} />
          <b style={{ color: c.bg }}>{c.label}</b>
          <span className="text-fg-muted">· what you'd see if you looked</span>
        </div>
      </Field>

      <Field label="Notes" hint="optional">
        <textarea
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          placeholder="Urgency, pain, anything that felt different…"
          className="w-full bg-surface-raised border border-DEFAULT rounded-xl px-3.5 py-3 text-sm text-fg resize-none min-h-20 outline-none focus:border-accent placeholder:text-fg-faint transition-colors"
        />
      </Field>
    </Sheet>
  )
}
