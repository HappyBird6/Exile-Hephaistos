import { expect, it } from 'vitest'
import manifest from './topBases.json'
import templates from '../../shared/i18n/modifierTemplates.json'
import {
  catalystProjection,
  catalystTypes,
  verifiedCatalystQuality,
} from './catalystQuality'
import { localizedModifierText } from './localizedModifiers'
import { maximumQuality } from './qualityLimit'
import { roundQualityRatio } from './qualityRoundingPolicy'
import type { Definition } from './craftingApi'
import type { ConcreteItem } from './workbenchApi'

const keys = [
  'kinetic',
  'vitalic',
  'mnemonic',
  'pearl',
  'amethyst',
  'prismatic',
  'ruby-ring',
  'two-stone-fire-cold',
] as const
const tags = {
  kinetic: ['physical', 'attack'],
  vitalic: ['life'],
  mnemonic: ['mana'],
  pearl: ['caster', 'speed'],
  amethyst: ['chaos', 'resistance'],
  prismatic: ['elemental', 'fire', 'cold', 'lightning', 'resistance'],
  'ruby-ring': ['fire', 'resistance'],
  'two-stone-fire-cold': ['fire', 'cold', 'resistance'],
}
for (const key of keys)
  it(`${key} source implicit translates and Catalyst scales once while preserving original rolls and reachable overflow`, () => {
    const b = manifest[key],
      binding = (
        templates.definitions as Record<
          string,
          { englishText: string; stats: NonNullable<Definition['stats']> }
        >
      )[b.implicitModifierId]!
    const d: Definition = {
      id: b.implicitModifierId,
      name: b.name,
      text: binding.englishText,
      stats: binding.stats,
      tier: 0,
      weight: 0,
      layer: 'IMPLICIT',
      affixType: 'NONE',
      familyIds: [],
      tags: tags[key],
    }
    const values = Object.fromEntries(d.stats!.map((s) => [s.id, s.max]))
    const state: ConcreteItem = {
      snapshotId: 'source-test',
      baseItemId: b.id,
      itemLevel: 82,
      rarity: 'NORMAL',
      implicits: [{ modifierId: d.id, values, fractured: false }],
      explicits: [],
      conditions: [],
      augmentSockets: null,
      catalystQuality: null,
    }
    for (const locale of ['en', 'ko', 'ja', 'zh-CN', 'zh-TW', 'es'] as const) {
      const text = localizedModifierText(d, values, locale)
      expect(text).not.toMatch(/\{v\d+\}|source units| = /)
      expect(text.length).toBeGreaterThan(4)
    }
    for (const type of Object.keys(
      catalystTypes,
    ) as (keyof typeof catalystTypes)[]) {
      const q = { type, amount: 20 },
        before = JSON.stringify(values),
        p = catalystProjection(d, values, q, state)
      const matches = catalystTypes[type].tags.some((t) => d.tags!.includes(t))
      expect(p.status).toBe(matches ? 'SCALED_INTEGER' : 'NO_MATCH')
      for (const [id, v] of Object.entries(values))
        expect(p.values[id]).toBe(
          matches ? roundQualityRatio(BigInt(v) * 120n, 100n) : v,
        )
      expect(JSON.stringify(values)).toBe(before)
      expect(
        verifiedCatalystQuality(
          { ...state, catalystQuality: q },
          { [d.id]: d },
        ),
      ).toBe(true)
    }
    expect(maximumQuality(state, { [d.id]: d })).toBe(20)
    expect(
      verifiedCatalystQuality(
        { ...state, catalystQuality: { type: 'REAVER', amount: 40 } },
        { [d.id]: d },
      ),
    ).toBe(true)
    expect(
      verifiedCatalystQuality(
        { ...state, catalystQuality: { type: 'REAVER', amount: 41 } },
        { [d.id]: d },
      ),
    ).toBe(false)
    expect(
      verifiedCatalystQuality(
        {
          ...state,
          baseItemId: 'Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow1',
          catalystQuality: { type: 'REAVER', amount: 20 },
        },
        { [d.id]: d },
      ),
    ).toBe(false)
  })
