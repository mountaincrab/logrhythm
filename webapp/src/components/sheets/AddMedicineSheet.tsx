import { useEffect, useState } from 'react'
import Sheet, { Field } from '../Sheet'
import WhenField from '../WhenField'
import { DoseFields, MedicationDialog, MedicationPicker, inputClass } from '../MedicationFields'
import { MedicineInput } from '../../hooks/useEntries'
import { useMedicationsContext } from '../../contexts/MedicationsContext'

interface Props {
  onClose: () => void
  onSave: (input: MedicineInput) => Promise<void>
  onDelete?: () => void
  initial?: MedicineInput
}

/**
 * Logs a one-off dose. Regular doses come from the schedule and record themselves, so this
 * is for the exceptions: a painkiller, a rescue dose, something taken off-plan.
 */
export default function AddMedicineSheet({ onClose, onSave, onDelete, initial }: Props) {
  const { medications, addMedication } = useMedicationsContext()
  const [occurredAt, setOccurredAt] = useState(initial?.occurredAt ?? Date.now())
  const [medicationId, setMedicationId] = useState<string | null>(initial?.medicationId ?? null)
  const [amount, setAmount] = useState(initial?.amount ?? '')
  const [unit, setUnit] = useState(initial?.unit ?? '')
  const [notes, setNotes] = useState(initial?.notes ?? '')
  const [saving, setSaving] = useState(false)
  const [creating, setCreating] = useState(false)

  // Single-medication users shouldn't have to pick every time.
  useEffect(() => {
    if (medicationId || initial || medications.length !== 1) return
    const only = medications[0]
    setMedicationId(only.id)
    setAmount(only.defaultAmount)
    setUnit(only.defaultUnit)
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
        amount: amount.trim(),
        unit: unit.trim(),
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
            onSelect={(m) => {
              setMedicationId(m.id)
              setAmount(m.defaultAmount)
              setUnit(m.defaultUnit)
            }}
            onCreateNew={() => setCreating(true)}
          />
        </Field>

        <Field label="Dose" hint="optional">
          <DoseFields amount={amount} unit={unit} onAmount={setAmount} onUnit={setUnit} />
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
            const id = await addMedication(draft)
            setMedicationId(id)
            setAmount(draft.defaultAmount)
            setUnit(draft.defaultUnit)
          }}
          onClose={() => setCreating(false)}
        />
      )}
    </>
  )
}
