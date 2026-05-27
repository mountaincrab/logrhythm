import { useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Pencil, Trash2 } from 'lucide-react'
import AppShell from '../components/AppShell'
import { useEntriesContext } from '../contexts/EntriesContext'
import { EntryKind, NoteEntry } from '../types'
import { ratingColor, ratingBlurb } from '../lib/ratings'
import { bristol } from '../lib/bristol'
import { mealTagLabel } from '../lib/mealTags'
import { formatTime, formatDayFull } from '../lib/dates'
import AddPoopSheet from '../components/sheets/AddPoopSheet'
import AddFoodSheet from '../components/sheets/AddFoodSheet'
import AddNoteSheet from '../components/sheets/AddNoteSheet'

function DetailFrame({
  eyebrow, headerLine, onEdit, children,
}: {
  eyebrow: string; headerLine: string; onEdit: () => void; children: React.ReactNode
}) {
  const navigate = useNavigate()
  return (
    <AppShell
      title={eyebrow}
      subtitle={headerLine}
      onBack={() => navigate(-1)}
      headerRight={
        <button
          onClick={onEdit}
          className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-surface-raised border border-DEFAULT text-fg text-sm font-semibold hover:bg-surface-high transition-colors"
        >
          <Pencil size={16} /> Edit
        </button>
      }
    >
      <div className="max-w-2xl space-y-3">{children}</div>
    </AppShell>
  )
}

function Card({ label, children, className = '' }: { label: string; children: React.ReactNode; className?: string }) {
  return (
    <div className={'bg-surface-raised border border-DEFAULT rounded-2xl px-4 py-3.5 ' + className}>
      <div className="ds-eyebrow mb-1">{label}</div>
      {children}
    </div>
  )
}

function DeleteButton({ onDelete }: { onDelete: () => void }) {
  return (
    <button
      onClick={onDelete}
      className="w-full flex items-center justify-center gap-2 py-3.5 rounded-2xl bg-surface-raised border border-DEFAULT text-danger-text font-bold text-sm hover:bg-surface-high transition-colors"
    >
      <Trash2 size={16} /> Delete entry
    </button>
  )
}

