import { ReactNode } from 'react'
import { X } from 'lucide-react'

interface Props {
  title: string
  onClose: () => void
  onSave: () => void
  saveLabel: string
  canSave: boolean
  saving: boolean
  onDelete?: () => void
  children: ReactNode
}

export default function Sheet({ title, onClose, onSave, saveLabel, canSave, saving, onDelete, children }: Props) {
  return (
    <div className="fixed inset-0 z-40 flex items-end sm:items-center justify-center bg-black/60">
      <div className="w-full sm:max-w-md bg-bg sm:rounded-3xl rounded-t-3xl shadow-dialog flex flex-col max-h-[92dvh] overflow-hidden">
        <div className="px-5 pt-4 pb-2 flex items-center gap-3 shrink-0">
          <button
            onClick={onClose}
            aria-label="Close"
            className="w-9 h-9 rounded-xl bg-surface flex items-center justify-center text-fg-muted hover:text-fg transition-colors"
          >
            <X size={20} />
          </button>
          <h1 className="text-[22px] font-extrabold tracking-tightish flex-1">{title}</h1>
        </div>

        <div className="flex-1 min-h-0 overflow-y-auto no-scrollbar px-5 pb-6 pt-1">{children}</div>

        <div className="px-5 py-3 border-t border-DEFAULT bg-surface flex gap-2 shrink-0">
          {onDelete && (
            <button
              onClick={onDelete}
              className="px-4 py-3.5 rounded-2xl text-[15px] font-bold bg-surface-high text-danger-text"
            >
              Delete
            </button>
          )}
          <button
            onClick={onClose}
            className="flex-1 px-4 py-3.5 rounded-2xl text-[15px] font-bold bg-surface-high text-fg-muted"
          >
            Cancel
          </button>
          <button
            onClick={onSave}
            disabled={!canSave || saving}
            className="flex-1 px-4 py-3.5 rounded-2xl text-[15px] font-bold bg-accent text-accent-fg shadow-fab disabled:opacity-40 disabled:shadow-none transition-opacity"
          >
            {saving ? 'Saving…' : saveLabel}
          </button>
        </div>
      </div>
    </div>
  )
}

export function Field({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) {
  return (
    <div className="mb-[18px]">
      <div className="flex items-center justify-between mb-2">
        <span className="ds-eyebrow">{label}</span>
        {hint && <span className="text-[11px] text-accent-text font-semibold">{hint}</span>}
      </div>
      {children}
    </div>
  )
}
