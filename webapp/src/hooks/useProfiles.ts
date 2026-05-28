import { useEffect, useState } from 'react'
import {
  collection, doc, query, where, onSnapshot,
  setDoc, updateDoc, serverTimestamp, Timestamp,
} from 'firebase/firestore'
import { db } from '../firebase'
import { Profile, AppThemeName } from '../types'

export const DEFAULT_PROFILE_ID = 'default'

const tsToMillis = (v: unknown): number =>
  v instanceof Timestamp ? v.toMillis() : typeof v === 'number' ? v : Date.now()

function mapProfile(id: string, data: Record<string, unknown>): Profile {
  return {
    id,
    name: (data.name as string) ?? 'Me',
    theme: ((data.theme as AppThemeName) || 'DEEP_NAVY'),
    createdAt: (data.createdAt as number) ?? Date.now(),
    updatedAt: tsToMillis(data.updatedAt),
    isDeleted: (data.isDeleted as boolean) ?? false,
  }
}

export function useProfiles(userId: string) {
  const [profiles, setProfiles] = useState<Profile[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const q = query(
      collection(db, 'users', userId, 'profiles'),
      where('isDeleted', '==', false),
    )
    return onSnapshot(q, (snap) => {
      const list = snap.docs
        .map((d) => mapProfile(d.id, d.data()))
        .sort((a, b) => a.createdAt - b.createdAt)
      setProfiles(list)
      setLoading(false)
    })
  }, [userId])

  const profilesCol = () => collection(db, 'users', userId, 'profiles')

  const ensureDefaultProfile = async () => {
    await setDoc(doc(profilesCol(), DEFAULT_PROFILE_ID), {
      name: 'Me',
      theme: 'DEEP_NAVY',
      createdAt: Date.now(),
      updatedAt: serverTimestamp(),
      isDeleted: false,
    }, { merge: true })
  }

  const createProfile = async (name: string): Promise<string> => {
    const id = crypto.randomUUID()
    await setDoc(doc(profilesCol(), id), {
      name: name.trim(),
      theme: 'DEEP_NAVY',
      createdAt: Date.now(),
      updatedAt: serverTimestamp(),
      isDeleted: false,
    })
    return id
  }

  const renameProfile = async (id: string, name: string) => {
    await updateDoc(doc(profilesCol(), id), { name: name.trim(), updatedAt: serverTimestamp() })
  }

  const deleteProfile = async (id: string) => {
    await updateDoc(doc(profilesCol(), id), { isDeleted: true, updatedAt: serverTimestamp() })
  }

  const setProfileTheme = async (id: string, theme: AppThemeName) => {
    await updateDoc(doc(profilesCol(), id), { theme, updatedAt: serverTimestamp() })
  }

  return { profiles, loading, ensureDefaultProfile, createProfile, renameProfile, deleteProfile, setProfileTheme }
}
