import {
  data,
  isLocaleLoaded,
  loadLocaleData,
  loadModifierDefinitions,
} from 'virtual:locale-data'
export const messages = data.messages as typeof import('./messages.json')
export const terms = data.terms as typeof import('./gameTerms.json')
export const modifierTemplates =
  data.modifiers as typeof import('./modifierTemplates.json')
export { isLocaleLoaded, loadLocaleData }

export const augmentData =
  data.augments as typeof import('../../features/crafting/augments.json')
export const augmentEffectTemplates =
  data.augmentEffects as typeof import('../../features/crafting/augmentEffectTemplates.json')
export { loadModifierDefinitions }
