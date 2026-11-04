export const chaosRepeatCurrencyIds = [
  'Chaos_Orb',
  'Greater_Chaos_Orb',
  'Perfect_Chaos_Orb',
] as const

export type ChaosRepeatCurrencyId = (typeof chaosRepeatCurrencyIds)[number]

/** API fallback for omitted maximumUses; the workbench button explicitly requests 1000. */
export const defaultChaosRepeatMaximumUses = '100'

/** User-approved UI request limit; independent of the server's reported cap. */
export const chaosRepeatMaximumUsesLimit = 1000

export type ChaosRepeatControlErrors = {
  pattern?: string
  maximumUses?: string
  request?: string
}

export type ChaosRepeatControlsProps = {
  enabled: boolean
  pattern: string
  /** True only for an actual in-flight request owned by the caller. */
  running: boolean
  disabled?: boolean
  currencyId?: ChaosRepeatCurrencyId
  errors?: ChaosRepeatControlErrors
  onEnabledChange: (enabled: boolean) => void
  onPatternChange: (pattern: string) => void
  /** Optional hook for adding a hint to the same selected/held Chaos description. */
  onHintInteraction?: (currencyId: ChaosRepeatCurrencyId) => void
}
