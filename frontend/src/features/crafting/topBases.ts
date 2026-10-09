import data from './topBases.json'

export type TopBaseKey = keyof typeof data
type TopBase = {
  id: string
  name: string
  slug: string
  family: string
  implicitModifierId?: string
  implicitStats?: { id: string; min: number; max: number }[]
  armour: number
  evasion?: number
  dexterity?: number
  sourceProperties?: Record<
    'en' | 'ko' | 'ja' | 'zh-CN' | 'zh-TW' | 'es',
    string[]
  >
  baseMovementSpeed?: number
  skillLines?: Record<'en' | 'ko' | 'ja' | 'zh-CN' | 'zh-TW' | 'es', string[]>
  energyShield?: number
  strength: number
  intelligence?: number
  requiredLevel: number
  maximumQuality: number | null
  requirements: Record<'en' | 'ko' | 'ja' | 'zh-CN' | 'zh-TW' | 'es', string>
}
export function topBase(key: string): TopBase | undefined {
  return Object.hasOwn(data, key) ? data[key as TopBaseKey] : undefined
}
export const reviewedGloveKeys = (Object.keys(data) as TopBaseKey[]).filter(
  (key) => data[key].family === 'gloves',
)
export function topBaseKey(id: string | undefined): TopBaseKey | undefined {
  return (Object.keys(data) as TopBaseKey[]).find((key) => data[key].id === id)
}

// Legacy entries keep their original selector position.
export const reviewedArmourKeys = (Object.keys(data) as TopBaseKey[]).filter(
  (key) => !['soldier', 'imperial'].includes(key),
)
