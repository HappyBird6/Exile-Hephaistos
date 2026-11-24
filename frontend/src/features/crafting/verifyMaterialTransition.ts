import { baseAffixCapacity } from './baseRegistry'
import { materialActions, materialProfiles } from './catalogData'
import { materialCatalogFor, materialRollMatches } from './materialRules'
import { jewelCapacity } from './basicJewel'
import type { Definition } from './craftingApi'
import { workbenchOmens } from './currencyDefinitions'
import type { WorkbenchAction } from './currencyDefinitions'
import type { AppliedItem, ConcreteItem } from './workbenchApi'
import { sameValues, sameModifiers } from './currencyResultEquality'

export function currencyMaterial(state: ConcreteItem, action: WorkbenchAction) {
  const material = materialProfiles[materialActions[action] ?? '']
  const catalogMaterials = materialCatalogFor(state.baseItemId)
  const rule = catalogMaterials?.targets[action]
  const targets = rule?.ids ?? []
  const essenceCandidates = material?.mode === 'ADD' ? targets : []
  const replacementTargets = material?.mode === 'REPLACE' ? targets : []
  return {
    material,
    catalogMaterials,
    rule,
    targets,
    essenceCandidates,
    replacementTargets,
  }
}

export function verifyMaterialTransition(
  state: ConcreteItem,
  v: AppliedItem,
  definitions: Record<string, Definition>,
  activeOmens: string[],
  context: ReturnType<typeof currencyMaterial>,
) {
  const next = v.state
  const { material, catalogMaterials, essenceCandidates, replacementTargets } =
    context
  if (v.applied && replacementTargets.length > 0) {
    const matching = workbenchOmens.filter(
      (o) =>
        material?.kind === 'ESSENCE' &&
        o.trigger === 'ESSENCE_HYSTERIA' &&
        activeOmens.includes(o.id),
    )
    const side =
      matching[0]?.id === 'Omen_of_Sinistral_Crystallisation'
        ? 'PREFIX'
        : matching[0]?.id === 'Omen_of_Dextral_Crystallisation'
          ? 'SUFFIX'
          : null
    const selectedTarget = definitions[v.events[1]?.modifierId ?? '']
    const targetSide = selectedTarget?.affixType
    const capacity = baseAffixCapacity(
      state.baseItemId,
      'RARE',
      state.installedAugments,
    )
    const targetFull =
      material?.kind === 'ESSENCE' &&
      (targetSide === 'PREFIX' || targetSide === 'SUFFIX') &&
      state.explicits.filter(
        (m) => definitions[m.modifierId]?.affixType === targetSide,
      ).length >=
        (targetSide === 'PREFIX' ? capacity.prefixes : capacity.suffixes)
    const candidates = state.explicits.filter(
      (m) =>
        !m.fractured &&
        (!side || definitions[m.modifierId]?.affixType === side) &&
        (!targetFull || definitions[m.modifierId]?.affixType === targetSide) &&
        (material?.kind !== 'LIQUID' ||
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
    const validTargets =
      material?.kind === 'LIQUID'
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
              (material?.kind === 'LIQUID'
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
  }
  const guaranteed = essenceCandidates.length
    ? next.explicits.find((m) => m.modifierId === v.events[0]?.modifierId)
    : undefined
  if (
    v.applied &&
    essenceCandidates.length > 0 &&
    (v.events.length !== 1 ||
      v.events[0]?.kind !== 'ADD' ||
      !guaranteed ||
      guaranteed.crafted ||
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
      !sameValues(v.events[0]?.values, guaranteed.values))
  )
    throw new Error(
      'Could not verify the guaranteed essence modifier. Your item is unchanged. Please retry.',
    )
  if (v.applied && material && catalogMaterials) {
    const added = v.events.find((event) => event.kind === 'ADD')!
    if (
      !materialRollMatches(
        definitions[added.modifierId]!,
        v.assumptions,
        catalogMaterials,
      )
    )
      throw new Error(
        'Could not verify the numeric model. Your item is unchanged. Please retry.',
      )
  }
}
