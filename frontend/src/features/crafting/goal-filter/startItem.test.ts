import { describe, expect, it } from 'vitest'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { concreteInitial } from '../workbenchApi'
import type { ConcreteItem } from '../workbenchApi'
import type { Definition, Initial } from '../craftingApi'
import jewelDefinitions from '../basicJewelDefinitions.json'
import {
  eligibleStartModifiers,
  startClassMatches,
  startItemIssues,
} from './startItem'

const equipment: Initial = {
  ...initialFixture,
  state: {
    ...initialFixture.state,
    baseItemId: 'Metadata/Items/Rings/FourRing1',
  },
}
const ruby: Initial = {
  ...initialFixture,
  state: {
    ...initialFixture.state,
    baseItemId: 'Metadata/Items/Jewels/JewelStr',
    implicits: [],
  },
  modifiers: Object.fromEntries(
    Object.entries(jewelDefinitions)
      .filter(([, d]) => d.baseItemId === 'Metadata/Items/Jewels/JewelStr')
      .map(([id, d]) => [
        id,
        {
          id,
          name: id,
          text: id,
          tier: 1,
          affixType: d.affixType,
          familyIds: d.familyIds,
          stats: d.stats,
          layer: 'EXPLICIT',
          tags: d.crafted ? ['crafted'] : [],
        },
      ]),
  ) as Record<string, Definition>,
}
function add(item: ConcreteItem, definition: Definition): ConcreteItem {
  return {
    ...item,
    rarity: 'RARE',
    explicits: [
      ...item.explicits,
      {
        modifierId: definition.id,
        values: Object.fromEntries(definition.stats!.map((s) => [s.id, s.min])),
      },
    ],
  }
}
function ordinary(initial: Initial, item: ConcreteItem, side: string) {
  return eligibleStartModifiers(initial, item).find(
    (d) => d.affixType === side && !d.tags?.includes('crafted'),
  )!
}
describe('Starting item catalog validation', () => {
  it('uses exact known class aliases without matching other classes or names', () => {
    expect(startClassMatches('Amulets', 'Amulet')).toBe(true)
    expect(startClassMatches('Amulets', 'Amulets')).toBe(true)
    expect(startClassMatches('Amulets', 'Unknown Amulet')).toBe(false)
    expect(startClassMatches('Rings', 'Amulet')).toBe(false)
  })
  it('permits non-Solar equipment definitions and rejects conflicts, stat errors and wrong snapshots', () => {
    const normal = concreteInitial(equipment)
    const rare = add(normal, ordinary(equipment, normal, 'PREFIX'))
    expect(startItemIssues(equipment, rare)).toEqual([])
    expect(
      eligibleStartModifiers(equipment, rare).some((d) => d.id === 'p'),
    ).toBe(false)
    expect(
      startItemIssues(equipment, { ...rare, snapshotId: 'other' }),
    ).not.toEqual([])
    expect(
      startItemIssues(equipment, {
        ...rare,
        explicits: [...rare.explicits, ...rare.explicits],
      }),
    ).not.toEqual([])
    expect(
      startItemIssues(equipment, {
        ...rare,
        explicits: [{ modifierId: 'p', values: { life: 21 } }],
      }),
    ).not.toEqual([])
    expect(
      startItemIssues(equipment, {
        ...rare,
        explicits: [{ modifierId: 'p', values: { other: 10 } }],
      }),
    ).not.toEqual([])
  })
  it('validates Normal/Magic/Rare equipment capacities against catalog instances', () => {
    const initial = { ...equipment, modifiers: { ...equipment.modifiers } }
    for (let i = 0; i < 4; i++)
      initial.modifiers[`p${i}`] = {
        ...initialFixture.modifiers.p!,
        id: `p${i}`,
        familyIds: [`family${i}`],
      }
    let item = concreteInitial(initial)
    for (let i = 0; i < 3; i++) item = add(item, initial.modifiers[`p${i}`]!)
    expect(startItemIssues(initial, item)).toEqual([])
    expect(
      eligibleStartModifiers(initial, item).filter(
        (d) => d.affixType === 'PREFIX',
      ),
    ).toEqual([])
    expect(
      startItemIssues(initial, add(item, initial.modifiers.p3!)),
    ).not.toEqual([])
    expect(startItemIssues(initial, { ...item, rarity: 'MAGIC' })).not.toEqual(
      [],
    )
    expect(startItemIssues(initial, { ...item, rarity: 'NORMAL' })).not.toEqual(
      [],
    )
  })
  it('reuses jewel insertion limits and rejects a third ordinary prefix', () => {
    let item = concreteInitial(ruby)
    item = add(item, ordinary(ruby, item, 'PREFIX'))
    item = add(item, ordinary(ruby, item, 'PREFIX'))
    expect(startItemIssues(ruby, item)).toEqual([])
    expect(
      eligibleStartModifiers(ruby, item).filter(
        (d) => d.affixType === 'PREFIX' && !d.tags?.includes('crafted'),
      ),
    ).toEqual([])
    item = add(item, ordinary(ruby, item, 'SUFFIX'))
    item = add(item, ordinary(ruby, item, 'SUFFIX'))
    expect(startItemIssues(ruby, item)).toEqual([])
    expect(eligibleStartModifiers(ruby, item)).toEqual([])
  })
  it('allows a reviewed Crafted capacity grant, preserves valid cap-loss state, and blocks a second Crafted modifier', () => {
    let item = concreteInitial(ruby)
    const grant = Object.values(ruby.modifiers).find((d) =>
      d.id.endsWith(':crafted:CraftedJewelAdditionalPrefixAllowed'),
    )!
    item = add(item, grant)
    for (let i = 0; i < 3; i++) item = add(item, ordinary(ruby, item, 'PREFIX'))
    expect(startItemIssues(ruby, item)).toEqual([])
    expect(
      eligibleStartModifiers(ruby, item).some((d) =>
        d.tags?.includes('crafted'),
      ),
    ).toBe(false)
    const lost = {
      ...item,
      explicits: item.explicits.filter((m) => m.modifierId !== grant.id),
    }
    expect(startItemIssues(ruby, lost)).toEqual([])
    expect(
      eligibleStartModifiers(ruby, lost).some((d) => d.affixType === 'PREFIX'),
    ).toBe(false)
  })
})
