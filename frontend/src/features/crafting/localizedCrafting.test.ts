import { afterEach, expect, it } from 'vitest'
import { gameName, locales, setLocale } from '../../shared/i18n/i18n'
import { localizedCatalystQuality, catalystItemIds } from './localizedCrafting'
import { catalystTypes, type CatalystQuality } from './catalystQuality'
afterEach(async () => {
  await setLocale('en')
})
it('renders every catalyst quality through the shared localized label, including refined jewel catalysts', async () => {
  for (const locale of locales) {
    await setLocale(locale)
    for (const type of Object.keys(
      catalystItemIds,
    ) as CatalystQuality['type'][]) {
      const quality = { type, amount: 40 }
      const amulet = localizedCatalystQuality(
        quality,
        'Metadata/Items/Amulets/FourAmulet9',
        locale,
      )
      expect(amulet).toContain('40')
      expect(amulet).not.toContain(type)
      expect(amulet).toContain(
        locale === 'en'
          ? catalystTypes[type].name
          : gameName(catalystItemIds[type], '', locale),
      )
      const jewel = localizedCatalystQuality(
        quality,
        'Metadata/Items/Jewels/JewelInt',
        locale,
      )
      expect(jewel).toContain(
        locale === 'en'
          ? catalystTypes[type].name
          : gameName('Refined_' + catalystItemIds[type], '', locale),
      )
    }
  }
})
