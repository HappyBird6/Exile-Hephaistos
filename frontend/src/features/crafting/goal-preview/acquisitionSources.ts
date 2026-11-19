import { augmentRule } from '../augmentRules'
import {
  materialActions,
  materialNames,
  materialProfiles,
} from '../catalogData'
import { workbenchCurrencyActions } from '../currencyDefinitions'
import { isBone } from '../desecrationCurrency'

export type AcquisitionSource =
  | { kind: 'IMPLICIT' | 'NORMAL'; id?: null }
  | {
      kind: 'ESSENCE' | 'ALLOY' | 'LIQUID' | 'DESECRATION' | 'AUGMENT'
      id: string
    }

const currencyNames = new Map<string, string>()
for (const [itemId, action] of Object.entries(workbenchCurrencyActions)) {
  const name = materialNames[itemId]
  if (name) currencyNames.set(action, name)
}

/** English Modifiers Calc headings and the registered material's exact English name. */
export function acquisitionSourceLabel(source: AcquisitionSource) {
  if (source.kind === 'IMPLICIT')
    return source.id == null ? 'Implicit' : undefined
  if (source.kind === 'NORMAL') return source.id == null ? 'base' : undefined
  if (!source.id) return undefined
  if (source.kind === 'DESECRATION')
    return isBone(source.id) ? 'Desecrated Modifiers' : undefined
  // Each augment that unlocks a roll pool has its own named Modifiers Calc section.
  if (source.kind === 'AUGMENT')
    return augmentRule(source.id).pool ? materialNames[source.id] : undefined

  const name = currencyNames.get(source.id)
  const profile = materialProfiles[materialActions[source.id] ?? '']
  if (!name || profile?.kind !== source.kind) return undefined
  if (source.kind === 'LIQUID') return `Liquid Emotions (${name})`
  if (source.kind === 'ESSENCE' && profile.mode === 'ADD')
    return `Essence (${name})`
  if (
    source.kind === 'ALLOY' ||
    (source.kind === 'ESSENCE' && profile.mode === 'REPLACE')
  )
    return `Perfect Essence / Alloy (${name})`
  return undefined
}

export function acquisitionSourceLabels(sources: readonly AcquisitionSource[]) {
  const labels = new Set<string>()
  for (const source of sources) {
    const label = acquisitionSourceLabel(source)
    if (!label) throw new Error('Unknown modifier acquisition source')
    labels.add(label)
  }
  return [...labels]
}
