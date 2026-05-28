import { Clock } from 'lucide-react'
import { Field } from './Sheet'
import { toLocalInputValue, fromLocalInputValue } from '../lib/dates'

export default function WhenField({ value, onChange }: { value: number; onChange: (ms: number) => void }) {
  return (
    <Field label="When">
      <label className="flex items-center gap-2.5 bg-surface-raised border border-DEFAULT rounded-xl px-3.5 py-3 cursor-pointer focus-within:border-accent transition-colors">
        <Clock size={16} className="text-fg-muted shrink-0" />
        <input
          type="datetime-local"
          value={toLocalInputValue(value)}
          onChange={(e) => e.target.value && onChange(fromLocalInputValue(e.target.value))}
          className="flex-1 bg-transparent text-fg font-semibold tabular-nums outline-none [color-scheme:dark]"
        />
      </label>
    </Field>
  )
}
