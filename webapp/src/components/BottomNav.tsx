import { Link, useLocation } from 'react-router-dom'
import { ClipboardList, CalendarDays, Settings2, LucideIcon } from 'lucide-react'

const TABS: { to: string; label: string; icon: LucideIcon }[] = [
  { to: '/', label: 'Home', icon: ClipboardList },
  { to: '/history', label: 'History', icon: CalendarDays },
  { to: '/settings', label: 'Settings', icon: Settings2 },
]

export default function BottomNav() {
  const { pathname } = useLocation()
  return (
    <nav className="flex bg-surface border-t border-DEFAULT px-2 pt-1.5 pb-1 gap-1 shrink-0">
      {TABS.map(({ to, label, icon: Icon }) => {
        const active = to === '/' ? pathname === '/' : pathname.startsWith(to)
        return (
          <Link
            key={to}
            to={to}
            className={
              'flex-1 flex flex-col items-center gap-0.5 py-1.5 text-[11px] font-semibold transition-colors ' +
              (active ? 'text-fg' : 'text-fg-muted')
            }
          >
            <span
              className={
                'w-14 h-[30px] rounded-full flex items-center justify-center transition-colors ' +
                (active ? 'bg-accent-soft text-accent-text' : '')
              }
            >
              <Icon size={20} strokeWidth={2} />
            </span>
            {label}
          </Link>
        )
      })}
    </nav>
  )
}
