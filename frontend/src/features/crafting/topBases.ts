import data from './topBases.json'
import display from '../../shared/i18n/baseDisplay.json'
import type { Locale } from '../../shared/i18n/i18n'
import { translate } from '../../shared/i18n/i18n'

export type TopBaseKey = keyof typeof data
type TopBase = {
  affixLimits?: {
    magicPrefixes: number
    magicSuffixes: number
    rarePrefixes: number
    rareSuffixes: number
  }
  temporaryBaseOption?: { kind: string; source: string; rationale: string }
  pool: string
  id: string
  name: string
  slug: string
  family: string
  implicitModifierId?: string
  implicitText?: string
  implicitStats?: { id: string; min: number; max: number }[]
  armour: number
  evasion?: number
  dexterity?: number
  sourceProperties?: Partial<Record<Locale, string[]>>
  baseMovementSpeed?: number
  skillLines?: Partial<Record<Locale, string[]>>
  energyShield?: number
  strength: number
  intelligence?: number
  requiredLevel: number
  maximumQuality: number | null
  requirements: Record<Locale, string>
}
function withLocalizedDisplay(key: TopBaseKey): TopBase {
  const base = data[key]
  const localized = display[key]
  return {
    ...base,
    requirements: {
      ...base.requirements,
      ru: localized.ru.requirements,
      th: localized.th.requirements,
    },
    sourceProperties: {
      ...('sourceProperties' in base ? base.sourceProperties : {}),
      ru: localized.ru.sourceProperties,
      th: localized.th.sourceProperties,
    },
    skillLines: {
      ...('skillLines' in base ? base.skillLines : {}),
      ru: localized.ru.skillLines,
      th: localized.th.skillLines,
    },
  }
}
const localizedBases = Object.fromEntries(
  (Object.keys(data) as TopBaseKey[]).map((key) => [
    key,
    withLocalizedDisplay(key),
  ]),
)
export function topBase(key: string): TopBase | undefined {
  return Object.hasOwn(localizedBases, key) ? localizedBases[key] : undefined
}
export function basePropertyLines(key: string, locale: Locale): string[] {
  const base = topBase(key)
  const source = base?.sourceProperties?.[locale] ?? []
  const fixedCharmSlot =
    base && ['invoking-belt', 'sinew-belt', 'forking-belt'].includes(key)
  const properties = (fixedCharmSlot ? [] : source).map((line) =>
    line.startsWith('Base Movement Speed:')
      ? translate(
          'base.movement_speed',
          { value: line.slice('Base Movement Speed:'.length).trim() },
          locale,
        )
      : line,
  )
  if (base?.temporaryBaseOption?.kind !== 'SOURCE_FIRST_GRANTED_SKILL_EXAMPLE')
    return [...properties]
  return []
}
export function baseSkillLines(key: string, locale: Locale): string[] {
  const base = topBase(key)
  return base?.temporaryBaseOption?.kind ===
    'SOURCE_FIRST_GRANTED_SKILL_EXAMPLE'
    ? []
    : [...(base?.skillLines?.[locale] ?? [])]
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
