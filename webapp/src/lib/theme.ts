import { AppThemeName } from '../types'

export interface ThemeMeta {
  id: AppThemeName
  label: string
  attr: string // data-theme value
  accent: string
  bg: string
}

export const THEMES: ThemeMeta[] = [
  { id: 'DEEP_NAVY', label: 'Deep Navy', attr: 'deep-navy', accent: '#4F7CFF', bg: '#0A1020' },
  { id: 'CHARCOAL', label: 'Charcoal', attr: 'charcoal', accent: '#06B6D4', bg: '#0A0A0A' },
  { id: 'RETRO', label: 'Retro', attr: 'retro', accent: '#FF00CC', bg: '#1A0B1E' },
]

export function themeAttr(name: AppThemeName): string {
  return THEMES.find((t) => t.id === name)?.attr ?? 'deep-navy'
}
