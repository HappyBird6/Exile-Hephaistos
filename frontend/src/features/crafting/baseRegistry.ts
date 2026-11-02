import bases from './topBases.json'
import policies from './basePolicies.json'
export type WorkbenchBaseKey = keyof typeof bases | keyof typeof policies.legacy
export type BasePolicy = {
  qualityLimit: boolean
  ordinaryCatalyst: boolean
  refinedCatalyst: boolean
  catalystQuality: boolean
  maximumQualityBreach: boolean
  socketExecutionMaximum: number | null
  divine: boolean
  itemClass?: string
  sourcePropertyUnscaled?: boolean
  ruleVersion?: string
  ledgerVersion?: string
  legacyCatalog?: string
}
const familyPolicies: Record<string, BasePolicy> = policies.families
const topOverrides = policies.baseOverrides as Record<
  string,
  Partial<BasePolicy>
>
const topEntries = bases as Record<
  string,
  { id: string; name: string; slug: string; family: string }
>
const legacyEntries = policies.legacy as Record<
  string,
  BasePolicy & { id: string; slug: string; initialText: string }
>
export const baseSlugs = Object.fromEntries([
  ...Object.entries(topEntries).map(([key, base]) => [key, base.slug]),
  ...Object.entries(legacyEntries).map(([key, base]) => [key, base.slug]),
]) as Record<WorkbenchBaseKey, string>
export const baseTexts = Object.fromEntries([
  ...Object.entries(topEntries).map(([key, base]) => [
    key,
    `Item Class: ${familyPolicies[base.family]!.itemClass}\nRarity: Normal\n${base.name}`,
  ]),
  ...Object.entries(legacyEntries).map(([key, base]) => [
    key,
    base.initialText,
  ]),
]) as Record<WorkbenchBaseKey, string>
export function basePolicy(id: string): BasePolicy | undefined {
  const top = Object.entries(topEntries).find(([, base]) => base.id === id)
  if (top) return { ...familyPolicies[top[1].family]!, ...topOverrides[top[0]] }
  return Object.values(legacyEntries).find((base) => base.id === id)
}
export function baseClass(key: string): string {
  const top = topEntries[key]
  if (top) return familyPolicies[top.family]!.itemClass!
  if (key === 'solar') return 'Amulet'
  return (
    legacyEntries[key]?.initialText.match(/^Item Class: (.+)/)?.[1] ?? 'Amulet'
  )
}
export function sourcePropertyUnscaled(key: string): boolean {
  const base = topEntries[key]
  return !!base && familyPolicies[base.family]?.sourcePropertyUnscaled === true
}

// Exact identity only: shared display names (e.g. Shrine Sceptre) stay unresolved.
export function clipboardBase(
  name: string,
  itemClass: string,
): WorkbenchBaseKey | undefined {
  const matches = (Object.keys(baseTexts) as WorkbenchBaseKey[]).filter(
    (key) => {
      const expectedClass = baseClass(key)
      return (
        baseTexts[key].split('\n').at(-1) === name &&
        (expectedClass === itemClass ||
          (['Amulet', 'Amulets'].includes(expectedClass) &&
            ['Amulet', 'Amulets'].includes(itemClass)))
      )
    },
  )
  return matches.length === 1 ? matches[0] : undefined
}

// New selections use the reviewed stronger base; saved/imported identities stay intact.
export const supersededBases: Partial<
  Record<WorkbenchBaseKey, WorkbenchBaseKey>
> = {
  stocky: 'massive',
  bow: 'warmonger',
  body: 'soldier',
  helmet: 'imperial',
}
export const selectableBaseKeys = (
  Object.keys(baseTexts) as WorkbenchBaseKey[]
).filter((key) => !Object.hasOwn(supersededBases, key))
