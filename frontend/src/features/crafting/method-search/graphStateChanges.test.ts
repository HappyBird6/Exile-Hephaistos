import { expect, it } from 'vitest'
import { graphStateChanges } from './graphStateChanges'
import { createItemPresentation } from '../path-tree/presentation'
import { requestFixture } from '../path-tree/fixtures.test-support'
const card = createItemPresentation(
  { name: 'Solar Amulet', itemClass: 'Amulet' },
  {},
)(requestFixture().start.item, 'en')
it('marks actual added, changed and removed lines without merging alternatives', () => {
  const before = {
    ...card,
    modifiers: [
      { id: 'life', text: 'Life 10' },
      { id: 'mana', text: 'Mana 20' },
    ],
  }
  const after = {
    ...card,
    modifiers: [
      { id: 'life', text: 'Life 15' },
      { id: 'minion', text: 'Minion skills +3' },
    ],
  }
  expect(graphStateChanges(after, before)).toEqual({
    marks: { life: 'changed', minion: 'added' },
    removed: [{ id: 'mana', text: 'Mana 20' }],
  })
  expect(after.modifiers).toHaveLength(2)
})
it('does not invent a change when there is no selected predecessor', () => {
  expect(graphStateChanges(card)).toEqual({ marks: {}, removed: [] })
  expect(graphStateChanges(card, card)).toEqual({ marks: {}, removed: [] })
})