export default function EntryDetailPage() {
  const { kind, id } = useParams<{ kind: EntryKind; id: string }>()
  const navigate = useNavigate()
  const ctx = useEntriesContext()
  const [editing, setEditing] = useState(false)

  const poop = kind === 'poop' ? ctx.poops.find((p) => p.id === id) : undefined
  const food = kind === 'food' ? ctx.foods.find((f) => f.id === id) : undefined
  const note = kind === 'note' ? ctx.notes.find((n) => n.id === id) : undefined
  const entry = poop ?? food ?? note

  const foodBefore = useMemo(() => {
    if (!poop) return []
    const start = poop.occurredAt - 86_400_000
    return ctx.foods
      .filter((f) => f.occurredAt >= start && f.occurredAt <= poop.occurredAt)
      .sort((a, b) => b.occurredAt - a.occurredAt)
  }, [poop, ctx.foods])

  if (!entry) {
    return (
      <AppShell title="Entry" onBack={() => navigate(-1)}>
        <div className="py-24 text-center">
          <p className="text-fg-muted text-sm">This entry no longer exists.</p>
          <button onClick={() => navigate('/')} className="text-accent-text text-sm font-semibold mt-3">Back to home</button>
        </div>
      </AppShell>
    )
  }

  const del = async () => {
    if (!id) return
    if (kind === 'poop') await ctx.deletePoop(id)
    else if (kind === 'food') await ctx.deleteFood(id)
    else if (kind === 'note') await ctx.deleteNote(id)
    navigate(-1)
  }

  const headerLine = `${formatDayFull(entry.occurredAt)} · ${formatTime(entry.occurredAt)}`

  if (poop) {
    const c = ratingColor(poop.blood)
    const sorted = [...poop.bristolTypes].sort((a, b) => a - b)
    const firstDesc = sorted.length === 1 ? bristol(sorted[0]) : undefined
    return (
      <>
        <DetailFrame eyebrow="Poop" headerLine={headerLine} onEdit={() => setEditing(true)}>
          <div className="bg-surface-raised border border-DEFAULT rounded-3xl p-[18px] flex items-center gap-4">
            <div className="w-[88px] h-[88px] rounded-3xl flex items-center justify-center text-5xl font-black shrink-0" style={{ background: c.bg, color: c.fg }}>
              {poop.blood}
            </div>
            <div>
              <div className="ds-eyebrow">Blood rating</div>
              <div className="text-xl font-extrabold mt-0.5" style={{ color: c.bg }}>{c.label}</div>
              <div className="text-xs text-fg-muted mt-0.5 leading-snug">{ratingBlurb(poop.blood)}</div>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <div className="bg-surface-raised border border-DEFAULT rounded-2xl px-3.5 py-3">
              <div className="ds-eyebrow">Time</div>
              <div className="text-lg font-extrabold mt-1 tabular-nums">{formatTime(poop.occurredAt)}</div>
              <div className="text-[11px] text-fg-muted">{formatDayFull(poop.occurredAt)}</div>
            </div>
            <div className="bg-surface-raised border border-DEFAULT rounded-2xl px-3.5 py-3">
              <div className="ds-eyebrow">Stool</div>
              <div className="text-lg font-extrabold mt-1">{sorted.length ? `Bristol ${sorted.join(', ')}` : '—'}</div>
              <div className="text-[11px] text-fg-muted">{firstDesc ? `${firstDesc.plain} · ${firstDesc.description}` : sorted.map((t) => bristol(t)?.plain).join(' / ')}</div>
            </div>
          </div>

          {poop.notes && (
            <Card label="Notes"><div className="text-sm leading-relaxed text-fg">{poop.notes}</div></Card>
          )}

          <Card label="Food in the 24h before">
            {foodBefore.length === 0 ? (
              <div className="text-sm text-fg-muted py-1">Nothing logged in the prior 24 hours.</div>
            ) : (
              foodBefore.map((f) => (
                <div key={f.id} className="flex items-center gap-2.5 py-2 border-t border-subtle first:border-t-0">
                  <div className="w-[52px] text-[11px] text-fg-muted font-mono font-bold tabular-nums">{formatTime(f.occurredAt)}</div>
                  <div className="flex-1 text-[13px]">{f.items}</div>
                </div>
              ))
            )}
          </Card>

          <DeleteButton onDelete={del} />
        </DetailFrame>

        {editing && (
          <AddPoopSheet
            initial={{ occurredAt: poop.occurredAt, bristolTypes: poop.bristolTypes, blood: poop.blood, notes: poop.notes }}
            onClose={() => setEditing(false)}
            onSave={(input) => ctx.updatePoop(poop.id, input)}
          />
        )}
      </>
    )
  }

  if (food) {
    const tag = mealTagLabel(food.mealTag)
    return (
      <>
        <DetailFrame eyebrow="Food" headerLine={headerLine} onEdit={() => setEditing(true)}>
          <Card label="What you ate"><div className="text-sm leading-relaxed text-fg">{food.items}</div></Card>
          {tag && <Card label="Tag"><div className="text-base font-semibold">{tag}</div></Card>}
          <DeleteButton onDelete={del} />
        </DetailFrame>
        {editing && (
          <AddFoodSheet
            initial={{ occurredAt: food.occurredAt, items: food.items, mealTag: food.mealTag }}
            onClose={() => setEditing(false)}
            onSave={(input) => ctx.updateFood(food.id, input)}
          />
        )}
      </>
    )
  }

  // note
  const n = note as NoteEntry
  const flags = [n.caffeine && 'Caffeine', n.alcohol && 'Alcohol'].filter(Boolean) as string[]
  return (
    <>
      <DetailFrame eyebrow="Note" headerLine={headerLine} onEdit={() => setEditing(true)}>
        <Card label="Note"><div className="text-sm leading-relaxed text-fg whitespace-pre-wrap">{n.content}</div></Card>
        {flags.length > 0 && (
          <Card label="Flags">
            <div className="flex flex-wrap gap-1.5 mt-0.5">
              {flags.map((f) => (
                <span key={f} className="px-2.5 py-1 rounded-full bg-surface-high text-fg-muted text-[11px] font-semibold">{f}</span>
              ))}
            </div>
          </Card>
        )}
        <DeleteButton onDelete={del} />
      </DetailFrame>
      {editing && (
        <AddNoteSheet
          initial={{ occurredAt: n.occurredAt, content: n.content, caffeine: n.caffeine, alcohol: n.alcohol }}
          onClose={() => setEditing(false)}
          onSave={(input) => ctx.updateNote(n.id, input)}
        />
      )}
    </>
  )
}
