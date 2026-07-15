// Blood rating colours 1..5 (green -> red).
// Mirrors Theme.kt RatingColors and phone.jsx RATING_COLORS.

export interface RatingColor {
  bg: string
  fg: string
  soft: string
  label: string
}

export const RATING_COLORS: Record<number, RatingColor> = {
  1: { bg: '#84CC16', fg: '#0A0A0A', soft: 'rgba(132,204,22,0.16)', label: 'No blood' },
  2: { bg: '#FACC15', fg: '#0A0A0A', soft: 'rgba(250,204,21,0.16)', label: 'Trace' },
  3: { bg: '#F97316', fg: '#FFFFFF', soft: 'rgba(249,115,22,0.18)', label: 'Small amount' },
  4: { bg: '#E64A19', fg: '#FFFFFF', soft: 'rgba(230,74,25,0.18)', label: 'Quite a lot' },
  5: { bg: '#DC2626', fg: '#FFFFFF', soft: 'rgba(220,38,38,0.20)', label: 'Loads' },
}

export function ratingColor(n: number): RatingColor {
  return RATING_COLORS[Math.min(5, Math.max(1, Math.round(n)))]
}

export function ratingBlurb(n: number): string {
  if (n <= 1) return 'All clear — no flare signal here.'
  if (n === 2) return 'More than a stripe — worth keeping an eye on.'
  return 'Worth flagging — talk to your clinician if this keeps up.'
}
