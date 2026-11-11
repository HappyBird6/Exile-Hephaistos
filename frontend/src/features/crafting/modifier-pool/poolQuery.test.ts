import { it, expect } from 'vitest'
import { modifierPoolKey } from './poolQuery'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { concreteInitial } from '../workbenchApi'
it('shares a roll-only reference, but separates levels, hidden slots and installed augment contexts', () => {
  const item = {
    ...concreteInitial(initialFixture),
    explicits: [{ modifierId: 'life', values: { life: 10 } }],
  }
  const key = (state = item) =>
    modifierPoolKey(initialFixture, state, 'dynamic')
  expect(
    key({ ...item, explicits: [{ modifierId: 'life', values: { life: 20 } }] }),
  ).toEqual(key())
  expect(key({ ...item, itemLevel: 1 })).not.toEqual(key())
  expect(key({ ...item, conditions: ['CORRUPTED'] })).not.toEqual(key())
  expect(
    modifierPoolKey(initialFixture, { ...item, itemLevel: 1 }, 'static'),
  ).toEqual(modifierPoolKey(initialFixture, item, 'static'))
})
