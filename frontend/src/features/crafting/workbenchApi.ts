import { craftingFetch as fetch } from '../../shared/gameIdTransport'
import { poolHeaders, acceptPoolUpdate } from './modifier-pool/poolSnapshots'
import { currencyOmens } from './crafting-state/activeOmens'
import { effectiveDefinitions } from './augmentRules'
import { observedSanctified } from './sanctification/sanctification'
import type { Sanctification } from './sanctification/sanctification'
import type { ItemObservations } from './itemObservations'
import { rulesetHeaders, verifyRulesetResponse } from './rulesetIdentity'
import { translate } from '../../shared/i18n/i18n'
import { supportsConcreteStateShape } from './workbenchStateShape'
import {
  modifierTemplates as displayBindings,
} from '../../shared/i18n/localeData'
import type { CatalystQuality } from './catalystQuality'
import type { QualityLimit } from './qualityLimit'
import type { Bucket, Definition, Initial } from './craftingApi'
import type { WorkbenchAction } from './currencyDefinitions'
import { verifyCurrencyResult } from './verifyCurrencyResult'
export {
  catalystActionType,
  workbenchCurrencyActions,
  workbenchActionNames,
  workbenchOmens,
  legacyHomogenisingIds,
  legacyFiveIds,
  legacyOmenIds,
  compatibleOmenPair,
  baseWorkbenchAction,
} from './currencyDefinitions'
export type { CatalystAction, WorkbenchAction } from './currencyDefinitions'

export interface DesecratedSlot {
  revealPool?: string | null
  minimumModifierLevel?: 40 | null
  id: string
  side: 'PREFIX' | 'SUFFIX'
  modifierId: string | null
}
export interface ConcreteItem extends Omit<Bucket, 'modifierIds'> {
  installedAugments?: { slotIndex: number; augmentId: string }[]
  sanctification?: Sanctification | null
  observations?: ItemObservations | null
  equipmentQuality?: number | null
  desecrated?: DesecratedSlot[]
  qualityInfused?: boolean
  catalystQuality?: CatalystQuality | null | undefined
  augmentSockets?: number | null | undefined
  explicits: {
    modifierId: string
    values: Record<string, number>
    crafted?: boolean
    fractured?: boolean
  }[]
}
export interface MappingResult {
  mapped: boolean
  state: ConcreteItem | null
  issues: { lineNumber: number; message: string }[]
}
export async function mapSolarText(
  text: string,
  signal: AbortSignal,
  definitions: Record<string, Definition>,
  rulesetIdentity: string,
  before?: ConcreteItem,
): Promise<MappingResult> {
  const response = await fetch(
    `/api/v1/crafting/workbench/${before ? 'edit-text' : 'map-text'}`,
    {
      method: 'POST',
      signal,
      headers: rulesetHeaders(rulesetIdentity),
      body: JSON.stringify({ text, ...(before ? { state: before } : {}) }),
    },
  )
  if (!response.ok)
    throw new Error(
      'Could not validate this item for crafting. The displayed text is preserved.',
    )
  verifyRulesetResponse(response, rulesetIdentity)
  return verifyTextMapping(await response.json(), definitions, before)
}
export function verifyTextMapping(
  value: unknown,
  definitions: Record<string, Definition>,
  before?: ConcreteItem,
): MappingResult {
  const v = value as MappingResult
  if (
    typeof v?.mapped !== 'boolean' ||
    !Array.isArray(v.issues) ||
    !v.issues.every(
      (i) => Number.isInteger(i.lineNumber) && typeof i.message === 'string',
    ) ||
    (v.mapped
      ? !v.state || !Array.isArray(v.state.explicits) || v.issues.length !== 0
      : v.state !== null)
  )
    throw new Error(
      'Could not verify the item mapping. The displayed text is preserved.',
    )
  if (
    v.state &&
    (!supportsConcreteStateShape(v.state) ||
      typeof v.state.snapshotId !== 'string' ||
      typeof v.state.baseItemId !== 'string' ||
      !Number.isInteger(v.state.itemLevel) ||
      v.state.itemLevel < 1 ||
      v.state.itemLevel > 100 ||
      !['NORMAL', 'MAGIC', 'RARE'].includes(v.state.rarity) ||
      !Array.isArray(v.state.implicits) ||
      v.state.implicits.length > 1 ||
      !Array.isArray(v.state.conditions) ||
      (!before &&
        v.state.conditions.length !== 0 &&
        !(
          v.state.conditions.length === 1 &&
          (v.state.conditions[0] === 'SANCTIFIED' ||
            (v.state.conditions[0] === 'CORRUPTED' &&
              v.state.desecrated?.length))
        )) ||
      v.state.explicits.length > 7 ||
      ![...v.state.implicits, ...v.state.explicits].every(
        (m) =>
          m &&
          definitions[m.modifierId]?.stats &&
          typeof m.values === 'object' &&
          m.values !== null &&
          Object.keys(m.values).length ===
            definitions[m.modifierId]!.stats!.length &&
          definitions[m.modifierId]!.stats!.every(
            (s) =>
              Number.isSafeInteger(m.values[s.id]) &&
              (observedSanctified(v.state!) ||
                (m.values[s.id]! >= s.min && m.values[s.id]! <= s.max)),
          ),
      ))
  )
    throw new Error(
      'Could not verify the item mapping. The displayed text is preserved.',
    )
  return v
}
export interface RollAssumption {
  id: string
  candidateUnit: string
  n: number
  candidates: string[]
  min: number | null
  max: number | null
  sourceUrl: string
  reason: string
  ratioTick?: number | null
}
export interface AppliedItem {
  rulesetIdentity: string
  qualityLimit?: QualityLimit | null
  ruleVersion: string
  ledgerVersion: string
  snapshotId: string
  state: ConcreteItem
  action: WorkbenchAction
  applied: boolean
  reason: string
  events: {
    kind: 'ADD' | 'REMOVE' | 'REROLL_IMPLICIT' | 'REROLL_EXPLICIT' | 'FRACTURE'
    modifierId: string
    values: Record<string, number>
    selectionProbability: number
  }[]
  consumedOmens: string[]
  remainingOmens: string[]
  assumptions: RollAssumption[]
}

