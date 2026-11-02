import { describe, expect, it } from 'vitest'
import { baseTexts, baseClass, clipboardBase } from './baseRegistry'

describe('clipboard base identity', () => {
  it('resolves every unique registered name and preserves ambiguous variants', () => {
    for (const [key, text] of Object.entries(baseTexts)) {
      const name = text.split('\n').at(-1)!
      expect(clipboardBase(name, baseClass(key))).toBe(
        key.startsWith('shrine-') ? undefined : key,
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
