import { expect, it } from 'vitest'
import catalog from '../../shared/test/sapphire-catalog.json'
import templates from '../../shared/i18n/modifierTemplates.json'
import { reviewedSapphire, sapphireBase } from './sapphireJewel'
import { workbenchCurrencyActions } from './workbenchApi'
import type { ConcreteItem } from './workbenchApi'

const item = (
  ids: string[],
  rarity: ConcreteItem['rarity'] = 'RARE',
): ConcreteItem => ({
  snapshotId: catalog.metadata.snapshotId,
  baseItemId: sapphireBase,
  itemLevel: 82,
  rarity,
  implicits: [],
  conditions: [],
  explicits: ids.map((id) => {
    const d = catalog.modifiers.find((d) => d.id === id)!
    return {
      modifierId: id,
      values: Object.fromEntries(d.stats.map((s) => [s.id, s.min])),
    }
  }),
})
it('preserves valid four-slot Sapphire films and rejects fifth slots, shared families and two Crafted results', () => {
  expect(reviewedSapphire(item([], 'NORMAL'))).toBe(true)
  expect(
    reviewedSapphire(
      item(
        ['sapphire:prefix:shimmering', 'sapphire:suffix:of-enchanting'],
        'MAGIC',
      ),
    ),
  ).toBe(true)
  const full = [
    'sapphire:prefix:shimmering',
    'sapphire:prefix:chilling',
    'sapphire:suffix:of-enchanting',
    'sapphire:suffix:of-unmaking',
  ]
  expect(reviewedSapphire(item(full))).toBe(true)
  expect(reviewedSapphire(item(full, 'MAGIC'))).toBe(false)
  expect(reviewedSapphire(item([...full, 'sapphire:suffix:of-energy']))).toBe(
    false,
  )
  expect(
    reviewedSapphire(
      item(['sapphire:prefix:bestial', 'sapphire:prefix:overgrown']),
    ),
  ).toBe(false)
  expect(
    reviewedSapphire(
      item([
        'sapphire:crafted:JewelColdDamage',
        'sapphire:crafted:JewelCastSpeed',
      ]),
    ),
  ).toBe(false)
  expect(
    reviewedSapphire(
      item(['sapphire:prefix:chilling', 'sapphire:crafted:JewelColdDamage']),
    ),
  ).toBe(false)
})
it('binds all 58 ordinary and 10 zero-spawn Crafted definitions to six sourced languages and material IDs', () => {
  expect(catalog.modifiers.filter((d) => d.weight > 0)).toHaveLength(58)
  const bindings = templates.definitions as Record<
    string,
    { template: string; englishText: string }
  >
  const langs = templates.templates as Record<
    string,
    Record<string, { name: string; template: string }>
  >
  for (const d of catalog.modifiers) {
    expect(bindings[d.id]?.englishText).toBe(d.text)
    for (const locale of ['en', 'ko', 'zh-CN', 'zh-TW', 'ja', 'es'])
      expect(langs[locale]?.[bindings[d.id]!.template]?.template).toBeTruthy()
    if (d.tags.includes('crafted'))
      expect(
        workbenchCurrencyActions[d.sourceUrl.split('/').at(-1)!],
      ).toBeTruthy()
  }
})
