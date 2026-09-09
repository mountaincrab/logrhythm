import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  collection, doc, getDoc, onSnapshot, query, serverTimestamp, setDoc, updateDoc,
} from 'firebase/firestore'
import { db } from '../firebase'
import { FoodItem, TrackedComponent } from '../types'
import { DEFAULT_FOOD_ITEM_ICON } from '../lib/food'

function numberMap(value: unknown): Record<string, number> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return {}
  return Object.fromEntries(
    Object.entries(value as Record<string, unknown>)
      .filter((entry): entry is [string, number] => typeof entry[1] === 'number' && Number.isFinite(entry[1])),
  )
}

function mapComponent(id: string, d: Record<string, unknown>): TrackedComponent {
  return {
    id,
    profileId: (d.profileId as string) ?? 'default',
    name: (d.name as string) ?? '',
    unit: (d.unit as string) ?? '',
    sortOrder: (d.sortOrder as number) ?? 0,
    createdAt: (d.createdAt as number) ?? 0,
    isArchived: (d.isArchived as boolean) ?? false,
  }
}

function mapFoodItem(id: string, d: Record<string, unknown>): FoodItem {
  return {
    id,
    profileId: (d.profileId as string) ?? 'default',
    name: (d.name as string) ?? '',
    icon: typeof d.icon === 'string' && d.icon.trim() ? d.icon : DEFAULT_FOOD_ITEM_ICON,
    amount: String(d.amount ?? ''),
    unit: (d.unit as string) ?? '',
    componentAmounts: numberMap(d.componentAmounts),
    sortOrder: (d.sortOrder as number) ?? 0,
    createdAt: (d.createdAt as number) ?? 0,
    isArchived: (d.isArchived as boolean) ?? false,
  }
}

export interface ComponentInput { name: string; unit: string }
export interface FoodItemInput {
  name: string
  icon: string
  amount: string
  unit: string
  componentAmounts: Record<string, number>
}

export function useFoodCatalog(userId: string, profileId: string) {
  const [allComponents, setAllComponents] = useState<TrackedComponent[]>([])
  const [allFoodItems, setAllFoodItems] = useState<FoodItem[]>([])
  const [loading, setLoading] = useState(true)
  const col = useCallback((name: string) => collection(db, 'users', userId, name), [userId])

  // Startup invariant shared with Android: create defaults only if their stable ids do not
  // exist, so a rename/archive is respected and two devices converge on the same documents.
  useEffect(() => {
    const ensure = async (key: string, name: string, unit: string, sortOrder: number) => {
      const id = `component_${profileId}_${key}`
      const ref = doc(col('tracked_components'), id)
      if ((await getDoc(ref)).exists()) return
      const now = Date.now()
      await setDoc(ref, {
        userId, profileId, name, unit, sortOrder,
        createdAt: now, updatedAt: serverTimestamp(), isArchived: false,
      })
    }
    void Promise.all([
      ensure('caffeine', 'Caffeine', 'mg', 0),
      ensure('alcohol', 'Alcohol', 'UK units', 1),
    ])
  }, [col, profileId, userId])

  useEffect(() => {
    setLoading(true)
    let componentsReady = false
    let itemsReady = false
    const ready = () => { if (componentsReady && itemsReady) setLoading(false) }
    const unsubComponents = onSnapshot(query(col('tracked_components')), (snap) => {
      setAllComponents(snap.docs.map((d) => mapComponent(d.id, d.data()))
        .filter((x) => x.profileId === profileId)
        .sort((a, b) => a.sortOrder - b.sortOrder || a.name.localeCompare(b.name)))
      componentsReady = true
      ready()
    })
    const unsubItems = onSnapshot(query(col('food_items')), (snap) => {
      setAllFoodItems(snap.docs.map((d) => mapFoodItem(d.id, d.data()))
        .filter((x) => x.profileId === profileId)
        .sort((a, b) => a.sortOrder - b.sortOrder || a.name.localeCompare(b.name)))
      itemsReady = true
      ready()
    })
    return () => { unsubComponents(); unsubItems() }
  }, [col, profileId])

  const components = useMemo(() => allComponents.filter((x) => !x.isArchived), [allComponents])
  const foodItems = useMemo(() => allFoodItems.filter((x) => !x.isArchived), [allFoodItems])
  const archivedComponents = useMemo(() => allComponents.filter((x) => x.isArchived), [allComponents])
  const archivedFoodItems = useMemo(() => allFoodItems.filter((x) => x.isArchived), [allFoodItems])
  const componentsById = useMemo(() => new Map(allComponents.map((x) => [x.id, x])), [allComponents])
  const foodItemsById = useMemo(() => new Map(allFoodItems.map((x) => [x.id, x])), [allFoodItems])

  const addComponent = async (input: ComponentInput) => {
    const id = crypto.randomUUID()
    await setDoc(doc(col('tracked_components'), id), {
      userId, profileId, name: input.name.trim(), unit: input.unit.trim(),
      sortOrder: allComponents.length, createdAt: Date.now(), updatedAt: serverTimestamp(), isArchived: false,
    })
    return id
  }
  const updateComponent = async (id: string, input: ComponentInput) => updateDoc(doc(col('tracked_components'), id), {
    name: input.name.trim(), unit: input.unit.trim(), updatedAt: serverTimestamp(),
  })
  const setComponentArchived = async (id: string, isArchived: boolean) => updateDoc(doc(col('tracked_components'), id), {
    isArchived, updatedAt: serverTimestamp(),
  })

  const addFoodItem = async (input: FoodItemInput) => {
    const id = crypto.randomUUID()
    await setDoc(doc(col('food_items'), id), {
      userId, profileId, name: input.name.trim(), amount: input.amount.trim(), unit: input.unit.trim(),
      icon: input.icon.trim(), componentAmounts: input.componentAmounts, sortOrder: allFoodItems.length,
      createdAt: Date.now(), updatedAt: serverTimestamp(), isArchived: false,
    })
    return id
  }
  const updateFoodItem = async (id: string, input: FoodItemInput) => updateDoc(doc(col('food_items'), id), {
    name: input.name.trim(), amount: input.amount.trim(), unit: input.unit.trim(),
    icon: input.icon.trim(), componentAmounts: input.componentAmounts, updatedAt: serverTimestamp(),
  })
  const setFoodItemArchived = async (id: string, isArchived: boolean) => updateDoc(doc(col('food_items'), id), {
    isArchived, updatedAt: serverTimestamp(),
  })

  return {
    components, foodItems, archivedComponents, archivedFoodItems,
    componentsById, foodItemsById, loading,
    addComponent, updateComponent, setComponentArchived,
    addFoodItem, updateFoodItem, setFoodItemArchived,
  }
}
