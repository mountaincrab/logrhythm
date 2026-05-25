import { ReactNode } from 'react'
import BottomNav from './BottomNav'

interface Props {
  title: string
  subtitle?: string
  headerRight?: ReactNode
  /** Optional bar rendered just above the bottom nav (e.g. Home's log bar). */
  footer?: ReactNode
  /** Remove default horizontal padding on the scroll area (e.g. History tabs). */
  flushContent?: boolean
  children: ReactNode
}

export default function AppShell({ title, subtitle, headerRight, footer, flushContent, children }: Props) {
  return (
    <div className="h-[100dvh] bg-bg flex justify-center">
      <div className="w-full max-w-md flex flex-col bg-bg relative overflow-hidden">
        <header className="px-5 pt-3 pb-1 flex items-center justify-between gap-3 shrink-0">
          <div className="min-w-0">
            <h1 className="text-[26px] font-extrabold tracking-tightish leading-tight truncate">{title}</h1>
            {subtitle && <p className="text-[13px] text-fg-muted mt-0.5 truncate">{subtitle}</p>}
          </div>
          {headerRight}
        </header>

        <main className={'flex-1 min-h-0 overflow-y-auto no-scrollbar ' + (flushContent ? '' : 'px-0')}>
          {children}
        </main>

        {footer}
        <BottomNav />
      </div>
    </div>
  )
}
