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
) {
  const result = derivedDefences(state, definitions)
  const labels = basePickerMessages[locale]
  const keys = {
    armour: labels.armourValue,
    evasion: labels.evasionValue,
    energyShield: labels.energyShieldValue,
  }
  const properties: ItemCardLine[] = []
  for (const key of ['armour', 'evasion', 'energyShield'] as const) {
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
  const source =
    topBase(topBaseKey(state.baseItemId) ?? '')?.sourceProperties?.en ?? []
  const replacedSourceIndexes = new Set<number>()
  if (properties.length)
    source.forEach((line, index) => {
      if (/^(Armour|Evasion(?: Rating)?|Energy Shield):/.test(line))
        replacedSourceIndexes.add(index)
    })
  return { properties, replacedSourceIndexes }
}
