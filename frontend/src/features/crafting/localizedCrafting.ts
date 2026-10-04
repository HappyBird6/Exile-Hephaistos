import { gameName } from '../../shared/i18n/i18n'
import { workbenchCurrencyActions, workbenchActionNames } from './workbenchApi'
import type { WorkbenchAction } from './workbenchApi'
import type { CatalystQuality } from './catalystQuality'

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
