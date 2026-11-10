import { expect, it } from 'vitest'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { concreteInitial } from '../workbenchApi'
import { itemFromSlots, sameItemState, sameRevealOffer } from './itemIdentity'
import {
  abyssFromConcrete,
  resolveCraftingState,
  withSlotItemMetadata,
} from './craftingState'
import { currencyOmens } from './activeOmens'
import {
  currentFrame,
  emptyFilms,
  recordCraft,
  startFilm,
} from '../workbenchHistory'
import type { AppliedItem } from '../workbenchApi'

const suffix = { modifierId: 's', values: { strength: 6 } }
const prefix = { modifierId: 'p', values: { life: 15 } }
const item = {
  ...concreteInitial(initialFixture),
  rarity: 'RARE' as const,
  explicits: [suffix, prefix],
  desecrated: [{ id: 'revealed', side: 'SUFFIX' as const, modifierId: 's' }],
}

it('compares unordered item collections without losing rolls or desecration constraints', () => {
  const reordered = {
    ...item,
    explicits: [prefix, { ...suffix, fractured: false, crafted: false }],
    installedAugments: [],
    catalystQuality: null,
  }
  expect(sameItemState(item, reordered)).toBe(true)
  expect(
    sameItemState(item, {
      ...reordered,
      explicits: [prefix, { ...suffix, values: { strength: 7 } }],
    }),
  ).toBe(false)
  expect(
    sameItemState(item, {
      ...reordered,
      desecrated: [{ ...item.desecrated[0]!, minimumModifierLevel: 40 }],
    }),
  ).toBe(false)
  expect(
    sameItemState(item, {
      ...reordered,
      explicits: [prefix, { ...suffix, crafted: true }],
    }),
  ).toBe(false)
})
it('preserves slot order but produces canonical concrete values and rejects stale checkpoints', () => {
  const source = abyssFromConcrete(item, initialFixture)
  expect(source.slots.map((s) => s.modifier?.modifierId)).toEqual(['s', 'p'])
  expect(itemFromSlots(source).explicits.map((m) => m.modifierId)).toEqual([
    'p',
    's',
  ])
  expect(resolveCraftingState(item, initialFixture, source).abyss).toBe(source)
  expect(() =>
    resolveCraftingState(
      { ...item, explicits: [prefix] },
      initialFixture,
      source,
    ),
  ).toThrow('Inconsistent')
})
it('keeps hidden constraints and pending reveal candidates through metadata-only changes', () => {
  const hidden = {
    ...concreteInitial(initialFixture),
    rarity: 'RARE' as const,
    augmentSockets: 2,
    desecrated: [
      {
        id: 'hidden',
        side: 'PREFIX' as const,
        modifierId: null,
        minimumModifierLevel: 40 as const,
        revealPool: 'ALTERED',
      },
    ],
  }
  const source = {
    ...abyssFromConcrete(hidden, initialFixture),
    offer: { slotId: 'hidden', candidates: [prefix, prefix, prefix] },
  }
  const nextItem = {
    ...hidden,
    installedAugments: [{ slotIndex: 0, augmentId: 'Perfect_Iron_Rune' }],
  }
  const next = withSlotItemMetadata(source, nextItem)!
  expect(next.slots).toBe(source.slots)
  expect(next.offer).toBe(source.offer)
  expect(sameItemState(itemFromSlots(next), nextItem)).toBe(true)
  expect(source.item.installedAugments).toBeUndefined()
  expect(() =>
    withSlotItemMetadata(source, { ...nextItem, explicits: [prefix] }),
  ).toThrow('updated checkpoint')
})
it('records concrete values and their slot checkpoint atomically and refuses incomplete history', () => {
  const before = { ...item, explicits: [suffix] }
  const history = startFilm(emptyFilms(), before, 'original', 'fixture-ruleset')
  currentFrame(history)!.abyss = abyssFromConcrete(before, initialFixture)
  const applied = {
    state: item,
    applied: true,
    rulesetIdentity: 'fixture-ruleset',
    action: 'EXALTED',
    events: [],
    assumptions: [],
    consumedOmens: [],
  } as unknown as AppliedItem
  expect(() => recordCraft(history, before, applied, 'fork')).toThrow(
    'checkpoint',
  )
  const next = recordCraft(
    history,
    before,
    applied,
    'fork',
    undefined,
    abyssFromConcrete(item, initialFixture),
  )
  expect(currentFrame(next)?.abyss?.slots).toHaveLength(2)
  expect(currentFrame(history)?.abyss?.slots).toHaveLength(1)
  expect(() =>
    recordCraft(
      history,
      before,
      applied,
      'fork',
      undefined,
      currentFrame(history)!.abyss,
    ),
  ).toThrow('checkpoint')
})
it('shares dormant-omen preservation and rejects missing, duplicate or invented omens', () => {
  const omens = currencyOmens([
    'Omen_of_Sinistral_Necromancy',
    'Omen_of_Whittling',
    'Omen_of_Sinistral_Exaltation',
  ])
  expect(omens.requested).toEqual([
    'Omen_of_Whittling',
    'Omen_of_Sinistral_Exaltation',
  ])
  expect(
    omens.valid(['Omen_of_Sinistral_Exaltation'], ['Omen_of_Whittling']),
  ).toBe(true)
  expect(omens.restore(['Omen_of_Whittling'])).toEqual([
    'Omen_of_Whittling',
    'Omen_of_Sinistral_Necromancy',
  ])
  expect(omens.valid([], ['Omen_of_Whittling'])).toBe(false)
  expect(omens.valid(['Omen_of_Whittling'], ['Omen_of_Whittling'])).toBe(false)
  expect(omens.valid(['invented'], ['Omen_of_Whittling'])).toBe(false)
})

it('normalizes reveal candidate flags without treating candidate order as interchangeable', () => {
  const offer = { slotId: 'hidden', candidates: [prefix, suffix, prefix] }
  expect(
    sameRevealOffer(offer, {
      ...offer,
      candidates: offer.candidates.map((m) => ({
        ...m,
        fractured: false,
        crafted: false,
      })),
    }),
  ).toBe(true)
  expect(
    sameRevealOffer(offer, { ...offer, candidates: [suffix, prefix, prefix] }),
  ).toBe(false)
})
