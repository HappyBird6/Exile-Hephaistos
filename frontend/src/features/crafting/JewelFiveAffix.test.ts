import { readFileSync } from 'node:fs'
import { expect, it } from 'vitest'
import { reviewedBasicJewel, jewelCapacity } from './basicJewel'
import type { ConcreteItem } from './workbenchApi'
import type { Definition, Initial } from './craftingApi'
import {
  emptyFilms,
  LocalFilmRepository,
  startFilm,
  verifiedHistoryState,
} from './workbenchHistory'

type Catalog = {
  metadata: Initial['metadata']
  base: Initial['baseRules'] & { id: string; implicitModifierId: string | null }
  modifiers: Definition[]
}
const bases = [
  'ruby',
  'emerald',
  'sapphire',
  'diamond',
  'time-lost-ruby',
  'time-lost-emerald',
  'time-lost-sapphire',
  'time-lost-diamond',
] as const
function fixture(base: string, p: number, s: number) {
  const c = JSON.parse(
    readFileSync(
      `../backend/src/main/resources/catalog/${base}/catalog.json`,
      'utf8',
    ),
  ) as Catalog
  const families = new Set<string>()
  const explicits: ConcreteItem['explicits'] = []
  const instance = (d: Definition) => ({
    modifierId: d.id,
    values: Object.fromEntries(d.stats!.map((stat) => [stat.id, stat.min])),
  })
  for (const d of c.modifiers)
    if (
      d.weight! > 0 &&
      !d.familyIds.some((id) => families.has(id)) &&
      (d.affixType === 'PREFIX' ? p > 0 : d.affixType === 'SUFFIX' && s > 0)
    ) {
      explicits.push(instance(d))
      d.familyIds.forEach((id) => families.add(id))
      if (d.affixType === 'PREFIX') p--
      else s--
    }
  expect(p + s).toBe(0)
  const item: ConcreteItem = {
    snapshotId: c.metadata.snapshotId,
    baseItemId: c.base.id,
    itemLevel: 82,
    rarity: 'RARE',
    conditions: [],
    explicits,
    implicits: c.base.implicitModifierId
      ? [instance(c.modifiers.find((d) => d.id === c.base.implicitModifierId)!)]
      : [],
  }
  const initial = {
    metadata: c.metadata,
    state: item,
    baseRules: c.base,
    modifiers: Object.fromEntries(c.modifiers.map((d) => [d.id, d])),
    rulesetIdentity: 'fixture-ruleset',
    actions: [],
    id: 'fixture',
    ruleVersion: 'five-affix',
  } as unknown as Initial
  return { item, initial }
}

it.each(bases)(
  'reads and restores five-affix %s items without raising their insertion caps',
  (base) => {
    for (const [p, s] of [
      [2, 3],
      [3, 2],
    ]) {
      const { item, initial } = fixture(base, p!, s!)
      expect(reviewedBasicJewel(item)).toBe(true)
      expect(verifiedHistoryState(item, initial)).toBe(true)
      expect(jewelCapacity(item, 'PREFIX')).toBe(2)
      expect(jewelCapacity(item, 'SUFFIX')).toBe(2)
      const history = startFilm(
        emptyFilms(),
        item,
        'five-affix',
        initial.rulesetIdentity,
      )
      let bytes: string | null = null
      const storage = {
        getItem: () => bytes,
        setItem: (_key: string, value: string) => {
          bytes = value
        },
      }
      const repository = new LocalFilmRepository(storage)
      repository.save(history)
      expect(repository.load()).toEqual(history)
      expect(reviewedBasicJewel({ ...item, rarity: 'MAGIC' })).toBe(false)
    }
    const six = fixture(base, 3, 3)
    expect(reviewedBasicJewel(six.item)).toBe(false)
  },
)

it.each(bases)(
  'counts the unrevealed third suffix on %s as an occupied preserved slot',
  (base) => {
    const { item, initial } = fixture(base, 2, 2)
    const hidden: ConcreteItem = {
      ...item,
      desecrated: [
        { id: 'preserved-third-suffix', side: 'SUFFIX', modifierId: null },
      ],
    }
    expect(reviewedBasicJewel(hidden)).toBe(true)
    expect(verifiedHistoryState(hidden, initial)).toBe(true)
    expect(
      reviewedBasicJewel({
        ...hidden,
        desecrated: [
          ...hidden.desecrated!,
          { id: 'second', side: 'PREFIX', modifierId: null },
        ],
      }),
    ).toBe(false)
  },
)
