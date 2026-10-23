import { expect, it } from 'vitest'
import { jointAttributeRenderings } from './jointAttributePresentation'
import { requestFixture } from '../path-tree/fixtures.test-support'
import type { AutomaticNode } from './automaticApi'
import type { PathTreeProps } from '../path-tree/PathTree'

const present: PathTreeProps['presentItem'] = (item) => ({
  name: 'Ring',
  rarity: 'RARE',
  base: 'Ring',
  itemClass: 'Ring',
  itemLevel: 82,
  properties: [],
  requirements: [],
  flags: [],
  modifiers: item.explicits.map((m) => ({
    id: m.modifierId,
    text: `Adds ${m.values.x} to ${m.values.y} Physical Damage`,
  })),
})
function fixture(points: [number, number][]): AutomaticNode {
  const items = points.map(([x, y]) => ({
    ...requestFixture().start.item,
    explicits: [{ modifierId: 'hybrid', values: { x, y }, fractured: false }],
  }))
  return {
    id: 1,
    depth: 1,
    item: items[0]!,
    quality40: false,
    progress: { goal: 'MATCH', entries: [] },
    discoveryFinished: true,
    directFinished: true,
    members: items.map((item) => ({ item, tiers: {}, rolls: {} })),
    attributes: {
      hybrid: points.flatMap(([x, y]) => [
        {
          modifierId: 'hybrid',
          stat: 'x',
          min: x,
          max: x,
          tier: 1,
          role: 'joint',
        },
        {
          modifierId: 'hybrid',
          stat: 'y',
          min: y,
          max: y,
          tier: 1,
          role: 'joint',
        },
      ]),
    },
  }
}
it('renders correlated numeric endpoints from complete tuples without crossing stat columns', () => {
  const n = fixture([
      [2, 4],
      [3, 6],
    ]),
    before = JSON.stringify(n)
  expect(jointAttributeRenderings(n, 'hybrid', [n], 'en', present)).toEqual([
    {
      label: 'T1',
      low: 'Adds 2 to 4 Physical Damage',
      high: 'Adds 3 to 6 Physical Damage',
    },
  ])
  expect(JSON.stringify(n)).toBe(before)
})
it('keeps non-monotone concrete tuples separate instead of inventing marginal minima', () => {
  const n = fixture([
    [1, 4],
    [4, 1],
  ])
  const rows = jointAttributeRenderings(n, 'hybrid', [n], 'en', present)
  expect(rows.map((r) => r.low)).toEqual([
    'Adds 1 to 4 Physical Damage',
    'Adds 4 to 1 Physical Damage',
  ])
  expect(rows.every((r) => r.low === r.high)).toBe(true)
  expect(JSON.stringify(rows)).not.toContain('Adds 1 to 1')
})
it('retains whole tuples from the saved joint source without flattening them into independent stats', () => {
  const source = fixture([
      [2, 4],
      [3, 6],
    ]),
    target = fixture([[2, 4]])
  target.id = 2
  target.joint = [
    {
      method: 'm',
      source: source.id,
      kind: 'CHECKPOINT',
      retained: { hybrid: 'hybrid' },
    },
  ]
  expect(
    jointAttributeRenderings(target, 'hybrid', [source, target], 'en', present),
  ).toEqual([
    {
      label: 'T1',
      low: 'Adds 2 to 4 Physical Damage',
      high: 'Adds 3 to 6 Physical Damage',
    },
  ])
})
