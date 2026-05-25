// Bristol stool scale 1..7 with plain-English aliases.
// Mirrors data/model/Bristol.kt and phone.jsx BRISTOL.

export interface BristolType {
  n: number
  plain: string
  description: string
}

export const BRISTOL_TYPES: BristolType[] = [
  { n: 1, plain: 'Hard lumps', description: 'Separate hard lumps, like nuts' },
  { n: 2, plain: 'Lumpy', description: 'Sausage-shaped but lumpy' },
  { n: 3, plain: 'Cracked', description: 'Sausage with cracks on its surface' },
  { n: 4, plain: 'Smooth', description: 'Smooth and soft, like a sausage' },
  { n: 5, plain: 'Soft lumps', description: 'Soft blobs with clear-cut edges' },
  { n: 6, plain: 'Mushy', description: 'Fluffy pieces, ragged edges' },
  { n: 7, plain: 'Liquid', description: 'Entirely liquid, no solid pieces' },
]

export function bristol(n: number): BristolType | undefined {
  return BRISTOL_TYPES.find((b) => b.n === n)
}
