import { Link, useLocation } from 'react-router-dom'
import { ClipboardList, CalendarDays, Settings2, LogOut, Check, LucideIcon } from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'
import { useProfileContext } from '../contexts/ProfileContext'

const NAV: { to: string; label: string; icon: LucideIcon }[] = [
  { to: '/', label: 'Home', icon: ClipboardList },
  { to: '/history', label: 'History', icon: CalendarDays },
]

export default function Sidebar() {
  const { user, signOut } = useAuth()
  const { profiles, activeProfileId, setActiveProfile } = useProfileContext()
  const { pathname } = useLocation()

  return (
    <aside className="hidden md:flex w-60 shrink-0 h-[100dvh] bg-surface border-r border-DEFAULT flex-col">
      <Link to="/" className="flex items-center gap-2.5 px-4 h-16 shrink-0 border-b border-DEFAULT">
        <div className="w-8 h-8 rounded-xl bg-grad-accent flex items-center justify-center text-base">📈</div>
        <span className="font-extrabold text-fg text-base tracking-tightish">LogRhythm</span>
      </Link>

      <nav className="px-2 py-3 flex flex-col gap-0.5 shrink-0">
        {NAV.map(({ to, label, icon: Icon }) => {
          const active = to === '/' ? pathname === '/' : pathname.startsWith(to)
          return (
            <Link
              key={to}
              to={to}
              className={
                'flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm font-semibold transition-colors ' +
                (active ? 'bg-accent-soft text-accent-text' : 'text-fg-muted hover:text-fg hover:bg-surface-high')
              }
            >
              <Icon size={17} /> {label}
            </Link>
          )
        })}
      </nav>

      <div className="px-2 flex-1 min-h-0 overflow-y-auto no-scrollbar">
        <div className="ds-eyebrow px-3 pt-1 pb-1.5">Profiles</div>
        <div className="flex flex-col gap-0.5">
          {profiles.map((p) => {
            const active = p.id === activeProfileId
            return (
              <button
                key={p.id}
                onClick={() => setActiveProfile(p.id)}
                className={
                  'flex items-center justify-between gap-2 px-3 py-2 rounded-lg text-sm text-left transition-colors ' +
                  (active ? 'bg-surface-high text-fg font-semibold' : 'text-fg-muted hover:text-fg hover:bg-surface-high')
                }
              >
                <span className="truncate">{p.name}</span>
                {active && <Check size={15} className="text-accent-text shrink-0" />}
              </button>
            )
          })}
        </div>
      </div>

      <div className="border-t border-DEFAULT px-2 py-2 flex flex-col gap-0.5 shrink-0">
        <Link
          to="/settings"
          className={
            'flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm transition-colors ' +
            (pathname === '/settings' ? 'bg-surface-high text-fg font-semibold' : 'text-fg-muted hover:text-fg hover:bg-surface-high')
          }
        >
          <Settings2 size={16} /> Settings
        </Link>
        <div className="flex items-center gap-2 px-3 pt-1.5">
          <span className="flex-1 text-xs text-fg-faint truncate" title={user?.email ?? ''}>{user?.email}</span>
          <button
            onClick={signOut}
            className="p-1.5 rounded-lg text-fg-faint hover:text-fg hover:bg-surface-high transition-colors"
            title="Sign out"
            aria-label="Sign out"
          >
            <LogOut size={15} />
          </button>
        </div>
      </div>
    </aside>
  )
}
