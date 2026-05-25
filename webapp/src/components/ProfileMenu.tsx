import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { ChevronDown, Check, UserCog } from 'lucide-react'
import { useProfileContext } from '../contexts/ProfileContext'

export default function ProfileMenu() {
  const { profiles, activeProfile, setActiveProfile } = useProfileContext()
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    const onClick = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', onClick)
    return () => document.removeEventListener('mousedown', onClick)
  }, [open])

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={() => setOpen((o) => !o)}
        className="flex items-center gap-1.5 h-10 px-3 rounded-xl bg-surface text-fg text-sm font-semibold max-w-[150px]"
      >
        <span className="truncate">{activeProfile?.name ?? 'Profile'}</span>
        <ChevronDown size={16} className="text-fg-muted shrink-0" />
      </button>

      {open && (
        <div className="absolute right-0 mt-2 w-56 bg-surface-raised border border-DEFAULT rounded-xl shadow-dialog z-30 overflow-hidden">
          <div className="ds-eyebrow px-4 pt-3 pb-1.5">Profiles</div>
          {profiles.map((p) => (
            <button
              key={p.id}
              onClick={() => { setActiveProfile(p.id); setOpen(false) }}
              className="w-full flex items-center justify-between px-4 py-2.5 text-sm text-fg hover:bg-surface-high transition-colors"
            >
              <span className="truncate">{p.name}</span>
              {p.id === activeProfile?.id && <Check size={16} className="text-accent-text shrink-0" />}
            </button>
          ))}
          <Link
            to="/settings"
            onClick={() => setOpen(false)}
            className="flex items-center gap-2 px-4 py-2.5 text-sm text-fg-muted hover:bg-surface-high border-t border-subtle transition-colors"
          >
            <UserCog size={16} /> Manage profiles
          </Link>
        </div>
      )}
    </div>
  )
}
