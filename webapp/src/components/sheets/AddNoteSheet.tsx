import { useState } from 'react'
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
  const [saving, setSaving] = useState(false)

  const save = async () => {
    setSaving(true)
    try {
      await onSave({ occurredAt, content: content.trim() })
      onClose()
    } finally {
      setSaving(false)
    }
  }

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
    </Sheet>
  )
}
