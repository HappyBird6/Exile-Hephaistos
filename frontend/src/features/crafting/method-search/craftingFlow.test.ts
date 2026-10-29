import { expect, it } from 'vitest'
import {
  candidateState,
  materializeAbyss,
  validFlow,
  abyssCard,
  type CraftingFlow,
} from './craftingFlow'
import type { AbyssState } from '../AbyssPanel'
import { requestFixture } from '../path-tree/fixtures.test-support'
import { createItemPresentation } from '../path-tree/presentation'

function state(): AbyssState {
  const item = requestFixture().start.item
  return {
    item: { ...item, rarity: 'RARE', catalystQuality: null, explicits: [] },
    offer: null,
    slots: Array.from({ length: 6 }, (_, i) => ({
      id: 'slot-' + i,
      side: i < 3 ? 'PREFIX' : 'SUFFIX',
      desecrated: i === 5,
      modifier: {
        modifierId: 'mod-' + i,
        values: { amount: i + 1 },
        fractured: i === 0,
      },
    })),
  }
}
function flow(s = state()): CraftingFlow {
  const request = requestFixture()
  return {
    origin: request.clientRequestId,
    request,
    prefix: [],
    anchor: null,
    carry: null,
    branch: {
      sourceNode: 0,
      source: s.item,
      prepared: s,
      action: 'EXALTED',
      omens: [],
      steps: [{ id: 'step', state: s, transition: { operation: 'prepare' } }],
    },
  }
}
it('never materializes an unrevealed slot or pending choice as an ordinary item', () => {
  const s = state()
  s.slots[5]!.modifier = null
  expect(materializeAbyss(s)).toBeNull()
  s.offer = {
    slotId: 'slot-5',
    candidates: [0, 1, 2].map((i) => ({
      modifierId: 'candidate-' + i,
      values: { amount: 47 + i },
      fractured: false,
    })),
  }
  expect(materializeAbyss(s)).toBeNull()
})
it('six-slot rework changes only the chosen slot and retains the fracture and side', () => {
  const s = state()
  s.offer = {
    slotId: 'slot-5',
    candidates: [0, 1, 2].map((i) => ({
      modifierId: 'candidate-' + i,
      values: { amount: 47 + i },
      fractured: false,
    })),
  }
  const next = candidateState(s, 1)
  expect(next.slots).toHaveLength(6)
  expect(next.slots.slice(0, 5)).toEqual(s.slots.slice(0, 5))
  expect(next.slots[5]).toEqual({
    ...s.slots[5],
    modifier: s.offer.candidates[1],
  })
  expect(materializeAbyss(next)?.explicits[0]?.fractured).toBe(true)
  expect(s.offer).not.toBeNull()
  expect(next.offer).toBeNull()
  expect(next).not.toHaveProperty('probability')
})
it('restores bounded flow and rejects duplicate slots, wrong side capacity and incomplete offers', () => {
  const f = flow()
  expect(validFlow(f, f.origin)).toBe(true)
  const bad = structuredClone(f)
  bad.branch!.prepared.slots[1]!.id = 'slot-0'
  expect(validFlow(bad, f.origin)).toBe(false)
  const sides = flow()
  sides.branch!.prepared.slots[3]!.side = 'PREFIX'
  expect(validFlow(sides, f.origin)).toBe(false)
  const offer = flow()
  offer.branch!.prepared.offer = { slotId: 'slot-5', candidates: [] }
  expect(validFlow(offer, f.origin)).toBe(false)
  expect(validFlow(f, 'other-input')).toBe(false)
})
it('keeps unrevealed modifiers visible and green without introducing synthetic catalog modifiers', () => {
  const s = state()
  s.slots[5]!.modifier = null
  const present = createItemPresentation(
    { name: 'Test', itemClass: 'Amulet' },
    {},
  )
  const card = abyssCard(s, present, 'en')
  expect(card.modifiers.find((m) => m.id === 'slot-5')).toMatchObject({
    kind: 'desecrated',
    affixLabel: 'S',
    text: 'Unrevealed desecrated modifier',
  })
  expect(s.item.explicits).toEqual([])
})

it('rejects corrupt source metadata and fabricated removal percentages on restore', () => {
  const f = flow()
  const malformed = structuredClone(f)
  malformed.anchor = {} as NonNullable<CraftingFlow['anchor']>
  expect(validFlow(malformed, f.origin)).toBe(false)
  f.branch!.steps[0]!.transition.removal = [
    { slotId: 'slot-0', probability: 2 },
  ]
  expect(validFlow(f, f.origin)).toBe(false)
})
