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
  export const materialRows: Record<
    string,
    {
      targets: Record<string, { ids: string[]; level: number }>
      details: Record<
        string,
        { sourceUrl: string; stats: { id: string; min: number; max: number }[] }
      >
      coupled: boolean
    }
  >
  export const materialActions: Record<string, string>
  export const materialProfiles: Record<
    string,
    {
      kind: string
      mode: string
      inputRarities: string[]
      resultRarity: string | null
    }
  >
  export function loadBaseData(base: string): Promise<void>
}