export { coupledModelsMatch } from './verifyCurrencyResult'

export function concreteInitial(initial: Initial): ConcreteItem {
  return {
    snapshotId: initial.state.snapshotId,
    baseItemId: initial.state.baseItemId,
    itemLevel: initial.state.itemLevel,
    rarity: initial.state.rarity,
    implicits: initial.state.implicits,
    conditions: initial.state.conditions,
    explicits: [],
    ...(initial.equipmentQuality == null
      ? {}
      : { equipmentQuality: initial.equipmentQuality }),
    ...(initial.augmentSockets === undefined
      ? {}
      : { augmentSockets: initial.augmentSockets }),
  }
}
export async function applyCurrency(
  state: ConcreteItem,
  action: WorkbenchAction,
  definitions: Record<string, Definition>,
  signal: AbortSignal,
  activeOmens: string[],
  rulesetIdentity: string,
): Promise<AppliedItem> {
  const omenContext = currencyOmens(activeOmens)
  activeOmens = omenContext.requested
  definitions = effectiveDefinitions(state, definitions)
  if (!supportsConcreteStateShape(state))
    throw new Error(
      'This item contains unsupported properties. Its state has not been changed.',
    )

  const response = await fetch('/api/v1/crafting/workbench/apply', {
    method: 'POST',
    signal,
    headers: {
      ...rulesetHeaders(rulesetIdentity),
      ...poolHeaders(state, rulesetIdentity),
    },
    body: JSON.stringify({ state, action, activeOmens }),
  })
  verifyRulesetResponse(response, rulesetIdentity)
  if (!response.ok)
    throw new Error(
      'Could not apply currency. Your item is unchanged. Please retry.',
    )
  const { modifierPool, ...v } = (await response.json()) as AppliedItem & {
    modifierPool?: unknown
  }
  if (v.rulesetIdentity !== rulesetIdentity)
    throw new Error(translate('ruleset.request_changed'))
  verifyCurrencyResult(state, action, definitions, activeOmens, v)
  if (signal.aborted) throw new DOMException('Aborted', 'AbortError')
  acceptPoolUpdate(modifierPool, state, v.state, rulesetIdentity, definitions)
  return { ...v, remainingOmens: omenContext.restore(v.remainingOmens) }
}

export function rolledText(
  definition: Definition,
  values: Record<string, number>,
): string {
  if (/^(ruby|emerald|sapphire|diamond):/.test(definition.id)) {
    const binding = (
      displayBindings.definitions as Record<
        string,
        { englishText: string; values: string[]; template: string }
      >
    )[definition.id]
    if (
      binding &&
      binding.englishText === definition.text &&
      definition.stats &&
      !definition.tags?.includes('unscalable')
    ) {
      let text = definition.text
      for (const [i, n] of binding.values.entries()) {
        const stat = definition.stats[i]
        if (stat && Number.isSafeInteger(values[stat.id])) {
          const replacement = n.startsWith('+')
            ? '+' + values[stat.id]
            : String(values[stat.id])
          text = text.replace(n, replacement)
        }
      }
      return text
    }
  }
  if (
    definition.stats &&
    definition.stats.length > 1 &&
    Object.keys(values).length === definition.stats.length &&
    definition.stats.every((s) => s.min === s.max && values[s.id] === s.min)
  )
    return definition.text
  const stat = definition.stats?.length === 1 ? definition.stats[0] : undefined
  const range = /\((-?\d+(?:\.\d+)?)\s*[\u2014\u2013?-]\s*(-?\d+(?:\.\d+)?)\)/
  const match = definition.text.match(range)
  if (
    stat &&
    match &&
    Number(match[1]) === stat.min &&
    Number(match[2]) === stat.max
  )
    return definition.text.replace(range, String(values[stat.id]))
  if (stat && stat.min === stat.max) return definition.text
  return `${definition.name}: ${Object.entries(values)
    .map(
      ([id, value]) => `${id.replaceAll('_', ' ')} = ${value} (source units)`,
    )
    .join(', ')}`
}
