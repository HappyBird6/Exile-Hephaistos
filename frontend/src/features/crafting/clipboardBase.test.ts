import { describe, expect, it } from 'vitest'
import {
  baseTexts,
  baseClass,
  clipboardBase,
  selectableBaseKeys,
  supersededBases,
} from './baseRegistry'

describe('clipboard base identity', () => {
  it('resolves every unique registered name and preserves ambiguous variants', () => {
    for (const [key, text] of Object.entries(baseTexts)) {
      const name = text.split('\n').at(-1)!
      expect(clipboardBase(name, baseClass(key))).toBe(
        key.startsWith('shrine-') || key.startsWith('shrouded-mail-')
          ? undefined
          : key,
      )
    }
  })
  it('does not guess names, classes, or replace lower bases with higher ones', () => {
    expect(clipboardBase('Stocky Mitts', 'Gloves')).toBe('stocky')
    expect(clipboardBase('Soldier Cuirass', 'Body Armours')).toBe('soldier')
    expect(clipboardBase('Soldier Cuirass', 'Amulets')).toBeUndefined()
    expect(clipboardBase('Healthy Solar Amulet', 'Amulets')).toBeUndefined()
    expect(clipboardBase('Solar Amulet', 'Amulets')).toBe('solar')
  })
})

it('keeps 179 compatible identities and excludes only superseded and user-excluded new selections', () => {
  expect(Object.keys(baseTexts)).toHaveLength(179)
  expect(selectableBaseKeys).toHaveLength(171)
  for (const [lower, upper] of Object.entries(supersededBases)) {
    expect(selectableBaseKeys).not.toContain(lower)
    expect(selectableBaseKeys).toContain(upper)
  }
})

it('resolves Shrine variants only from an exact consistent granted skill', () => {
  for (const [element, key] of [
    ['Fire', 'shrine-fire'],
    ['Ice', 'shrine-ice'],
    ['Lightning', 'shrine-lightning'],
  ]) {
    expect(
      clipboardBase(
        'Shrine Sceptre',
        'Sceptres',
        `Grants Skill: Level 17 Purity of ${element}`,
      ),
    ).toBe(key)
  }
  expect(
    clipboardBase('Shrine Sceptre', 'Sceptres', 'Grants Skill: Unknown'),
  ).toBeUndefined()
  expect(
    clipboardBase(
      'Shrine Sceptre',
      'Sceptres',
      'Grants Skill: Purity of Fire\nGrants Skill: Purity of Ice',
    ),
  ).toBeUndefined()
  expect(
    clipboardBase(
      'Shrine Sceptre',
      'Sceptres',
      'Grants Skill: Level 0 Purity of Fire',
    ),
  ).toBeUndefined()
})
