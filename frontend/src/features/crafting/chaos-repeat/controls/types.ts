import { repeatContract } from '../../../../shared/contracts/public'
export const chaosRepeatCurrencyIds = [
  'Chaos_Orb',
  'Greater_Chaos_Orb',
  'Perfect_Chaos_Orb',
] as const

export type ChaosRepeatCurrencyId = (typeof chaosRepeatCurrencyIds)[number]

/** API fallback for omitted maximumUses, generated from the shared public contract. */
export const defaultChaosRepeatMaximumUses = String(repeatContract.defaultUses)

/** UI request limit from the same source as server validation. */
export const chaosRepeatMaximumUsesLimit = repeatContract.maximumUses

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
