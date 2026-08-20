import { MedicationForm } from '../types'

/**
 * The medication icon set: one general mark plus one icon per MedicationForm.
 *
 * These replace the emoji this app used to print, for two reasons. There is no emoji for
 * granules, foam, an enema or a suppository, so five forms shared two glyphs — and the one
 * doing most of the work was the pill, which is also what "medicine" itself was drawn as.
 * The general mark is a bottle precisely because it is the one shape in the set that isn't
 * a tablet, so the timeline can say "a dose" and "which form" in the same row without the
 * two looking alike.
 *
 * Mirror of `ui/components/MedicationIcons.kt` — the same path data on the same 32x32
 * viewport. They carry their own colours rather than taking a theme tint: they sit beside
 * the poop/food/note emoji, so they have to read as part of that family on all three
 * themes rather than as chrome. Keep the two files in step — a shape changed here and not
 * there is a difference the user sees when they switch device.
 */

interface Part {
  d: string
  fill: string
  opacity?: number
}

const GENERAL: Part[] = [
  { d: 'M12.5 3H19.5A2 2 0 0 1 21.5 5V6.4A2 2 0 0 1 19.5 8.4H12.5A2 2 0 0 1 10.5 6.4V5A2 2 0 0 1 12.5 3Z', fill: '#46557A' },
  { d: 'M13.05 4.2H15.75A0.85 0.85 0 0 1 16.6 5.05V5.05A0.85 0.85 0 0 1 15.75 5.9H13.05A0.85 0.85 0 0 1 12.2 5.05V5.05A0.85 0.85 0 0 1 13.05 4.2Z', fill: '#7C8CB2' },
  { d: 'M11.5 8H20.5A4.5 4.5 0 0 1 25 12.5V24.5A4.5 4.5 0 0 1 20.5 29H11.5A4.5 4.5 0 0 1 7 24.5V12.5A4.5 4.5 0 0 1 11.5 8Z', fill: '#F5A623' },
  { d: 'M18 8h2.5A4.5 4.5 0 0 1 25 12.5v12A4.5 4.5 0 0 1 20.5 29H18Z', fill: '#DA8613' },
  { d: 'M15.2 13H16.8A1.2 1.2 0 0 1 18 14.2V22.8A1.2 1.2 0 0 1 16.8 24H15.2A1.2 1.2 0 0 1 14 22.8V14.2A1.2 1.2 0 0 1 15.2 13Z', fill: '#FFFFFF' },
  { d: 'M11.7 16.5H20.3A1.2 1.2 0 0 1 21.5 17.7V19.3A1.2 1.2 0 0 1 20.3 20.5H11.7A1.2 1.2 0 0 1 10.5 19.3V17.7A1.2 1.2 0 0 1 11.7 16.5Z', fill: '#FFFFFF' },
]

const TABLET: Part[] = [
  { d: 'M4 16a12 12 0 1 0 24 0a12 12 0 1 0 -24 0Z', fill: '#A9BBDF' },
  { d: 'M5 16a11 11 0 1 0 22 0a11 11 0 1 0 -22 0Z', fill: '#EEF3FF' },
  { d: 'M16 5a11 11 0 0 1 0 22Z', fill: '#C3D4F2' },
  { d: 'M16 5.7H16A1.3 1.3 0 0 1 17.3 7V25A1.3 1.3 0 0 1 16 26.3H16A1.3 1.3 0 0 1 14.7 25V7A1.3 1.3 0 0 1 16 5.7Z', fill: '#8CA3CE' },
  { d: 'M9 10.8a2.4 1.7 0 1 0 4.8 0a2.4 1.7 0 1 0 -4.8 0Z', fill: '#FFFFFF', opacity: 0.9 },
]

const GRANULES: Part[] = [
  { d: 'M6.9 4H25.1A1.4 1.4 0 0 1 26.5 5.4V7.2A1.4 1.4 0 0 1 25.1 8.6H6.9A1.4 1.4 0 0 1 5.5 7.2V5.4A1.4 1.4 0 0 1 6.9 4Z', fill: '#1D7A63' },
  { d: 'M7.5 8.6h17v15.9a3.5 3.5 0 0 1-3.5 3.5h-10a3.5 3.5 0 0 1-3.5-3.5Z', fill: '#2FA98A' },
  { d: 'M18 8.6h6.5v15.9a3.5 3.5 0 0 1-3.5 3.5H18Z', fill: '#23896E' },
  { d: 'M10.8 14.2a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0 -3.6 0Z', fill: '#F5C84E' },
  { d: 'M15.6 17.6a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0 -3.6 0Z', fill: '#F5C84E' },
  { d: 'M11.6 21.4a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0 -3.6 0Z', fill: '#F5C84E' },
  { d: 'M18.9 12.8a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0Z', fill: '#F5C84E' },
]

