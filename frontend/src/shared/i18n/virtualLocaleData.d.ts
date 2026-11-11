declare module 'virtual:locale-data' {
  export const data: {
    messages: unknown
    terms: unknown
    modifiers: unknown
    augments: unknown
    augmentEffects: unknown
  }
  export function loadModifierDefinitions(ids: string[]): Promise<void>
  export function isLocaleLoaded(locale: string): boolean
  export function loadLocaleData(locale: string): Promise<void>
}
declare module 'virtual:crafting-data' {
  export const materialNames: Record<string, string>
  export const definitions: unknown
  export const reveal: unknown
  export const essences: unknown
  export function loadBaseData(base: string): Promise<void>
}
