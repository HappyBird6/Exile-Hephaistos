import { expect, it } from 'vitest'
import { render } from '@testing-library/react'
import { compactAttribute } from './compactAttribute'
import { compactFrozenCard } from './compactFrozenCard'
import { commonStateCard } from './commonStateCard'
import { ItemCard } from '../ItemCard'
import type { ItemCardData } from '../itemCardData'
const card = (modifiers: ItemCardData['modifiers']): ItemCardData => ({
  name: 'Solar',
  rarity: 'RARE',
  base: 'Solar',
  itemClass: 'Amulet',
  itemLevel: 82,
  properties: [],
  requirements: [],
  flags: [],
  modifiers,
})
it.each([
  [
    'en',
    '+6% to Cold Resistance',
    '+12% to Cold Resistance',
    'Cold Resistance',
  ],
  ['ko', '냉기 저항 +6%', '냉기 저항 +12%', '냉기 저항'],
  ['zh-CN', '+6% 冰霜抗性', '+12% 冰霜抗性', '冰霜抗性'],
  ['zh-TW', '+6% 冰冷抗性', '+12% 冰冷抗性', '冰冷抗性'],
  ['ja', '冷気耐性 +6%', '冷気耐性 +12%', '冷気耐性'],
  [
    'es',
    '+6% a la resistencia al frío',
    '+12% a la resistencia al frío',
    'resistencia al frío',
  ],
] as const)(
  'renders the property once and tier numeric values separately in %s',
  (locale, low, high, name) => {
    const line = compactAttribute(
      { id: 'cold', text: low },
      [
        { label: 'T2', low, high: low },
        { label: 'T1', low: high, high },
      ],
      locale,
    )
    expect(line.text).toBe(name)
    expect(line.tierRows!.map((r) => r.value)).toEqual(['+6%', '+12%'])
    const mounted = render(<ItemCard item={card([line])} />)
    expect(mounted.container.textContent!.split(name)).toHaveLength(2)
    expect(mounted.container.querySelectorAll('tr')).toHaveLength(2)
    mounted.unmount()
  },
)
it('keeps constant numeric qualifiers in the name and only moves changing columns', () => {
  const line = compactAttribute(
    { id: 'a', text: 'Gain 5 Life per 10 Strength' },
    [
      {
        label: 'T2',
        low: 'Gain 5 Life per 10 Strength',
        high: 'Gain 8 Life per 10 Strength',
      },
      {
        label: 'T1',
        low: 'Gain 10 Life per 10 Strength',
        high: 'Gain 12 Life per 10 Strength',
      },
    ],
    'en',
  )
  expect(line.text).toBe('Gain Life per 10 Strength')
  expect(line.tierRows!.map((r) => r.value)).toEqual(['5–8', '10–12'])
})
it('combines a fixed value and distribution for one attribute without repeating the name', () => {
  const varying = compactAttribute(
    { id: 'a', attributeKey: 'cold', text: '+6% to Cold Resistance' },
    [
      {
        label: 'T2',
        low: '+6% to Cold Resistance',
        high: '+12% to Cold Resistance',
      },
    ],
    'en',
  )
  const result = commonStateCard([
    card([varying]),
    card([
      {
        id: 'fixed',
        attributeKey: 'cold',
        text: '+20% to Cold Resistance',
        affixLabel: 'S1',
      },
    ]),
  ])
  expect(result.modifiers).toHaveLength(1)
  expect(result.modifiers[0]!.text).toBe('Cold Resistance')
  expect(result.modifiers[0]!.tierRows!.map((r) => r.value)).toEqual([
    '+6–12%',
    '+20%',
  ])
})
it('does not merge coexisting different attribute identities with matching text', () => {
  const result = commonStateCard([
    card([
      { id: 'a', attributeKey: 'first', text: '+5 Life' },
      { id: 'b', attributeKey: 'second', text: '+7 Life' },
    ]),
    card([
      { id: 'a2', attributeKey: 'first', text: '+8 Life' },
      { id: 'b2', attributeKey: 'second', text: '+9 Life' },
    ]),
  ])
  expect(result.modifiers).toHaveLength(2)
})
it('compacts old frozen repeated property strings without a current catalog', () => {
  const original = card([
    {
      id: 'cold',
      text: '+6–12% to Cold Resistance / +18–24% to Cold Resistance',
      detail: 'S2 Cold Resistance\nS1 Cold Resistance',
    },
  ])
  const before = JSON.stringify(original),
    result = compactFrozenCard(original)
  expect(result.modifiers[0]!.text).toBe('Cold Resistance')
  expect(result.modifiers[0]!.tierRows!.map((r) => r.value)).toEqual([
    '+6–12%',
    '+18–24%',
  ])
  expect(JSON.stringify(original)).toBe(before)
})
