import { Link, useLocation } from 'react-router-dom'
import { ClipboardList, CalendarDays, Settings2, LucideIcon } from 'lucide-react'

const NAV: { to: string; label: string; icon: LucideIcon }[] = [
  { to: '/', label: 'Home', icon: ClipboardList },
  { to: '/history', label: 'History', icon: CalendarDays },
  { to: '/settings', label: 'Settings', icon: Settings2 },
]

/** Phone bottom tab bar — mirrors the Android `BottomTabBar` (icon pill +
 *  label, accent-soft highlight on the active tab). Hidden from `md` up, where
 *  the sidebar takes over. */
export default function MobileNav() {
  const { pathname } = useLocation()
  return (
    <nav
      className="md:hidden shrink-0 border-t border-DEFAULT bg-surface"
      style={{ paddingBottom: 'env(safe-area-inset-bottom)' }}
    >
      <div className="mx-auto max-w-4xl flex px-2 py-1.5">
        {NAV.map(({ to, label, icon: Icon }) => {
          const active = to === '/' ? pathname === '/' : pathname.startsWith(to)
          return (
            <Link key={to} to={to} className="flex-1 flex flex-col items-center gap-1 py-1.5">
              <span
                className={
                  'flex items-center justify-center h-[30px] w-14 rounded-full transition-colors ' +
                  (active ? 'bg-accent-soft text-accent-text' : 'text-fg-muted')
                }
              >
                <Icon size={20} />
              </span>
              <span className={'text-[11px] font-semibold ' + (active ? 'text-fg' : 'text-fg-muted')}>{label}</span>
            </Link>
          )
        })}
      </div>
    </nav>
  )
}
