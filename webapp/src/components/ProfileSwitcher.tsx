import { useState } from 'react'
import { Check, Plus } from 'lucide-react'
import { useProfileContext } from '../contexts/ProfileContext'

/** Circular avatar showing the profile's first initial — mirrors the Android
 *  `ProfileAvatar` composable (highlighted = active profile). */
export function ProfileAvatar({ name, highlighted, size = 38 }: { name: string; highlighted: boolean; size?: number }) {
  const initial = (name.trim()[0] ?? '?').toUpperCase()
  return (
    <span
      className="inline-flex items-center justify-center rounded-full font-bold shrink-0"
      style={{
        width: size,
        height: size,
        fontSize: size / 2.4,
        background: highlighted ? 'var(--accent)' : 'var(--surface-high)',
        color: highlighted ? 'var(--accent-fg)' : 'var(--fg)',
      }}
    >
      {initial}
    </span>
  )
}

/** Mobile-only profile switcher: a header avatar that opens a bottom sheet of
 *  profiles (mirrors the Android Home top-bar avatar + ProfileSwitcherSheet).
 *  On desktop the sidebar already carries the switcher, so this is `md:hidden`. */
export default function ProfileSwitcher() {
  const { profiles, activeProfile, activeProfileId, setActiveProfile, createProfile } = useProfileContext()
  const [open, setOpen] = useState(false)
  const [adding, setAdding] = useState(false)
  const [name, setName] = useState('')

  const close = () => {
    setOpen(false)
    setAdding(false)
    setName('')
  }

  const add = async () => {
    const n = name.trim()
    if (!n) return
    const id = await createProfile(n)
    setActiveProfile(id)
    close()
  }

  return (
    <>
      <button onClick={() => setOpen(true)} aria-label="Switch profile" className="rounded-full">
        <ProfileAvatar name={activeProfile?.name ?? '?'} highlighted size={38} />
      </button>

      {open && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/60" onClick={close}>
          <div
            className="w-full sm:max-w-md bg-surface rounded-t-3xl sm:rounded-3xl shadow-dialog p-5 pb-8 max-h-[85dvh] overflow-y-auto no-scrollbar"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 className="text-lg font-extrabold tracking-tightish mb-3">Profiles</h2>
            <div className="flex flex-col">
              {profiles.map((p) => {
                const active = p.id === activeProfileId
                return (
                  <button
                    key={p.id}
                    onClick={() => { setActiveProfile(p.id); close() }}
                    className="flex items-center gap-3 py-2.5 px-1 rounded-xl text-left hover:bg-surface-high transition-colors"
                  >
                    <ProfileAvatar name={p.name} highlighted={active} size={36} />
                    <span className="flex-1 text-[15px] font-semibold text-fg truncate">{p.name}</span>
                    {active && <Check size={20} className="text-accent-text shrink-0" />}
                  </button>
                )
              })}

              {adding ? (
                <div className="flex gap-2 mt-2">
                  <input
                    autoFocus
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    onKeyDown={(e) => { if (e.key === 'Enter') add(); if (e.key === 'Escape') { setAdding(false); setName('') } }}
                    placeholder="e.g. Alex"
                    className="flex-1 bg-surface-raised border border-DEFAULT rounded-xl px-3 py-2.5 text-sm text-fg outline-none focus:border-accent placeholder:text-fg-faint transition-colors"
                  />
                  <button
                    onClick={add}
                    disabled={!name.trim()}
                    className="px-4 rounded-xl bg-accent text-accent-fg font-semibold text-sm disabled:opacity-40"
                  >
                    Add
                  </button>
                </div>
              ) : (
                <button
                  onClick={() => setAdding(true)}
                  className="flex items-center gap-3 py-3 px-1 rounded-xl text-left hover:bg-surface-high transition-colors"
                >
                  <span className="w-9 h-9 rounded-full bg-surface-high inline-flex items-center justify-center text-accent-text shrink-0">
                    <Plus size={20} />
                  </span>
                  <span className="text-[15px] font-semibold text-accent-text">Add profile</span>
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  )
}
