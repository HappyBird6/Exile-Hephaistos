import { baseAffixCapacity } from '../baseRegistry'
import { isBone, necromancySide } from '../desecrationCurrency'
import type { Initial } from '../craftingApi'
import type { ConcreteItem } from '../workbenchApi'
import type { Item } from '../path-tree/types'
import { canonical } from '../path-tree/validation'
import type {
  AcquisitionResult,
  AcquisitionRoute,
  AcquisitionStep,
} from './acquisitionRouteApi'

type StartingState = Item | ConcreteItem
type Definitions = Initial['modifiers']

// Only commute an adjacent, verified slot reservation and a single additive Exalt.
// The reserved side must still have room to roll from the same sides in either order.
function independentPair(
  a: AcquisitionStep,
  b: AcquisitionStep,
  before: StartingState,
  mid: ConcreteItem,
  end: ConcreteItem,
  defs: Definitions,
) {
  const boneFirst = isBone(a.action)
  const bone = boneFirst ? a : b
  const exalt = boneFirst ? b : a
  if (
    !isBone(bone.action) ||
    !['EXALTED', 'PERFECT_EXALTED', 'GREATER_EXALTED'].includes(exalt.action) ||
    exalt.acquired.length !== 1 ||
    exalt.dummy ||
    exalt.omens.some(
      (omen) =>
        ![
          'Omen_of_Sinistral_Exaltation',
          'Omen_of_Dextral_Exaltation',
        ].includes(omen),
    ) ||
    exalt.chance?.probability == null ||
    bone.chance?.probability !== 1 ||
    bone.acquired.length ||
    before.desecrated?.length
  )
    return false
  const side = necromancySide(bone.omens)
  if (!side || before.explicits.some((m) => !defs[m.modifierId])) return false
  const capacity = baseAffixCapacity(
    before.baseItemId,
    before.rarity,
    'installedAugments' in before ? before.installedAugments : undefined,
  )
  const occupied = before.explicits.filter(
    (m) => defs[m.modifierId]?.affixType === side,
  ).length
  if (
    (side === 'PREFIX' ? capacity.prefixes : capacity.suffixes) - occupied <
    2
  )
    return false
  const hidden = end.desecrated
  if (
    hidden?.length !== 1 ||
    hidden[0]?.side !== side ||
    hidden[0].modifierId !== null
  )
    return false
  const withSlot = (state: StartingState) => ({ ...state, desecrated: hidden })
  const exalted = boneFirst ? end : mid
  const added = exalted.explicits.filter(
    (m) => !before.explicits.some((old) => old.modifierId === m.modifierId),
  )
  if (added.length !== 1) return false
  const withoutAddition = {
    ...exalted,
    explicits: exalted.explicits.filter((m) => m !== added[0]),
  }
  return boneFirst
    ? canonical(withSlot(before)) === canonical(mid) &&
        canonical(withoutAddition) === canonical(mid)
    : canonical(withoutAddition) === canonical(before) &&
        canonical(withSlot(mid)) === canonical(end)
}

// This is a display key. Navigation, probability, cost and replay retain the original route index.
export function acquisitionGroupKey(
  result: AcquisitionResult,
  route: AcquisitionRoute,
  start: StartingState,
  defs: Definitions,
  commute = true,
): string | null {
  const card = result.cards[route.cardIndex]
  if (
    !card ||
    route.probability == null ||
    !route.steps.length ||
    route.steps.some(
      (s) => s.lost.length || s.omenOutcomes || s.chance?.probabilityBounds,
    ) ||
    route.stateIndices.length !== route.steps.length ||
    new Set(route.order).size !== route.order.length
  )
    return null
  const states = route.stateIndices.map((i) => result.states[i])
  const final = states.at(-1)
  if (!final || states.some((s) => !s)) return null
  const positions = new Map(route.order.map((n, i) => [n, i]))
  const targets = new Map<string, number>()
  for (const option of card.options) {
    const position = positions.get(option.number)
    if (position === undefined) continue
    for (const id of option.modifierIds) {
      if (targets.has(id)) return null
      targets.set(id, position)
    }
  }
  const finalRolls = new Map(
    final.explicits.map((m) => [m.modifierId, canonical(m.values)]),
  )
  function stateKey(state: ConcreteItem) {
    return {
      ...state,
      explicits: state.explicits
        .map((m) => {
          const position = targets.get(m.modifierId)
          const d = defs[m.modifierId]
          return position == null || !d
            ? canonical(m)
            : canonical([
                position,
                d.affixType,
                d.tier,
                // A later Divine can change the observed roll within this tier.
                finalRolls.get(m.modifierId) === canonical(m.values)
                  ? null
                  : m.values,
                m.fractured ?? false,
                m.crafted ?? false,
              ])
        })
        .sort(),
      desecrated: state.desecrated?.map((s) => ({
        ...s,
        modifierId: targets.get(s.modifierId ?? '') ?? s.modifierId,
      })),
    }
  }
  function stepKey(s: AcquisitionStep) {
    return {
      // Group by the action itself; cumulative reach belongs to the ordered graph prefix.
      action: s.action,
      omens: s.omens,
      socketIndex: s.socketIndex,
      fracturedModifierId: s.fracturedModifierId,
      lost: s.lost,
      chance: s.chance,
      expectedUses: s.expectedUses,
      maxUses: s.maxUses,
      dummy: s.dummy,
      repeatUntilHit: s.repeatUntilHit,
      acquired: s.acquired.map((n) => positions.get(n)),
      repeatTarget: positions.get(s.repeatTarget ?? -1),
    }
  }
  const sequence = []
  for (let i = 0; i < route.steps.length; i++) {
    const a = route.steps[i]!
    const b = route.steps[i + 1]
    if (
      commute &&
      b &&
      independentPair(
        a,
        b,
        i ? states[i - 1]! : start,
        states[i]!,
        states[i + 1]!,
        defs,
      )
    ) {
      sequence.push([
        [stepKey(isBone(a.action) ? a : b), stepKey(isBone(a.action) ? b : a)],
        stateKey(states[++i]!),
      ])
    } else sequence.push([stepKey(a), stateKey(states[i]!)])
  }
  return canonical([
    route.cardIndex,
    route.lucky ?? false,
    route.probability,
    final.explicits.map(canonical).sort(),
    sequence,
  ])
}
