import { useState } from 'react'
import { Coffee, Wine } from 'lucide-react'
import Sheet, { Field } from '../Sheet'
import WhenField from '../WhenField'
import { NoteInput } from '../../hooks/useEntries'

interface Props {
  onClose: () => void
  onSave: (input: NoteInput) => Promise<void>
  onDelete?: () => void
  initial?: NoteInput
}

export default function AddNoteSheet({ onClose, onSave, onDelete, initial }: Props) {
  const [occurredAt, setOccurredAt] = useState(initial?.occurredAt ?? Date.now())
  const [content, setContent] = useState(initial?.content ?? '')
  const [caffeine, setCaffeine] = useState(initial?.caffeine ?? false)
  const [alcohol, setAlcohol] = useState(initial?.alcohol ?? false)
  const [saving, setSaving] = useState(false)

  const save = async () => {
    setSaving(true)
    try {
      await onSave({ occurredAt, content: content.trim(), caffeine, alcohol })
      onClose()
    } finally {
      setSaving(false)
    }
  }

  const flags: { on: boolean; set: (v: boolean) => void; label: string; icon: typeof Coffee }[] = [
    { on: caffeine, set: setCaffeine, label: 'Caffeine', icon: Coffee },
    { on: alcohol, set: setAlcohol, label: 'Alcohol', icon: Wine },
  ]

  return (
    <Sheet
      title={initial ? 'Edit note' : 'Log a note'}
      onClose={onClose}
      onSave={save}
      onDelete={onDelete}
      saveLabel={initial ? 'Save' : 'Save note'}
      canSave={content.trim().length > 0}
      saving={saving}
    >
      <WhenField value={occurredAt} onChange={setOccurredAt} />

      <Field label="Note">
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="Symptoms, meds, mood, anything worth remembering…"
          className="w-full bg-surface-raised border border-DEFAULT rounded-xl px-3.5 py-3 text-sm text-fg resize-none min-h-[120px] outline-none focus:border-accent placeholder:text-fg-faint transition-colors"
        />
      </Field>

      <Field label="Flags" hint="optional">
        <div className="grid grid-cols-2 gap-2">
          {flags.map(({ on, set, label, icon: Icon }) => (
            <button
              key={label}
              onClick={() => set(!on)}
              className={
                'flex items-center gap-2.5 rounded-xl px-3 py-2.5 border transition-colors ' +
                (on ? 'bg-accent-soft border-accent' : 'bg-surface-raised border-DEFAULT')
              }
            >
              <span
                className={
                  'w-8 h-8 rounded-lg flex items-center justify-center shrink-0 ' +
                  (on ? 'bg-accent text-accent-fg' : 'bg-surface-high text-fg-muted')
                }
              >
                <Icon size={16} />
              </span>
              <div className="text-left">
                <div className="text-[13px] font-semibold text-fg">{label}</div>
                <div className={'text-[11px] ' + (on ? 'text-accent-text' : 'text-fg-muted')}>{on ? 'Yes' : 'Not today'}</div>
              </div>
            </button>
          ))}
        </div>
      </Field>
    </Sheet>
  )
}
