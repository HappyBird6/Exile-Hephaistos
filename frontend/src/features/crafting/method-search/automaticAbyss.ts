import type { AbyssState } from '../AbyssPanel'
import { assertShape } from '../path-tree/validation'

export type Ratio = { numerator: number; denominator: number }
export type ConditionalState = {
  item: AbyssState
  ranges: Record<string, { stat: string; min: number; max: number }[]>
  condition: string
}
export type AutomaticAbyssRoute = {
  strategy: {
    kind: 'COMPLETE_SIDE' | 'BLOCK_HIGH_WEIGHT' | 'SHARE_REMOVAL_RISK'
    modifierSelection: null | {
      baseline: { targetWeight: number; totalWeight: number }
      afterBlocking: { targetWeight: number; totalWeight: number }
      blockerModifierId: string
    }
  }
  prepared: ConditionalState
  assumed: ConditionalState | null
  status: 'CONDITIONAL_FRONTIER'
  transitions: {
    source: ConditionalState
    currency: string
    omens: string[]
    removals: {
      slotId: string | null
      probability: Ratio
      remaining: ConditionalState
      candidates: {
        modifierId: string
        selection: Ratio
        result: ConditionalState
        next: {
          currency: string
          omens: string[]
          removalBranches: number
          candidateCount: number
        }[]
      }[]
    }[]
  }[]
}

export const mass = (p: Ratio) => p.numerator / p.denominator
export function validateAutomaticAbyss(
  routes: AutomaticAbyssRoute[] | undefined,
) {
  if (routes === undefined) return
  const fail = () => {
    throw new Error('Invalid automatic conditional state')
  }
  const ratio = (p: Ratio) => {
    if (
      !p ||
      !Number.isSafeInteger(p.numerator) ||
      !Number.isSafeInteger(p.denominator) ||
      p.denominator <= 0 ||
      p.numerator < 0 ||
      p.numerator > p.denominator
    )
      fail()
  }
  const state = (s: ConditionalState) => {
    if (
      !s ||
      !s.item ||
      !Array.isArray(s.item.slots) ||
      s.item.slots.length > 6 ||
      !s.ranges ||
      !['UNREVEALED_SLOT', 'IF_REVEAL_CHOOSES_MODIFIER'].includes(s.condition)
    )
      fail()
    assertShape('item', s.item.item)
    const slots = s.item.slots
    if (
      s.item.item.explicits.length ||
      s.item.offer !== null ||
      new Set(slots.map((v) => v.id)).size !== slots.length
    )
      fail()
    for (const side of ['PREFIX', 'SUFFIX'])
      if (slots.filter((v) => v.side === side).length > 3) fail()
    for (const slot of slots) {
      if (
        !slot.id ||
        !['PREFIX', 'SUFFIX'].includes(slot.side) ||
        typeof slot.desecrated !== 'boolean' ||
        (!slot.modifier && !slot.desecrated) ||
        (slot.desecrated && slot.modifier?.fractured)
      )
        fail()
    }
    assertShape('item', {
      ...s.item.item,
      explicits: slots.flatMap((v) => (v.modifier ? [v.modifier] : [])),
    })
    for (const [id, ranges] of Object.entries(s.ranges)) {
      if (
        !slots.some((v) => v.modifier?.modifierId === id) ||
        !Array.isArray(ranges)
      )
        fail()
      for (const r of ranges)
        if (
          !r.stat ||
          !Number.isSafeInteger(r.min) ||
          !Number.isSafeInteger(r.max) ||
          r.min > r.max
        )
          fail()
    }
  }
  if (!Array.isArray(routes)) fail()
  for (const route of routes) {
    if (
      route.status !== 'CONDITIONAL_FRONTIER' ||
      !['COMPLETE_SIDE', 'BLOCK_HIGH_WEIGHT', 'SHARE_REMOVAL_RISK'].includes(
        route.strategy.kind,
      )
    )
      fail()
    state(route.prepared)
    if (route.assumed) state(route.assumed)
    for (const transition of route.transitions) {
      state(transition.source)
      for (const removal of transition.removals) {
        ratio(removal.probability)
        state(removal.remaining)
        for (const c of removal.candidates) {
          ratio(c.selection)
          state(c.result)
        }
        if (
          removal.candidates.length &&
          Math.abs(
            removal.candidates.reduce((sum, c) => sum + mass(c.selection), 0) -
              1,
          ) > 1e-9
        )
          fail()
        const locked = transition.source.item.slots.filter(
          (v) => v.modifier?.fractured,
        )
        for (const result of [
          removal.remaining,
          ...removal.candidates.map((c) => c.result),
        ]) {
          if (
            locked.some(
              (v) =>
                !result.item.slots.some(
                  (n) => JSON.stringify(n) === JSON.stringify(v),
                ),
            )
          )
            fail()
        }
      }
      if (
        transition.removals.length &&
        Math.abs(
          transition.removals.reduce((sum, r) => sum + mass(r.probability), 0) -
            1,
        ) > 1e-9
      )
        fail()
    }
  }
}
