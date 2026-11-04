export const chaosRepeatCurrencyIds = [
  'Chaos_Orb',
  'Greater_Chaos_Orb',
  'Perfect_Chaos_Orb',
] as const

export type ChaosRepeatCurrencyId = (typeof chaosRepeatCurrencyIds)[number]

/** Initial editor value, not a server cap or an enforced UI limit. */
export const defaultChaosRepeatMaximumUses = '500'

export type ChaosRepeatControlErrors = {
  pattern?: string
  maximumUses?: string
  request?: string
}

export type ChaosRepeatControlsProps = {
  enabled: boolean
  pattern: string
  /** Preserve raw input, including empty/incomplete values, for parent validation. */
  maximumUses: string
  /** True only for an actual in-flight request owned by the caller. */
  running: boolean
  disabled?: boolean
  currencyId?: ChaosRepeatCurrencyId
  errors?: ChaosRepeatControlErrors
  /** Display-only, externally validated cap. null/undefined means unknown. */
  serverMaximumUses?: number | null
  onEnabledChange: (enabled: boolean) => void
  onPatternChange: (pattern: string) => void
  onMaximumUsesChange: (maximumUses: string) => void
  /** Optional hook for adding a hint to the same selected/held Chaos description. */
  onHintInteraction?: (currencyId: ChaosRepeatCurrencyId) => void
}
