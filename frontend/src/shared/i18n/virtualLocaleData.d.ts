declare module 'virtual:locale-data' {
  export const data: { messages: unknown; terms: unknown; modifiers: unknown }
  export function isLocaleLoaded(locale: string): boolean
  export function loadLocaleData(locale: string): Promise<void>
}
