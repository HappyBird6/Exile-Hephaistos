import { isQualityInfuser, infuserTransitionMatches } from './qualityInfuser'
import { currencyOmens } from './crafting-state/activeOmens'
import { effectiveDefinitions, isCraftedModifier } from './augmentRules'
import equipmentCurrencies from './equipmentCurrencies.json'
import { observedSanctified } from './sanctification/sanctification'
import type { Sanctification } from './sanctification/sanctification'
import { equivalent } from './sanctification/equivalence'
import { sanctificationTransitionMatches } from './sanctification/sanctification'
import {
  isEquipmentQualityAction,
  qualityTransitionMatches,
} from './qualityCurrency'
import { sameObservations, type ItemObservations } from './itemObservations'
import { rulesetHeaders, verifyRulesetResponse } from './rulesetIdentity'
import { translate } from '../../shared/i18n/i18n'
import { topBase, topBaseKey } from './topBases'
import { basePolicy, baseAffixCapacity } from './baseRegistry'
import { essenceTargets as reviewedEssenceTargets } from './catalogData'
import { supportsConcreteStateShape } from './workbenchStateShape'
import { modifierTemplates as displayBindings } from '../../shared/i18n/localeData'
import liquidTargets from './basicJewelLiquidTargets.json'
import {
  workbenchJewelBases,
  isWorkbenchJewel,
  reviewedBasicJewel,
  jewelCapacity,
} from './basicJewel'
import { qualityCapChangeMatches } from './qualityCapChangePolicy'
import { whittlingCandidates } from './omenRemovalCandidates'
import { preservesChaosJewelOverflow } from './jewelChaos'
import { verifiedCatalystQuality } from './catalystQuality'
import type { CatalystQuality } from './catalystQuality'
import { maximumQuality, qualityLimitMatches } from './qualityLimit'
import type { QualityLimit } from './qualityLimit'
import type { Bucket, Definition, Initial } from './craftingApi'

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
import {
  catalystActionType,
  workbenchOmens,
  legacyHomogenisingIds,
  legacyFiveIds,
  compatibleOmenPair,
  baseWorkbenchAction,
  bowFixedEssenceModifiers,
  fixedEssenceModifiers,
  stockyFixedEssenceModifiers,
  choiceEssenceModifiers,
  wandFixedEssenceModifiers,
  sceptreFixedEssenceModifiers,
  replacementEssenceModifiers,
  stockyReplacementTargets,
} from './currencyDefinitions'
import type { WorkbenchAction } from './currencyDefinitions'

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
  const v = (await response.json()) as MappingResult
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

