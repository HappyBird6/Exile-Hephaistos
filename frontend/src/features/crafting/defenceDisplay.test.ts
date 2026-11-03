import { expect, it } from 'vitest'
import { defenceDisplay } from './defenceDisplay'
import bases from './topBases.json'
import type { ConcreteItem } from './workbenchApi'

const state: ConcreteItem = {
  snapshotId: 'display-test',
  baseItemId: bases['tawhoan-tower-shield'].id,
  itemLevel: 82,
  rarity: 'RARE',
  implicits: [],
  explicits: [],
  conditions: [],
  equipmentQuality: 20,
}
it('replaces the shield source total with one computed property and retains unrelated source lines', () => {
  const before = structuredClone(state)
  const result = defenceDisplay(state, {}, 'en')
  expect(result.properties).toEqual([
    { id: 'derived-armour', text: 'Armour: 316.8', numericLocale: 'en' },
  ])
  expect([...result.replacedSourceIndexes]).toEqual([1])
  expect(state).toEqual(before)
})
it('does not assume zero quality for a retained item with unknown quality', () => {
  const result = defenceDisplay({ ...state, equipmentQuality: null }, {}, 'en')
  expect(result.properties[0]?.text).toBe('Armour: Unknown')
})
it('does not label unrelated equipment as armour or add source totals to computation', () => {
  expect(
    defenceDisplay({ ...state, baseItemId: bases['jade-amulet'].id }, {}, 'en')
      .properties,
  ).toEqual([])
})
