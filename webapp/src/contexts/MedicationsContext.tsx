import { createContext, useContext, ReactNode } from 'react'
import { useAuth } from './AuthContext'
import { useProfileContext } from './ProfileContext'
import { useMedications } from '../hooks/useMedications'

type MedicationsValue = ReturnType<typeof useMedications>

const MedicationsContext = createContext<MedicationsValue | null>(null)

export function MedicationsProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const { activeProfileId } = useProfileContext()
  const value = useMedications(user!.uid, activeProfileId)
  return <MedicationsContext.Provider value={value}>{children}</MedicationsContext.Provider>
}

export function useMedicationsContext() {
  const ctx = useContext(MedicationsContext)
  if (!ctx) throw new Error('useMedicationsContext must be used within MedicationsProvider')
  return ctx
}
