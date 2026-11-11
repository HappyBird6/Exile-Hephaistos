import { data, isLocaleLoaded, loadLocaleData } from 'virtual:locale-data'
export const messages = data.messages as typeof import('./messages.json')
export const terms = data.terms as typeof import('./gameTerms.json')
export const modifierTemplates =
  data.modifiers as typeof import('./modifierTemplates.json')
export { isLocaleLoaded, loadLocaleData }
