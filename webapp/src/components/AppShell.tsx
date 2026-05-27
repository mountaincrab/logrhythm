import { ReactNode } from 'react'
import { ArrowLeft } from 'lucide-react'
import Sidebar from './Sidebar'

interface Props {
  title: string
  subtitle?: string
  headerRight?: ReactNode
  /** When provided, renders a back arrow before the title. */
  onBack?: () => void
  children: ReactNode
}

export default function AppShell({ title, subtitle, headerRight, onBack, children }: Props) {
  return (
    <div className="flex h-[100dvh] overflow-hidden bg-bg text-fg">
      <Sidebar />
      <div className="flex-1 min-w-0 flex flex-col overflow-hidden">
        <header className="shrink-0 border-b border-DEFAULT">
          <div className="mx-auto w-full max-w-4xl px-6 lg:px-10 h-16 flex items-center justify-between gap-4">
            <div className="flex items-center gap-3 min-w-0">
              {onBack && (
                <button
                  onClick={onBack}
                  className="w-9 h-9 rounded-xl bg-surface-raised border border-DEFAULT flex items-center justify-center text-fg-muted hover:text-fg transition-colors shrink-0"
                  aria-label="Back"
                >
                  <ArrowLeft size={18} />
                </button>
              )}
              <div className="min-w-0">
                <h1 className="text-xl font-extrabold tracking-tightish leading-tight truncate">{title}</h1>
                {subtitle && <p className="text-[13px] text-fg-muted truncate">{subtitle}</p>}
              </div>
            </div>
            {headerRight && <div className="shrink-0">{headerRight}</div>}
          </div>
        </header>

        <main className="flex-1 min-h-0 overflow-y-auto">
          <div className="mx-auto w-full max-w-4xl px-6 lg:px-10 py-6">{children}</div>
        </main>
      </div>
    </div>
  )
}
