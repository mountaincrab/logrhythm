import { useEffect, useState } from 'react'
import Sheet, { Field } from '../Sheet'
import WhenField from '../WhenField'
import { MedicationDialog, MedicationPicker, inputClass } from '../MedicationFields'
import { MedicineInput } from '../../hooks/useEntries'
import { useMedicationsContext } from '../../contexts/MedicationsContext'

interface Props {
  onClose: () => void
  onSave: (input: MedicineInput) => Promise<void>
  onDelete?: () => void
  initial?: MedicineInput
}

/**
 * Logs a one-off dose, and edits any dose — including one a schedule added, which is how
 * you correct the quantity you actually took.
 */
export default function AddMedicineSheet({ onClose, onSave, onDelete, initial }: Props) {
  const { medications, addMedication } = useMedicationsContext()
  const [occurredAt, setOccurredAt] = useState(initial?.occurredAt ?? Date.now())
  const [medicationId, setMedicationId] = useState<string | null>(initial?.medicationId ?? null)
  const [quantity, setQuantity] = useState(initial?.quantity ?? '1')
  const [notes, setNotes] = useState(initial?.notes ?? '')
  const [saving, setSaving] = useState(false)
  const [creating, setCreating] = useState(false)

  // Single-medication users shouldn't have to pick every time.
  useEffect(() => {
    if (medicationId || initial || medications.length !== 1) return
    setMedicationId(medications[0].id)
  }, [medications, medicationId, initial])

  const selected = medications.find((m) => m.id === medicationId)

  const save = async () => {
    if (!medicationId) return
    setSaving(true)
    try {
      await onSave({
        occurredAt,
        medicationId,
        medicationName: selected?.name ?? initial?.medicationName ?? '',
        dose: selected?.dose ?? initial?.dose ?? '',
        quantity: quantity.trim(),
        notes: notes.trim() || null,
      })
      onClose()
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <Sheet
        title={initial ? 'Edit dose' : 'Log medicine'}
        onClose={onClose}
        onSave={save}
        onDelete={onDelete}
        saveLabel={initial ? 'Save' : 'Save dose'}
        canSave={medicationId !== null}
        saving={saving}
      >
        <WhenField value={occurredAt} onChange={setOccurredAt} />

        <Field label="Medication">
          {medications.length === 0 && (
            <p className="text-[13px] text-fg-muted mb-2">No medications yet — add one to log a dose.</p>
          )}
          <MedicationPicker
            medications={medications}
            selectedId={medicationId}
            onSelect={(m) => setMedicationId(m.id)}
            onCreateNew={() => setCreating(true)}
          />
        </Field>

        {/* How many units — the strength itself is part of the medication's definition. */}
        <Field label="Quantity" hint={selected?.dose ? `× ${selected.dose}` : undefined}>
          <input
            className={inputClass}
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            placeholder="e.g. 2"
          />
        </Field>

        <Field label="Note" hint="optional">
          <textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Why you took it, how it went…"
            className={inputClass + ' resize-none min-h-[80px] font-normal text-sm'}
          />
        </Field>
      </Sheet>

      {creating && (
        <MedicationDialog
          onSave={async (draft) => {
            setMedicationId(await addMedication(draft))
          }}
          onClose={() => setCreating(false)}
        />
      )}
    </>
  )
}
