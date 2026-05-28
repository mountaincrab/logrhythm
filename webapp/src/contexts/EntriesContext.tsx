import { createContext, useContext, ReactNode } from 'react'
import { useAuth } from './AuthContext'
import { useProfileContext } from './ProfileContext'
import { useEntries } from '../hooks/useEntries'

type EntriesValue = ReturnType<typeof useEntries>

const EntriesContext = createContext<EntriesValue | null>(null)

export function EntriesProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const { activeProfileId } = useProfileContext()
  const value = useEntries(user!.uid, activeProfileId)
  return <EntriesContext.Provider value={value}>{children}</EntriesContext.Provider>
}

export function useEntriesContext() {
  const ctx = useContext(EntriesContext)
  if (!ctx) throw new Error('useEntriesContext must be used within EntriesProvider')
  return ctx
}
