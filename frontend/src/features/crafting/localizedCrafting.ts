import { gameName, translate, type Locale } from '../../shared/i18n/i18n'
import { workbenchCurrencyActions, workbenchActionNames } from './workbenchApi'
import type { WorkbenchAction } from './workbenchApi'
import type { CatalystQuality } from './catalystQuality'
import { catalystTypes } from './catalystQuality'
import { qualityWorkMessages } from './method-search/qualityWorkMessages'
import type { ConcreteItem } from './workbenchApi'
import { isBasicJewel } from './basicJewel'

// Display adapters join only on stable IDs. Never replace catalog or request fields.
export function localizedAction(action: WorkbenchAction) {
  const id = Object.keys(workbenchCurrencyActions).find(
    (id) => workbenchCurrencyActions[id] === action,
  )
  return gameName(id ?? action, workbenchActionNames[action])
}
export const catalystItemIds: Record<CatalystQuality['type'], string> = {
  FLESH: 'Flesh_Catalyst',
  NEURAL: 'Neural_Catalyst',
  CARAPACE: 'Carapace_Catalyst',
  UUL_NETOL: 'Uul-Netols_Catalyst',
  XOPH: 'Xophs_Catalyst',
  TUL: 'Tuls_Catalyst',
  ESH: 'Eshs_Catalyst',
  CHAYULA: 'Chayulas_Catalyst',
  REAVER: 'Reaver_Catalyst',
  SIBILANT: 'Sibilant_Catalyst',
  SKITTERING: 'Skittering_Catalyst',
  ADAPTIVE: 'Adaptive_Catalyst',
  NECROTIC: 'Necrotic_Catalyst',
}

/** One quality label for the workbench, saved sessions, public previews and snapshots. */
export function localizedCatalystQuality(
  quality: CatalystQuality,
  baseId: string,
  locale: Locale,
) {
  return translate(
    'quality.current',
    {
      type:
        locale === 'en'
          ? catalystTypes[quality.type].name
          : gameName(
              (isBasicJewel(baseId) ? 'Refined_' : '') +
                catalystItemIds[quality.type],
              catalystTypes[quality.type].name,
              locale,
            ),
      value: quality.amount,
    },
    locale,
  )
}

/** 아이템 카드에서 확인된 기폭제 상태 또는 종류를 정하지 않은 제작 체크포인트를 표시한다. */
export function sessionCatalystProperty(
  state: ConcreteItem,
  unspecified: boolean,
  locale: Locale,
) {
  if (unspecified)
    return [
      { id: 'planned-quality', text: qualityWorkMessages[locale].completed },
    ]
  return state.catalystQuality
    ? [
        {
          id: 'catalyst-quality',
          text: localizedCatalystQuality(
            state.catalystQuality,
            state.baseItemId,
            locale,
          ),
        },
      ]
    : []
}
