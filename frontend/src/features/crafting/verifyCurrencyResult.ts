import { isQualityInfuser, infuserTransitionMatches } from './qualityInfuser'
import { isCraftedModifier } from './augmentRules'
import { equivalent } from './sanctification/equivalence'
import {
  sanctificationTransitionMatches,
} from './sanctification/sanctification'
import {
  isEquipmentQualityAction,
  qualityTransitionMatches,
} from './qualityCurrency'
import { sameObservations } from './itemObservations'
import { topBase, topBaseKey } from './topBases'
import { basePolicy, baseAffixCapacity } from './baseRegistry'
import { materialTargetMatches } from './materialRules'
import { supportsConcreteStateShape } from './workbenchStateShape'
import {
  isWorkbenchJewel,
  reviewedBasicJewel,
  jewelCapacity,
} from './basicJewel'
import { qualityCapChangeMatches } from './qualityCapChangePolicy'
import { whittlingCandidates } from './omenRemovalCandidates'
import { preservesChaosJewelOverflow } from './jewelChaos'
import { verifiedCatalystQuality } from './catalystQuality'
import { maximumQuality, qualityLimitMatches } from './qualityLimit'
import type { Definition } from './craftingApi'
import {
  catalystActionType,
  workbenchOmens,
  legacyHomogenisingIds,
  legacyFiveIds,
  compatibleOmenPair,
  baseWorkbenchAction,
} from './currencyDefinitions'
import type { WorkbenchAction } from './currencyDefinitions'
import type { AppliedItem, ConcreteItem } from './workbenchApi'
import { sameValues, sameModifiers } from './currencyResultEquality'
import {
  currencyMaterial,
  verifyMaterialTransition,
} from './verifyMaterialTransition'

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
export function verifyCurrencyResult(
  state: ConcreteItem,
  action: WorkbenchAction,
  definitions: Record<string, Definition>,
  activeOmens: string[],
  v: AppliedItem,
) {
  const next = v?.state
  const baseAction = baseWorkbenchAction(action)
  const reviewedKey = topBaseKey(state.baseItemId)
  const context = currencyMaterial(state, action)
  const { material, catalogMaterials, rule, targets, replacementTargets } = context
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
      material?.kind === 'LIQUID' &&
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
      material &&
      (!rule ||
        state.itemLevel < rule.level ||
        !material.inputRarities.includes(state.rarity) ||
        !targets.every((id) =>
          materialTargetMatches(definitions[id], catalogMaterials?.details[id]),
        ))) ||
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
      : baseAction === 'REGAL' || action === 'ALCHEMY'
        ? 'RARE'
        : (material?.resultRarity ?? state.rarity)
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
  verifyMaterialTransition(state, v, definitions, activeOmens, context)
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
}