const FOAM: Part[] = [
  { d: 'M14.2 2.8H17.8A1.2 1.2 0 0 1 19 4V5A1.2 1.2 0 0 1 17.8 6.2H14.2A1.2 1.2 0 0 1 13 5V4A1.2 1.2 0 0 1 14.2 2.8Z', fill: '#95A1F5' },
  { d: 'M14.2 6H17.8A4.4 4.4 0 0 1 22.2 10.4V24.6A4.4 4.4 0 0 1 17.8 29H14.2A4.4 4.4 0 0 1 9.8 24.6V10.4A4.4 4.4 0 0 1 14.2 6Z', fill: '#6C7BE8' },
  { d: 'M16 6h1.8a4.4 4.4 0 0 1 4.4 4.4v14.2A4.4 4.4 0 0 1 17.8 29H16Z', fill: '#4E5CC9' },
  { d: 'M9.8 13.8H22.2V17.4H9.8Z', fill: '#C3C9F8' },
  { d: 'M22.3 7.4a3.3 3.3 0 1 0 6.6 0a3.3 3.3 0 1 0 -6.6 0Z', fill: '#FFFFFF' },
  { d: 'M26.6 13a2 2 0 1 0 4 0a2 2 0 1 0 -4 0Z', fill: '#FFFFFF' },
  { d: 'M22.8 13.6a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0Z', fill: '#FFFFFF' },
]

const ENEMA: Part[] = [
  { d: 'M16 2.6H16A1.6 1.6 0 0 1 17.6 4.2V7.4A1.6 1.6 0 0 1 16 9H16A1.6 1.6 0 0 1 14.4 7.4V4.2A1.6 1.6 0 0 1 16 2.6Z', fill: '#F5B3C7' },
  { d: 'M14.2 8H17.8A1.4 1.4 0 0 1 19.2 9.4V9.4A1.4 1.4 0 0 1 17.8 10.8H14.2A1.4 1.4 0 0 1 12.8 9.4V9.4A1.4 1.4 0 0 1 14.2 8Z', fill: '#C94F72' },
  { d: 'M7.4 20.4a8.6 8.6 0 1 0 17.2 0a8.6 8.6 0 1 0 -17.2 0Z', fill: '#E96D91' },
  { d: 'M16 11.8a8.6 8.6 0 0 1 0 17.2Z', fill: '#CD4E74' },
  { d: 'M10.2 17a2.2 1.6 0 1 0 4.4 0a2.2 1.6 0 1 0 -4.4 0Z', fill: '#FFFFFF', opacity: 0.55 },
]

const SUPPOSITORY: Part[] = [
  { d: 'M10 22v-6c0-6 2.4-10.6 6-12 3.6 1.4 6 6 6 12v6a6 6 0 0 1-12 0Z', fill: '#F6D06B' },
  { d: 'M16 4c3.6 1.4 6 6 6 12v6a6 6 0 0 1-6 6Z', fill: '#DCAE3C' },
  { d: 'M11.5 12.4a1.5 2.6 0 1 0 3 0a1.5 2.6 0 1 0 -3 0Z', fill: '#FFFFFF', opacity: 0.5 },
]

const BY_FORM: Record<MedicationForm, Part[]> = {
  TABLET,
  GRANULES,
  FOAM,
  ENEMA,
  SUPPOSITORY,
}

function Glyph({ parts, size, className }: { parts: Part[]; size: number; className?: string }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 32 32"
      aria-hidden="true"
      focusable="false"
      className={`inline-block shrink-0 align-[-0.15em] ${className ?? ''}`}
    >
      {parts.map((p, i) => (
        <path key={i} d={p.d} fill={p.fill} fillOpacity={p.opacity} />
      ))}
    </svg>
  )
}

/** The general "a dose was taken" mark — never a specific form. */
export function MedicineIcon({ size = 16, className }: { size?: number; className?: string }) {
  return <Glyph parts={GENERAL} size={size} className={className} />
}

/** The icon for one medication's physical form. */
export function MedicationFormIcon({
  form, size = 16, className,
}: {
  form: MedicationForm
  size?: number
  className?: string
}) {
  return <Glyph parts={BY_FORM[form]} size={size} className={className} />
}
