import { createContext, useContext, useEffect, useMemo, useState, ReactNode } from 'react'
import { useAuth } from './AuthContext'
import { useProfiles, DEFAULT_PROFILE_ID } from '../hooks/useProfiles'
import { Profile, AppThemeName } from '../types'
import { themeAttr } from '../lib/theme'

interface ProfileContextValue {
  profiles: Profile[]
  loading: boolean
  activeProfileId: string
  activeProfile: Profile | null
  setActiveProfile: (id: string) => void
  createProfile: (name: string) => Promise<string>
  renameProfile: (id: string, name: string) => Promise<void>
  deleteProfile: (id: string) => Promise<void>
  setTheme: (theme: AppThemeName) => Promise<void>
}

const ProfileContext = createContext<ProfileContextValue | null>(null)

const STORAGE_KEY = 'logrhythm:activeProfile'

export function ProfileProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const uid = user!.uid
  const { profiles, loading, ensureDefaultProfile, createProfile, renameProfile, deleteProfile, setProfileTheme } =
    useProfiles(uid)

  const [activeProfileId, setActiveProfileId] = useState<string>(
    () => localStorage.getItem(STORAGE_KEY) ?? DEFAULT_PROFILE_ID,
  )

  // Create a default profile the first time a brand-new account signs in.
  useEffect(() => {
    if (!loading && profiles.length === 0) ensureDefaultProfile()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loading, profiles.length])

  // Keep the active selection pointing at a real profile.
  useEffect(() => {
    if (loading || profiles.length === 0) return
    if (!profiles.some((p) => p.id === activeProfileId)) {
      setActiveProfileId(profiles[0].id)
    }
  }, [loading, profiles, activeProfileId])

  const setActiveProfile = (id: string) => {
    setActiveProfileId(id)
    localStorage.setItem(STORAGE_KEY, id)
  }

  const activeProfile = useMemo(
    () => profiles.find((p) => p.id === activeProfileId) ?? null,
    [profiles, activeProfileId],
  )

  // Apply the active profile's theme to the document.
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', themeAttr(activeProfile?.theme ?? 'DEEP_NAVY'))
  }, [activeProfile?.theme])

  const setTheme = async (theme: AppThemeName) => {
    if (activeProfileId) await setProfileTheme(activeProfileId, theme)
  }

  return (
    <ProfileContext.Provider
      value={{
        profiles, loading, activeProfileId, activeProfile,
        setActiveProfile, createProfile, renameProfile, deleteProfile, setTheme,
      }}
    >
      {children}
    </ProfileContext.Provider>
  )
}

export function useProfileContext() {
  const ctx = useContext(ProfileContext)
  if (!ctx) throw new Error('useProfileContext must be used within ProfileProvider')
  return ctx
}
