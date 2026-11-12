import { expect, it } from 'vitest'
import { currentItemText, tryCurrentItemText } from './currentItemText'
import { concreteInitial } from './workbenchApi'
import { initialFixture } from '../../shared/test/craftingFixtures'
it('preserves an item with an unknown historical modifier without offering partial clipboard text', () => {
  const item = {
    ...concreteInitial(initialFixture),
    explicits: [{ modifierId: 'old-season-modifier', values: { old: 7 } }],
  }
  const saved = structuredClone(item)
  expect(() =>
    currentItemText(item, initialFixture.modifiers, 'Solar Amulet', 'Amulets'),
  ).toThrow('Unknown modifier')
  expect(
    tryCurrentItemText(
      item,
      initialFixture.modifiers,
      'Solar Amulet',
      'Amulets',
    ),
  ).toBeNull()
  expect(item).toEqual(saved)
})
it('keeps verified text available and unchanged', () => {
  const item = concreteInitial(initialFixture)
  expect(
    tryCurrentItemText(
      item,
      initialFixture.modifiers,
      'Solar Amulet',
      'Amulets',
    ),
  ).toBe(
    currentItemText(item, initialFixture.modifiers, 'Solar Amulet', 'Amulets'),
  )
})