export function coupledModelsMatch(
  result: Pick<AppliedItem, 'events' | 'assumptions' | 'state'>,
  definitions: Record<string, Definition>,
): boolean {
  const rolled = result.events.filter(
    (event) =>
      ['ADD', 'REROLL_IMPLICIT', 'REROLL_EXPLICIT'].includes(event.kind) &&
      (definitions[event.modifierId]?.stats?.length ?? 0) > 1,
  )
  const models = result.assumptions.filter(
    (a) => a.id === 'user-coupled-ratio-half-up-v1',
  )
  if (models.length !== rolled.length) return false
  const bound = new Set<string>()
  return models.every((a) => {
    const id = a.candidates[0]
    const tick = a.ratioTick
    if (
      !id ||
      a.candidates.length !== 1 ||
      bound.has(id) ||
      a.n !== 10001 ||
      a.min !== 0 ||
      a.max !== 10000 ||
      !Number.isSafeInteger(tick) ||
      tick == null ||
      tick < 0 ||
      tick > 10000
    )
      return false
    bound.add(id)
    const event = rolled.find((e) => e.modifierId === id)
    const instance = [
      ...result.state.implicits,
      ...result.state.explicits,
    ].find((m) => m.modifierId === id)
    const stats = definitions[id]?.stats
    if (
      !event ||
      !instance ||
      !stats ||
      stats.length < 2 ||
      Object.keys(event.values).length !== stats.length
    )
      return false
    return stats.every((s) => {
      if (
        !Number.isSafeInteger(s.min) ||
        !Number.isSafeInteger(s.max) ||
        s.min > s.max
      )
        return false
      const numerator =
        BigInt(s.min) * BigInt(10000 - tick) + BigInt(s.max) * BigInt(tick)
      const sign = numerator < 0n ? -1n : 1n
      const absolute = numerator < 0n ? -numerator : numerator
      const expected = Number(sign * ((absolute + 5000n) / 10000n))
      return (
        Number.isSafeInteger(expected) &&
        event.values[s.id] === expected &&
        instance.values[s.id] === expected
      )
    })
  })
}
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
    headers: rulesetHeaders(rulesetIdentity),
    body: JSON.stringify({ state, action, activeOmens }),
  })
  verifyRulesetResponse(response, rulesetIdentity)
  if (!response.ok)
    throw new Error(
      'Could not apply currency. Your item is unchanged. Please retry.',
    )
  const v = (await response.json()) as AppliedItem
  if (v.rulesetIdentity !== rulesetIdentity)
    throw new Error(translate('ruleset.request_changed'))
  const next = v?.state
  const baseAction = baseWorkbenchAction(action)
  const sameValues = (
    a: Record<string, number> | undefined,
    b: Record<string, number> | undefined,
  ) =>
    Boolean(a && b) &&
    Object.keys(a!).length === Object.keys(b!).length &&
    Object.entries(a!).every(
      ([key, value]) => Object.hasOwn(b!, key) && b![key] === value,
    )
  const fixedTarget =
    state.baseItemId ===
    'Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre1'
      ? sceptreFixedEssenceModifiers[action]
      : state.baseItemId ===
          'Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand3'
        ? wandFixedEssenceModifiers[action]
        : state.baseItemId ===
            'Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow1'
          ? bowFixedEssenceModifiers[action]
          : state.baseItemId === 'Metadata/Items/Armours/Gloves/FourGlovesStr1'
            ? (stockyFixedEssenceModifiers[action] ??
              fixedEssenceModifiers[action])
            : fixedEssenceModifiers[action]
  const reviewedKey = topBaseKey(state.baseItemId)
  const reviewedTargets = reviewedKey
    ? (
        reviewedEssenceTargets as Record<
          string,
          {
            fixed: Record<string, string[]>
            replacements: Record<string, string[]>
          }
        >
      )[reviewedKey]
    : undefined
  const essenceCandidates = reviewedTargets
    ? (reviewedTargets.fixed[action] ?? [])
    : fixedTarget
      ? [fixedTarget]
      : (choiceEssenceModifiers[action] ?? [])
  const baseName = Object.keys(workbenchJewelBases).find(
    (b) =>
      (workbenchJewelBases as Record<string, string>)[b] === state.baseItemId,
  )
  const equipmentTargets = (
    equipmentCurrencies.targets as Record<string, Record<string, string[]>>
  )[state.baseItemId]?.[action]
  const replacementTargets =
    equipmentTargets ??
    (action.includes('LIQUID_')
      ? baseName
        ? ((liquidTargets as Record<string, Record<string, string[]>>)[
            baseName
          ]?.[action] ?? [])
        : []
      : reviewedTargets
        ? (reviewedTargets.replacements[action] ??
          ([
            'PERFECT_ESSENCE_GROUNDING',
            'PERFECT_ESSENCE_OPULENCE',
            'ESSENCE_ABYSS',
          ].includes(action)
            ? stockyReplacementTargets[action]
            : []) ??
          [])
        : ((state.baseItemId === 'Metadata/Items/Armours/Gloves/FourGlovesStr1'
            ? (stockyReplacementTargets[action] ??
              replacementEssenceModifiers[action])
            : replacementEssenceModifiers[action]) ?? []))
  const sameModifiers = (
    a: ConcreteItem['explicits'],
    b: ConcreteItem['explicits'],
  ) =>
    Array.isArray(a) &&
    Array.isArray(b) &&
    a.length === b.length &&
    a.every(
      (m, i) =>
        m.modifierId === b[i]?.modifierId &&
        Boolean(m.fractured) === Boolean(b[i]?.fractured) &&
        Boolean(m.crafted) === Boolean(b[i]?.crafted) &&
        sameValues(m.values, b[i]?.values),
    )
  const validValues = (values: Record<string, number>) =>
    values &&
    typeof values === 'object' &&
    Object.values(values).every(Number.isSafeInteger)
  if (
    !next ||
    !supportsConcreteStateShape(next) ||
    !sameObservations(next.observations, state.observations) ||
    !equivalent(next.desecrated ?? [], state.desecrated ?? []) ||
    (!isQualityInfuser(action) &&
      !qualityTransitionMatches(state, v, action)) ||
    !infuserTransitionMatches(state, v, action, definitions) ||
    !sanctificationTransitionMatches(
      state,
      v,
      action,
      activeOmens,
      definitions,
    ) ||
    !verifiedCatalystQuality(next, definitions) ||
    (isWorkbenchJewel(next.baseItemId) && !reviewedBasicJewel(next)) ||
    (v.applied &&
      action.includes('LIQUID_') &&
      (!isWorkbenchJewel(state.baseItemId) ||
        state.explicits.some((m) =>
          isCraftedModifier(m, definitions[m.modifierId]),
        ))) ||
    (catalystActionType(action) === null &&
      !isQualityInfuser(action) &&
      !qualityCapChangeMatches(state, v, definitions)) ||
    (state.baseItemId === 'Metadata/Items/Rings/FourRing1' &&
      (!sameModifiers(next.implicits, state.implicits) ||
        next.implicits.length !== 1 ||
        next.implicits[0]?.modifierId !==
          'iron-ring:implicit:added-physical-damage-to-attacks' ||
        Object.keys(next.implicits[0].values).length !== 2 ||
        next.implicits[0].values.attack_minimum_added_physical_damage !== 1 ||
        next.implicits[0].values.attack_maximum_added_physical_damage !== 4 ||
        Boolean(next.implicits[0].fractured))) ||
    (v.qualityLimit !== undefined &&
      (v.qualityLimit === null
        ? (!['belts', 'quivers'].includes(
            topBase(reviewedKey ?? '')?.family ?? '',
          ) &&
            ![
              'Metadata/Items/Jewels/JewelRadiusStr',
              'Metadata/Items/Jewels/JewelRadiusDex',
              'Metadata/Items/Jewels/JewelRadiusInt',
              'Metadata/Items/Jewels/JewelRadiusDiamond',
              'Metadata/Items/Belts/FourBelt1',
              'Metadata/Items/Rings/FourRing1',
            ].includes(next.baseItemId)) ||
          maximumQuality(next, definitions) !== null
        : !qualityLimitMatches(v.qualityLimit, next, definitions))) ||
    (v.applied &&
      [
        'LESSER_ESSENCE_COMMAND',
        'ESSENCE_COMMAND',
        'GREATER_ESSENCE_COMMAND',
        'PERFECT_ESSENCE_COMMAND',
      ].includes(action) &&
      state.baseItemId !==
        'Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre1' &&
      topBase(reviewedKey ?? '')?.family !== 'sceptres') ||
    (v.applied &&
      action === 'PERFECT_ESSENCE_INSULATION' &&
      topBase(reviewedKey ?? '')?.family !== 'belts' &&
      state.baseItemId !== 'Metadata/Items/Belts/FourBelt1') ||
    (v.applied &&
      action === 'PERFECT_ESSENCE_MIND' &&
      state.baseItemId !== 'Metadata/Items/Rings/FourRing1' &&
      !['rings', 'amulets'].includes(
        topBase(reviewedKey ?? '')?.family ?? '',
      )) ||
    (v.applied &&
      action === 'PERFECT_ESSENCE_THAWING' &&
      state.baseItemId !== 'Metadata/Items/Armours/Helmets/FourHelmetStr1' &&
      !['helmet', 'helmets'].includes(
        topBase(reviewedKey ?? '')?.family ?? '',
      )) ||
    (v.applied &&
      [
        'PERFECT_ESSENCE_BODY',
        'PERFECT_ESSENCE_RUIN',
        'PERFECT_ESSENCE_SEEKING',
      ].includes(action) &&
      state.baseItemId !== 'Metadata/Items/Armours/BodyArmours/FourBodyStr1' &&
      topBase(reviewedKey ?? '')?.family !== 'body') ||
    (v.applied &&
      [
        'PERFECT_ESSENCE_MIND',
        'PERFECT_ESSENCE_THAWING',
        'PERFECT_ESSENCE_INSULATION',
        'PERFECT_ESSENCE_COMMAND',
        'PERFECT_ESSENCE_BODY',
        'PERFECT_ESSENCE_RUIN',
        'PERFECT_ESSENCE_SEEKING',
        'PERFECT_ESSENCE_SORCERY',
        'PERFECT_ESSENCE_ALACRITY',
        'PERFECT_ESSENCE_ABRASION',
        'PERFECT_ESSENCE_FLAMES',
        'PERFECT_ESSENCE_ICE',
        'PERFECT_ESSENCE_ELECTRICITY',
        'PERFECT_ESSENCE_BATTLE',
        'PERFECT_ESSENCE_HASTE',
        'PERFECT_ESSENCE_GROUNDING',
        'PERFECT_ESSENCE_OPULENCE',
      ].includes(action) &&
      state.itemLevel < 72) ||
    (v.applied &&
      (action.endsWith('_ALLOY') || action === 'ESSENCE_HORROR') &&
      (!equipmentTargets?.length ||
        !equipmentTargets.every((id) => {
          const expected = equipmentCurrencies.definitions.find(
            (d) => d.id === id,
          )
          const actual = definitions[id]
          return (
            expected &&
            actual &&
            state.itemLevel >= expected.requiredItemLevel &&
            (actual.sourceUrl == null
              ? !id.startsWith('equipment:')
              : actual.sourceUrl === expected.sourceUrl) &&
            JSON.stringify(actual.stats) === JSON.stringify(expected.stats)
          )
        }))) ||
    v.action !== action ||
    typeof v.applied !== 'boolean' ||
    typeof v.reason !== 'string' ||
    typeof v.ruleVersion !== 'string' ||
    typeof v.ledgerVersion !== 'string' ||
    v.snapshotId !== state.snapshotId ||
    next.snapshotId !== state.snapshotId ||
    next.baseItemId !== state.baseItemId ||
    next.itemLevel !== state.itemLevel ||
    !['NORMAL', 'MAGIC', 'RARE'].includes(next.rarity) ||
    ((action !== 'DIVINE' || !activeOmens.includes('Omen_of_the_Blessed')) &&
      !sameModifiers(next.implicits, state.implicits)) ||
    !Array.isArray(next.implicits) ||
    next.implicits.length !== state.implicits.length ||
    next.implicits.some((m) => m.fractured) ||
    next.implicits[0]?.modifierId !== state.implicits[0]?.modifierId ||
    !next.implicits.every((m) =>
      definitions[m.modifierId]?.stats?.every(
        (r) =>
          Number.isSafeInteger(m.values[r.id]) &&
          m.values[r.id]! >= r.min &&
          m.values[r.id]! <= r.max,
      ),
    ) ||
    !Array.isArray(next.conditions) ||
    (v.applied &&
    action === 'DIVINE' &&
    activeOmens.includes('Omen_of_Sanctification')
      ? next.conditions.length !== 1 || next.conditions[0] !== 'SANCTIFIED'
      : JSON.stringify([...next.conditions].sort()) !==
        JSON.stringify([...state.conditions].sort())) ||
    !Array.isArray(next.explicits) ||
    next.explicits.length > 7 ||
    !next.explicits.every(
      (m) =>
        definitions[m.modifierId] &&
        validValues(m.values) &&
        (m.fractured === undefined || typeof m.fractured === 'boolean') &&
        (m.crafted === undefined || typeof m.crafted === 'boolean') &&
        definitions[m.modifierId]!.stats?.length ===
          Object.keys(m.values).length &&
        definitions[m.modifierId]!.stats?.every(
          (s) =>
            Number.isSafeInteger(m.values[s.id]) &&
            m.values[s.id]! >= s.min &&
            m.values[s.id]! <= s.max,
        ),
    ) ||
    !Array.isArray(v.events) ||
    !v.events.every(
      (e) =>
        [
          'ADD',
          'REMOVE',
          'REROLL_IMPLICIT',
          'REROLL_EXPLICIT',
          'FRACTURE',
        ].includes(e.kind) &&
        definitions[e.modifierId] &&
        validValues(e.values) &&
        Number.isFinite(e.selectionProbability) &&
        e.selectionProbability > 0 &&
        e.selectionProbability <= 1,
    ) ||
    !Array.isArray(v.consumedOmens) ||
    !Array.isArray(v.remainingOmens) ||
    [...v.consumedOmens, ...v.remainingOmens].length !== activeOmens.length ||
    new Set([...v.consumedOmens, ...v.remainingOmens]).size !==
      activeOmens.length ||
    ![...v.consumedOmens, ...v.remainingOmens].every((id) =>
      activeOmens.includes(id),
    ) ||
    (!v.applied && v.consumedOmens.length !== 0) ||
    !Array.isArray(v.assumptions) ||
    !v.assumptions.every(
      (a) =>
        typeof a.id === 'string' &&
        typeof a.candidateUnit === 'string' &&
        Number.isSafeInteger(a.n) &&
        a.n > 0 &&
        Array.isArray(a.candidates) &&
        a.candidates.every((c) => typeof c === 'string') &&
        typeof a.reason === 'string' &&
        typeof a.sourceUrl === 'string',
    )
  )
    throw new Error(
      'Could not verify the applied item. Your item is unchanged. Please retry.',
    )
  const catalystType = catalystActionType(action)
  if (
    catalystType &&
    (!sameModifiers(next.explicits, state.explicits) ||
      next.rarity !== state.rarity ||
      v.events.length !== 0 ||
      v.assumptions.length !== 0 ||
      v.consumedOmens.length !== 0 ||
      (v.applied
        ? (action.startsWith('REFINED_')
            ? basePolicy(state.baseItemId)?.refinedCatalyst
            : basePolicy(state.baseItemId)?.ordinaryCatalyst) !== true ||
          next.catalystQuality?.type !== catalystType ||
          next.catalystQuality?.amount !==
            Math.max(
              state.catalystQuality?.amount ?? 0,
              maximumQuality(state, definitions) ?? 0,
            )
        : next.catalystQuality?.type !== state.catalystQuality?.type ||
          next.catalystQuality?.amount !== state.catalystQuality?.amount))
  )
    throw new Error(
      'Could not verify the catalyst policy. Your item is unchanged.',
    )
  if (!coupledModelsMatch(v, definitions))
    throw new Error(
      'Could not verify the coupled roll model. Your item is unchanged. Please retry.',
    )
  if (
    JSON.stringify(next.installedAugments ?? []) !==
    JSON.stringify(state.installedAugments ?? [])
  )
    throw new Error('Unverified installed Augments')
  const oldSockets = state.augmentSockets ?? null
  const newSockets = next.augmentSockets ?? null
  if (
    (action === 'ARTIFICER' &&
      v.applied &&
      (basePolicy(state.baseItemId)?.socketExecutionMaximum == null ||
        oldSockets == null ||
        newSockets !== oldSockets + 1 ||
        newSockets > basePolicy(state.baseItemId)!.socketExecutionMaximum! ||
        next.rarity !== state.rarity ||
        !sameModifiers(next.explicits, state.explicits) ||
        !sameModifiers(next.implicits, state.implicits) ||
        v.events.length !== 0 ||
        v.assumptions.length !== 0 ||
        v.consumedOmens.length !== 0)) ||
    ((action !== 'ARTIFICER' || !v.applied) && newSockets !== oldSockets)
  )
    throw new Error(
      'Could not verify the Augment Sockets. Your item is unchanged. Please retry.',
    )
  const families = new Set<string>()
  let prefixes = 0
  let suffixes = 0
  for (const instance of next.explicits) {
    const d = definitions[instance.modifierId]!
    if (d.familyIds.some((id) => families.has(id)) || d.affixType === 'NONE')
      throw new Error(
        'Could not verify the applied item. Your item is unchanged. Please retry.',
      )
    d.familyIds.forEach((id) => families.add(id))
    if (d.affixType === 'PREFIX') prefixes++
    else suffixes++
  }
  const capacity = baseAffixCapacity(
    next.baseItemId,
    next.rarity,
    next.installedAugments,
  )
  const exaltSide = activeOmens.includes('Omen_of_Sinistral_Exaltation')
    ? 'PREFIX'
    : activeOmens.includes('Omen_of_Dextral_Exaltation')
      ? 'SUFFIX'
      : null
  const freeExaltSlots = exaltSide
    ? (exaltSide === 'PREFIX' ? capacity.prefixes : capacity.suffixes) -
      state.explicits.filter(
        (m) => definitions[m.modifierId]?.affixType === exaltSide,
      ).length
    : capacity.prefixes + capacity.suffixes - state.explicits.length
  const exaltAdditions = activeOmens.includes('Omen_of_Greater_Exaltation')
    ? exaltSide
      ? Math.min(2, Math.max(0, freeExaltSlots))
      : 2
    : 1
  const expectedRarity =
    baseAction === 'TRANSMUTATION'
      ? 'MAGIC'
      : baseAction === 'REGAL' ||
          action === 'ALCHEMY' ||
          essenceCandidates.length > 0
        ? 'RARE'
        : state.rarity
  const expectedCount =
    action === 'ALCHEMY'
      ? 4
      : state.explicits.length +
        (baseAction === 'ANNULMENT'
          ? activeOmens.includes('Omen_of_Greater_Annulment')
            ? -2
            : -1
          : baseAction === 'CHAOS' ||
              action === 'DIVINE' ||
              action === 'ARTIFICER' ||
              isEquipmentQualityAction(action) ||
              isQualityInfuser(action) ||
              action === 'FRACTURING' ||
              catalystActionType(action) !== null ||
              replacementTargets.length > 0
            ? 0
            : baseAction === 'EXALTED' &&
                activeOmens.includes('Omen_of_Greater_Exaltation')
              ? exaltAdditions
              : 1)
  if (
    (isWorkbenchJewel(next.baseItemId)
      ? !reviewedBasicJewel(next)
      : prefixes > capacity.prefixes || suffixes > capacity.suffixes) ||
    (v.applied &&
      (next.rarity !== expectedRarity ||
        next.explicits.length !== expectedCount)) ||
    (!v.applied &&
      (!sameModifiers(next.explicits, state.explicits) ||
        next.rarity !== state.rarity ||
        !sameModifiers(next.implicits, state.implicits) ||
        v.events.length !== 0 ||
        v.assumptions.length !== 0))
  )
    throw new Error(
      'Could not verify the applied item. Your item is unchanged. Please retry.',
    )
  if (
    action === 'DIVINE' &&
    (v.events.some((event) =>
      activeOmens.includes('Omen_of_the_Blessed')
        ? event.kind === 'REROLL_EXPLICIT'
        : event.kind === 'REROLL_IMPLICIT',
    ) ||
      next.explicits.map((m) => m.modifierId).join('|') !==
        state.explicits.map((m) => m.modifierId).join('|') ||
      (activeOmens.includes('Omen_of_the_Blessed') &&
        !sameModifiers(next.explicits, state.explicits)))
  )
    throw new Error(
      'Could not verify the applied item. Your item is unchanged. Please retry.',
    )
  const locks = next.explicits.filter((m) => m.fractured)
  if (
    v.applied &&
    baseAction === 'CHAOS' &&
    !preservesChaosJewelOverflow(state, next, definitions)
  )
    throw new Error(
      'Could not verify the preserved Jewel modifiers. Your item is unchanged. Please retry.',
    )
  if (v.applied && replacementTargets.length > 0) {
    const matching = workbenchOmens.filter(
      (o) =>
        !action.endsWith('_ALLOY') &&
        !action.includes('LIQUID_') &&
        o.trigger === 'ESSENCE_HYSTERIA' &&
        activeOmens.includes(o.id),
    )
    const side =
      matching[0]?.id === 'Omen_of_Sinistral_Crystallisation'
        ? 'PREFIX'
        : matching[0]?.id === 'Omen_of_Dextral_Crystallisation'
          ? 'SUFFIX'
          : null
    const candidates = state.explicits.filter(
      (m) =>
        !m.fractured &&
        (!side || definitions[m.modifierId]?.affixType === side) &&
        (!action.includes('LIQUID_') ||
          replacementTargets.some((id) => {
            const target = definitions[id]
            if (!target) return false
            const rest = state.explicits
              .filter((old) => old.modifierId !== m.modifierId)
              .map((old) => definitions[old.modifierId]!)
            return (
              rest.filter((d) => d.affixType === target.affixType).length <
                jewelCapacity(state, target.affixType) &&
              rest.every(
                (d) => !d.familyIds.some((f) => target.familyIds.includes(f)),
              )
            )
          })),
    )
    const removed = v.events[0]
    const validTargets = action.includes('LIQUID_')
      ? replacementTargets.filter((id) => {
          const target = definitions[id]!
          const restState = {
            ...state,
            explicits: state.explicits.filter(
              (m) => m.modifierId !== removed?.modifierId,
            ),
          }
          const rest = restState.explicits.map(
            (m) => definitions[m.modifierId]!,
          )
          return (
            rest.filter((d) => d.affixType === target.affixType).length <
              jewelCapacity(restState, target.affixType) &&
            rest.every(
              (d) => !d.familyIds.some((f) => target.familyIds.includes(f)),
            )
          )
        })
      : replacementTargets
    const added = v.events[1]
    const old = candidates.find((m) => m.modifierId === removed?.modifierId)
    const target = next.explicits.find(
      (m) => m.modifierId === added?.modifierId,
    )
    const preserved = state.explicits.filter(
      (m) => m.modifierId !== old?.modifierId,
    )
    if (
      state.rarity !== 'RARE' ||
      v.events.length !== 2 ||
      removed?.kind !== 'REMOVE' ||
      !old ||
      removed.selectionProbability !== 1 / candidates.length ||
      Object.keys(removed.values).length !== 0 ||
      added?.kind !== 'ADD' ||
      !validTargets.includes(added.modifierId) ||
      added.selectionProbability !== 1 / validTargets.length ||
      !target ||
      !sameValues(added.values, target.values) ||
      matching.length > 1 ||
      v.consumedOmens.length !== matching.length ||
      !matching.every((o) => v.consumedOmens.includes(o.id)) ||
      !sameModifiers(
        preserved,
        next.explicits.filter((m) =>
          preserved.some((p) => p.modifierId === m.modifierId),
        ),
      ) ||
      (validTargets.length > 1 &&
        !v.assumptions.some(
          (a) =>
            a.id ===
              (action.includes('LIQUID_')
                ? 'uniform-liquid-outcomes-v1'
                : 'uniform-essence-choice-v1') &&
            a.n === validTargets.length &&
            a.candidates.length === validTargets.length &&
            new Set(a.candidates).size === validTargets.length &&
            a.candidates.every((id) => validTargets.includes(id)),
        )) ||
      !v.assumptions.some(
        (a) =>
          a.id === 'uniform-removal-v1' &&
          a.n === candidates.length &&
          a.candidates.length === candidates.length &&
          new Set(a.candidates).size === candidates.length &&
          a.candidates.every((id) =>
            candidates.some((m) => m.modifierId === id),
          ),
      )
    )
      throw new Error(
        'Could not verify the essence replacement. Your item is unchanged. Please retry.',
      )
    if (equipmentTargets) {
      const expected = equipmentCurrencies.definitions.find(
        (d) => d.id === added?.modifierId,
      )
      const range = expected?.stats.length === 1 ? expected.stats[0] : undefined
      if (range) {
        const models = v.assumptions.filter((a) =>
          [
            'uniform-integer-roll-v1',
            'assumed-source-integer-roll-v1',
          ].includes(a.id),
        )
        if (
          models.length !== (range.min === range.max ? 0 : 1) ||
          models.some(
            (a) =>
              a.candidateUnit !== range.id ||
              a.min !== range.min ||
              a.max !== range.max ||
              a.n !== range.max - range.min + 1 ||
              a.candidates.length !== 0 ||
              a.sourceUrl !== expected!.sourceUrl,
          )
        )
          throw new Error(
            'Could not verify the equipment modifier roll model. Your item is unchanged. Please retry.',
          )
      }
    }
    if (
      [
        'PERFECT_ESSENCE_MIND',
        'PERFECT_ESSENCE_THAWING',
        'PERFECT_ESSENCE_INSULATION',
        'PERFECT_ESSENCE_COMMAND',
        'PERFECT_ESSENCE_BODY',
        'PERFECT_ESSENCE_RUIN',
        'PERFECT_ESSENCE_SEEKING',
        'PERFECT_ESSENCE_SORCERY',
        'PERFECT_ESSENCE_ALACRITY',
        'PERFECT_ESSENCE_ABRASION',
        'PERFECT_ESSENCE_FLAMES',
        'PERFECT_ESSENCE_ICE',
        'PERFECT_ESSENCE_ELECTRICITY',
        'PERFECT_ESSENCE_BATTLE',
        'PERFECT_ESSENCE_HASTE',
        'PERFECT_ESSENCE_GROUNDING',
        'PERFECT_ESSENCE_OPULENCE',
        'EXPANSIVE_ALLOY',
        'CYCLONIC_ALLOY',
        'MYSTIC_ALLOY',
        'ADAPTIVE_ALLOY',
        'SWIFT_ALLOY',
        'SOVEREIGN_ALLOY',
        'PRISMATIC_ALLOY',
      ].includes(action) &&
      (!equipmentTargets || added?.modifierId.startsWith('stocky-mitts:'))
    ) {
      const range = definitions[added!.modifierId]?.stats?.[0]
      const expectedSource =
        'https://poe2db.tw/us/hover?s=Data%5CMods%2F' +
        (action === 'PERFECT_ESSENCE_MIND'
          ? 'EssenceIncreasedManaPercent1'
          : action === 'PERFECT_ESSENCE_THAWING'
            ? 'EssenceColdRecoupLife1'
            : action === 'PERFECT_ESSENCE_INSULATION'
              ? 'EssenceFireRecoupLife1'
              : action === 'PERFECT_ESSENCE_COMMAND'
                ? 'EssenceAuraEffect1'
                : action === 'PERFECT_ESSENCE_BODY'
                  ? 'EssenceIncreasedLifePercent1'
                  : action === 'PERFECT_ESSENCE_RUIN'
                    ? 'EssencePhysicalDamageTakenAsChaos1'
                    : action === 'PERFECT_ESSENCE_SEEKING'
                      ? 'EssenceReducedCriticalDamageAgainstYou1'
                      : action === 'PERFECT_ESSENCE_SORCERY'
                        ? 'EssenceSpellSkillLevel1H1'
                        : action === 'PERFECT_ESSENCE_ALACRITY'
                          ? 'EssenceManaCostReduction'
                          : action === 'PERFECT_ESSENCE_ABRASION'
                            ? 'EssenceDamageasExtraPhysical1'
                            : action === 'PERFECT_ESSENCE_FLAMES'
                              ? 'EssenceDamageasExtraFire1'
                              : action === 'PERFECT_ESSENCE_ICE'
                                ? 'EssenceDamageasExtraCold1'
                                : action === 'PERFECT_ESSENCE_ELECTRICITY'
                                  ? 'EssenceDamageasExtraLightning1'
                                  : action === 'PERFECT_ESSENCE_BATTLE'
                                    ? 'EssenceAttackSkillLevel1H1'
                                    : action === 'PERFECT_ESSENCE_HASTE'
                                      ? 'EssenceOnslaughtonKill1'
                                      : action === 'PERFECT_ESSENCE_GROUNDING'
                                        ? 'EssenceLightningRecoupLife1'
                                        : action === 'PERFECT_ESSENCE_OPULENCE'
                                          ? 'EssenceGoldDropped1'
                                          : action === 'ADAPTIVE_ALLOY'
                                            ? 'AlloyAttackSpeedIfMissingWardRecently1'
                                            : action === 'SWIFT_ALLOY'
                                              ? 'AlloyCastSpeedGloves1'
                                              : action === 'SOVEREIGN_ALLOY'
                                                ? 'AlloyLocalWardIncreasePercent1'
                                                : action === 'EXPANSIVE_ALLOY'
                                                  ? 'AlloyRemnantPickupRange1'
                                                  : action === 'CYCLONIC_ALLOY'
                                                    ? 'AlloyDamagingAilmentDuration1'
                                                    : action === 'MYSTIC_ALLOY'
                                                      ? 'AlloyAttackAreaOfEffect1'
                                                      : 'AlloyElementalPenetration1')
      const models = v.assumptions.filter(
        (a) => a.id === 'assumed-source-integer-roll-v1',
      )
      if (
        !range ||
        models.length !== (range.min === range.max ? 0 : 1) ||
        !models.every(
          (a) =>
            a.candidateUnit === range.id &&
            a.min === range.min &&
            a.max === range.max &&
            a.n === range.max - range.min + 1 &&
            a.candidates.length === 0 &&
            a.sourceUrl === expectedSource &&
            a.reason.includes('UNVERIFIED'),
        )
      ) {
        throw new Error(
          'Could not verify the numeric model. Your item is unchanged. Please retry.',
        )
      }
    }
  }
  if (
    v.applied &&
    essenceCandidates.length > 0 &&
    (state.rarity !== 'MAGIC' ||
      v.events.length !== 1 ||
      v.events[0]?.kind !== 'ADD' ||
      !essenceCandidates.includes(v.events[0]?.modifierId ?? '') ||
      v.events[0]?.selectionProbability !== 1 / essenceCandidates.length ||
      (essenceCandidates.length > 1 &&
        !v.assumptions.some(
          (a) =>
            a.id === 'uniform-essence-choice-v1' &&
            a.n === essenceCandidates.length &&
            a.candidates.length === essenceCandidates.length &&
            new Set(a.candidates).size === essenceCandidates.length &&
            a.candidates.every((id) => essenceCandidates.includes(id)),
        )) ||
      v.consumedOmens.length !== 0 ||
      !sameModifiers(
        state.explicits
          .map((old) =>
            next.explicits.find((m) => m.modifierId === old.modifierId)!,
          )
          .filter(Boolean),
        state.explicits,
      ) ||
      !sameValues(
        v.events[0]?.values,
        next.explicits.find((m) => m.modifierId === v.events[0]?.modifierId)
          ?.values,
      ))
  )
    throw new Error(
      'Could not verify the guaranteed essence modifier. Your item is unchanged. Please retry.',
    )
  if (v.applied && sceptreFixedEssenceModifiers[action]) {
    const target = definitions[sceptreFixedEssenceModifiers[action]!]
    if (
      !target ||
      state.itemLevel <
        {
          LESSER_ESSENCE_COMMAND: 8,
          ESSENCE_COMMAND: 33,
          GREATER_ESSENCE_COMMAND: 60,
        }[
          action as
            | 'LESSER_ESSENCE_COMMAND'
            | 'ESSENCE_COMMAND'
            | 'GREATER_ESSENCE_COMMAND'
        ] ||
      next.rarity !== 'RARE'
    )
      throw new Error(
        'Could not verify the guaranteed essence modifier. Your item is unchanged. Please retry.',
      )
    const code = {
      LESSER_ESSENCE_COMMAND: 'NearbyAlliesAllDamage2',
      ESSENCE_COMMAND: 'NearbyAlliesAllDamage4',
      GREATER_ESSENCE_COMMAND: 'NearbyAlliesAllDamage6',
    }[
      action as
        'LESSER_ESSENCE_COMMAND' | 'ESSENCE_COMMAND' | 'GREATER_ESSENCE_COMMAND'
    ]
    const range = target.stats?.[0]
    const models = v.assumptions.filter(
      (a) => a.id === 'assumed-source-integer-roll-v1',
    )
    if (
      !range ||
      target.stats?.length !== 1 ||
      models.length !== 1 ||
      !models.every(
        (a) =>
          a.candidateUnit === range.id &&
          a.min === range.min &&
          a.max === range.max &&
          a.n === range.max - range.min + 1 &&
          a.candidates.length === 0 &&
          a.sourceUrl ===
            'https://poe2db.tw/us/hover?s=Data%5CMods%2F' + code &&
          a.reason.includes('UNVERIFIED'),
      )
    )
      throw new Error(
        'Could not verify the numeric model. Your item is unchanged. Please retry.',
      )
  }
  if (
    v.applied &&
    baseAction === 'EXALTED' &&
    activeOmens.includes('Omen_of_Greater_Exaltation') &&
    (state.rarity !== 'RARE' ||
      exaltAdditions < 1 ||
      freeExaltSlots < exaltAdditions ||
      !sameModifiers(
        state.explicits
          .map((old) =>
            next.explicits.find((m) => m.modifierId === old.modifierId)!,
          )
          .filter(Boolean),
        state.explicits,
      ) ||
      v.events.length !== exaltAdditions ||
      new Set(v.events.map((event) => event.modifierId)).size !==
        exaltAdditions ||
      v.events.some(
        (event) =>
          event.kind !== 'ADD' ||
          (exaltSide !== null &&
            definitions[event.modifierId]?.affixType !== exaltSide) ||
          state.explicits.some((old) => old.modifierId === event.modifierId) ||
          !sameValues(
            event.values,
            next.explicits.find((m) => m.modifierId === event.modifierId)
              ?.values,
          ),
      ) ||
      v.consumedOmens.includes('Omen_of_Greater_Exaltation') !==
        (exaltAdditions === 2) ||
      (exaltSide !== null &&
        !v.consumedOmens.includes(
          exaltSide === 'PREFIX'
            ? 'Omen_of_Sinistral_Exaltation'
            : 'Omen_of_Dextral_Exaltation',
        )))
  )
    throw new Error(
      'Could not verify both added modifiers. Your item is unchanged. Please retry.',
    )
  if (
    v.applied &&
    baseAction === 'CHAOS' &&
    activeOmens.includes('Omen_of_Whittling')
  ) {
    const candidates = whittlingCandidates(state, definitions, activeOmens)
    const matching = workbenchOmens.filter(
      (o) => o.trigger === 'CHAOS' && activeOmens.includes(o.id),
    )
    const removed = v.events.filter((e) => e.kind === 'REMOVE')
    const added = v.events.filter((e) => e.kind === 'ADD')
    const removal = removed[0]
    const model = v.assumptions.filter((a) => a.id === 'uniform-removal-v1')
    if (
      candidates.length === 0 ||
      matching.length > 2 ||
      (matching.length === 2 &&
        !compatibleOmenPair(matching[0]!.id, matching[1]!.id)) ||
      v.consumedOmens.length !== matching.length ||
      !matching.every((o) => v.consumedOmens.includes(o.id)) ||
      v.events.length !== 2 ||
      removed.length !== 1 ||
      added.length !== 1 ||
      !removal ||
      !candidates.includes(removal.modifierId) ||
      Object.keys(removal.values).length !== 0 ||
      removal.selectionProbability !== 1 / candidates.length ||
      model.length !== 1 ||
      model[0]!.n !== candidates.length ||
      model[0]!.candidates.length !== candidates.length ||
      new Set(model[0]!.candidates).size !== candidates.length ||
      !model[0]!.candidates.every((id) => candidates.includes(id)) ||
      !sameModifiers(
        state.explicits.filter((m) => m.modifierId !== removal.modifierId),
        next.explicits.filter((m) => m.modifierId !== added[0]!.modifierId),
      ) ||
      !sameValues(
        added[0]!.values,
        next.explicits.find((m) => m.modifierId === added[0]!.modifierId)
          ?.values,
      )
    )
      throw new Error(
        'Could not verify the Whittling removal candidates. Your item is unchanged. Please retry.',
      )
  }
  if (
    v.applied &&
    action === 'ANNULMENT' &&
    activeOmens.includes('Omen_of_Light')
  ) {
    const removed = v.events.filter((event) => event.kind === 'REMOVE')
    const marked = state.desecrated?.map((slot) => slot.modifierId) ?? []
    if (
      removed.length !== 1 ||
      !marked.includes(removed[0]!.modifierId) ||
      v.events.length !== 1 ||
      v.consumedOmens.length !== 1 ||
      v.consumedOmens[0] !== 'Omen_of_Light' ||
      !sameModifiers(
        state.explicits.filter((m) => m.modifierId !== removed[0]!.modifierId),
        next.explicits,
      )
    )
      throw new Error('Could not verify the Desecrated modifier removal.')
  }
  const legacyFive = legacyFiveIds.filter(
    (id) =>
      activeOmens.includes(id) &&
      (id.endsWith('Alchemy')
        ? action === 'ALCHEMY'
        : id.endsWith('Coronation')
          ? baseAction === 'REGAL'
          : baseAction === 'ANNULMENT'),
  )
  if (v.applied && legacyFive.length > 0) {
    const id = legacyFive[0]!
    const trigger = id.endsWith('Alchemy')
      ? 'ALCHEMY'
      : id.endsWith('Coronation')
        ? 'REGAL'
        : 'ANNULMENT'
    const matches = workbenchOmens.filter(
      (o) => o.trigger === trigger && activeOmens.includes(o.id),
    )
    const side = id.includes('Sinistral') ? 'PREFIX' : 'SUFFIX'
    const added = v.events.filter((e) => e.kind === 'ADD')
    const removed = v.events.filter((e) => e.kind === 'REMOVE')
    let valid =
      baseAction === trigger &&
      (matches.length === 1 ||
        (trigger === 'ANNULMENT' &&
          matches.length === 2 &&
          matches.some(
            (o) =>
              o.id === 'Omen_of_Sinistral_Annulment' ||
              o.id === 'Omen_of_Dextral_Annulment',
          ))) &&
      v.consumedOmens.length === matches.length &&
      matches.every((o) => v.consumedOmens.includes(o.id))
    if (trigger === 'ALCHEMY') {
      valid &&=
        !state.explicits.some((m) => m.fractured) &&
        added.length === 4 &&
        removed.length === state.explicits.length &&
        next.explicits.filter(
          (m) => definitions[m.modifierId]?.affixType === side,
        ).length === (isWorkbenchJewel(state.baseItemId) ? 2 : 3)
    } else if (trigger === 'REGAL') {
      valid &&=
        added.length === 1 &&
        removed.length === 0 &&
        definitions[added[0]!.modifierId]?.affixType === side &&
        sameModifiers(
          state.explicits,
          next.explicits.filter((m) =>
            state.explicits.some((old) => old.modifierId === m.modifierId),
          ),
        )
    } else {
      const candidates = state.explicits.filter(
        (m) =>
          !m.fractured &&
          (matches.length === 1 ||
            definitions[m.modifierId]?.affixType ===
              (matches.some((o) => o.id === 'Omen_of_Sinistral_Annulment')
                ? 'PREFIX'
                : 'SUFFIX')),
      )
      valid &&=
        candidates.length >= 2 &&
        added.length === 0 &&
        removed.length === 2 &&
        new Set(removed.map((e) => e.modifierId)).size === 2 &&
        removed.every(
          (e, i) =>
            candidates.some((m) => m.modifierId === e.modifierId) &&
            Object.keys(e.values).length === 0 &&
            e.selectionProbability === 1 / (candidates.length - i),
        ) &&
        sameModifiers(
          state.explicits.filter(
            (m) => !removed.some((e) => e.modifierId === m.modifierId),
          ),
          next.explicits,
        )
    }
    if (!valid)
      throw new Error(
        'Could not verify the legacy omen result. Your item is unchanged. Please retry.',
      )
  }
  const homogenising = legacyHomogenisingIds.find(
    (id) =>
      activeOmens.includes(id) &&
      baseAction === (id.endsWith('Coronation') ? 'REGAL' : 'EXALTED'),
  )
  if (v.applied && homogenising) {
    const matching = workbenchOmens.filter(
      (o) => o.trigger === baseAction && activeOmens.includes(o.id),
    )
    const originalTags = new Set(
      [...state.implicits, ...state.explicits].flatMap(
        (m) => definitions[m.modifierId]?.tags ?? [],
      ),
    )
    let intermediate = [...state.explicits]
    const valid =
      matching.every(
        (o) => o.id === homogenising || compatibleOmenPair(o.id, homogenising),
      ) &&
      v.consumedOmens.length === matching.length &&
      matching.every((o) => v.consumedOmens.includes(o.id)) &&
      v.events.length === expectedCount - state.explicits.length &&
      sameModifiers(
        state.explicits,
        next.explicits.filter((m) =>
          state.explicits.some((old) => old.modifierId === m.modifierId),
        ),
      ) &&
      v.events.every((event) => {
        const existingFamilies = new Set(
          intermediate.flatMap((m) => definitions[m.modifierId]!.familyIds),
        )
        let pool = Object.values(definitions).filter(
          (d) =>
            d.layer === 'EXPLICIT' &&
            (d.weight ?? 0) > 0 &&
            // requiredItemLevel is preserved by the initial API definition contract.
            d.requiredItemLevel != null &&
            d.requiredItemLevel <= state.itemLevel &&
            !d.familyIds.some((family) => existingFamilies.has(family)) &&
            intermediate.filter(
              (m) => definitions[m.modifierId]!.affixType === d.affixType,
            ).length <
              (isWorkbenchJewel(state.baseItemId)
                ? jewelCapacity(
                    { ...state, explicits: intermediate },
                    d.affixType,
                  )
                : 3) &&
            (originalTags.size === 0 ||
              d.tags?.some((tag) => originalTags.has(tag))),
        )
        const minimum = action.startsWith('GREATER_')
          ? 35
          : action.startsWith('PERFECT_')
            ? 50
            : 0
        if (minimum > 0) {
          const highest = new Map<string, number>()
          for (const d of pool) {
            if (d.familyIds.length !== 1) return false
            const group = `${d.affixType}:${d.familyIds[0]}`
            highest.set(
              group,
              Math.max(highest.get(group) ?? 0, d.requiredItemLevel!),
            )
          }
          pool = pool.filter(
            (d) =>
              d.requiredItemLevel! >= minimum ||
              d.requiredItemLevel ===
                highest.get(`${d.affixType}:${d.familyIds[0]}`),
          )
        }
        const chosen = pool.find((d) => d.id === event.modifierId)
        const added = next.explicits.find(
          (m) => m.modifierId === event.modifierId,
        )
        if (
          !chosen ||
          !added ||
          event.kind !== 'ADD' ||
          !sameValues(event.values, added.values) ||
          event.selectionProbability !==
            chosen.weight! / pool.reduce((sum, d) => sum + d.weight!, 0)
        )
          return false
        intermediate = [...intermediate, added]
        return true
      })
    if (!valid)
      throw new Error(
        'Could not verify the legacy tag-restricted additions. Your item is unchanged. Please retry.',
      )
  }
  const previousLocks = state.explicits.filter((m) => m.fractured)
  if (
    locks.length > 1 ||
    (locks.length && next.rarity !== 'RARE') ||
    previousLocks.some(
      (locked) =>
        !next.explicits.some(
          (m) =>
            m.modifierId === locked.modifierId &&
            m.fractured &&
            sameValues(m.values, locked.values),
        ),
    ) ||
    (v.applied &&
      action === 'FRACTURING' &&
      (locks.length !== 1 ||
        previousLocks.length !== 0 ||
        state.rarity !== 'RARE' ||
        state.explicits.length < 4 ||
        next.explicits.some(
          (m, i) =>
            m.modifierId !== state.explicits[i]?.modifierId ||
            !sameValues(m.values, state.explicits[i]?.values),
        ) ||
        v.events.length !== 1 ||
        v.events[0]?.kind !== 'FRACTURE' ||
        v.events[0]?.modifierId !== locks[0]?.modifierId ||
        v.events[0]?.selectionProbability !== 1 / state.explicits.length)) ||
    (action !== 'FRACTURING' && locks.length !== previousLocks.length)
  )
    throw new Error(
      'Could not verify the Fractured modifier. Your item is unchanged. Please retry.',
    )
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
