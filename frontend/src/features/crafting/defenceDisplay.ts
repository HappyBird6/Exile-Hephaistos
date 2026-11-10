import { derivedWeapons } from './derived-defences/derivedWeapons'
import { propertyLabels } from './derived-defences/propertyLabels'
import type { Locale } from '../../shared/i18n/i18n'
import { translate } from '../../shared/i18n/i18n'
import { basePickerMessages } from './base-picker/basePickerMessages'
import { topBase, topBaseKey } from './topBases'
import { derivedDefences } from './derived-defences/derivedDefences'
import { formatAttributeNumber } from './numeric-display/attributeNumbers'
import type { ItemCardLine } from './itemCardData'
import type { ConcreteItem } from './workbenchApi'
import type { Definition } from './craftingApi'

/** Presentation adapter: replace source defence totals, never add them to computed inputs. */
export function defenceDisplay(
  state: ConcreteItem,
  definitions: Record<string, Definition>,
  locale: Locale,
  bonded = false,
) {
  const result = derivedDefences(state, definitions, bonded)
  const labels = basePickerMessages[locale]
  const keys = {
    armour: labels.armourValue,
    evasion: labels.evasionValue,
    energyShield: labels.energyShieldValue,
    ward: propertyLabels(locale).ward,
  }
  const properties: ItemCardLine[] = []
  for (const key of ['armour', 'evasion', 'energyShield', 'ward'] as const) {
    const value = result[key]
    if (value)
      properties.push({
        id: 'derived-' + key,
        numericLocale: locale,
        text:
          keys[key] +
          ': ' +
          (value.value === null
            ? translate('ui.unknown', {}, locale)
            : formatAttributeNumber(value.value, locale)),
      })
  }
  const weapons = derivedWeapons(state, bonded, definitions)
  const weaponLabels = propertyLabels(locale)
  if (weapons) {
    for (const key of [
      'physical',
      'fire',
      'cold',
      'lightning',
      'chaos',
    ] as const) {
      const range = weapons.damage[key]
      if (range === undefined) continue
      properties.push({
        id: 'derived-' + key,
        numericLocale: locale,
        text:
          weaponLabels[key] +
          ': ' +
          (range === null
            ? translate('ui.unknown', {}, locale)
            : range
                .map((value) => formatAttributeNumber(value, locale))
                .join('–')),
      })
    }
    for (const key of ['attacks', 'critical'] as const) {
      const value = weapons[key]
      if (value !== null)
        properties.push({
          id: 'derived-' + key,
          numericLocale: locale,
          text:
            weaponLabels[key] +
            ': ' +
            new Intl.NumberFormat(locale, { maximumFractionDigits: 2 }).format(
              value,
            ) +
            (key === 'critical' ? '%' : ''),
        })
    }
  }
  const source =
    topBase(topBaseKey(state.baseItemId) ?? '')?.sourceProperties?.en ?? []
  const replacedSourceIndexes = new Set<number>()
  if (properties.length)
    source.forEach((line, index) => {
      if (
        /^(Armour|Evasion(?: Rating)?|Energy Shield|Runic Ward):/.test(line) ||
        (weapons &&
          /^(Physical|Fire|Cold|Lightning|Chaos|Elemental) Damage:|^(Attacks per Second|Critical Hit Chance):/.test(
            line,
          ))
      )
        replacedSourceIndexes.add(index)
    })
  return { properties, replacedSourceIndexes }
}
