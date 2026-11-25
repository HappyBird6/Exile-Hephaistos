import { gameName, translate, type Locale } from '../../../shared/i18n/i18n'
import { catalystTypes, type CatalystQuality } from '../catalystQuality'
import { catalystItemIds } from '../localizedCrafting'

export function testGoalMessages(locale: Locale) {
  return {
    quality40: translate('support.quality40', {}, locale),
    catalyst: translate('support.catalyst', {}, locale),
    anyCatalyst: translate('support.any_catalyst', {}, locale),
  }
}

export function catalystOptionLabel(
  type: CatalystQuality['type'],
  locale: Locale,
) {
  const catalyst = catalystTypes[type]
  const name = gameName(catalystItemIds[type], catalyst.name, locale)
  const effect = translate(`quality.tag.${catalyst.tags[0]}`, {}, locale)
  return `${name} (${effect})`
}
