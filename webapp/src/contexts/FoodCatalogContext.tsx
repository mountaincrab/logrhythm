import { createContext, ReactNode, useContext } from 'react'
import { useAuth } from './AuthContext'
import { useProfileContext } from './ProfileContext'
import { useFoodCatalog } from '../hooks/useFoodCatalog'

type FoodCatalogValue = ReturnType<typeof useFoodCatalog>
const FoodCatalogContext = createContext<FoodCatalogValue | null>(null)

export function FoodCatalogProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const { activeProfileId } = useProfileContext()
  const value = useFoodCatalog(user!.uid, activeProfileId)
  return <FoodCatalogContext.Provider value={value}>{children}</FoodCatalogContext.Provider>
}

export function useFoodCatalogContext() {
  const context = useContext(FoodCatalogContext)
  if (!context) throw new Error('useFoodCatalogContext must be used within FoodCatalogProvider')
  return context
}
