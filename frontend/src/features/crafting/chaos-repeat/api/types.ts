import type { Locale } from '../../../../shared/i18n/i18n'
import type { AbyssState } from '../../AbyssPanel'
import type { AppliedItem, ConcreteItem } from '../../workbenchApi'
import type {
  ChaosRepeatAction,
  ChaosRepeatReceipt,
} from '../chaosRepeatReceipt'

export type ChaosRepeatState =
  | { kind: 'CONCRETE'; item: ConcreteItem }
  | { kind: 'ABYSS'; abyss: AbyssState }

export type ChaosRepeatRequest = {
  requestId: string
  state: ChaosRepeatState
  action: ChaosRepeatAction
  activeOmens: string[]
  pattern: string
  locale: Locale
  maximumUses?: number
}

export type ChaosRepeatTermination =
  'MATCH' | 'LIMIT' | 'INAPPLICABLE' | 'ERROR'
export type ChaosRepeatFailure = {
  code: 'REPEAT_EXECUTION_FAILED'
  stage: 'APPLY' | 'MATCHER'
}
export type ChaosRepeatRejection = {
  code: 'CURRENCY_INAPPLICABLE'
  reason: string
}
export type ChaosRepeatWitness =
  | ({ kind: 'CONCRETE' } & Omit<AppliedItem, 'state' | 'rulesetIdentity'>)
  | {
      kind: 'ABYSS'
      action: ChaosRepeatAction
      applied: true
      reason: string
      consumedOmens: string[]
      remainingOmens: string[]
    }

export type ChaosRepeatResult =
  | {
      kind: 'completed' | 'partial'
      requestId: string
      finalState: ChaosRepeatState
      actualUses: number
      /** Confirmed aggregate, one ID per actual consumption. */
      consumedOmens: string[]
      remainingOmens: string[]
      termination: ChaosRepeatTermination
      failure: ChaosRepeatFailure | null
      lastRejection: ChaosRepeatRejection | null
      receipt: ChaosRepeatReceipt
    }
  | {
      kind: 'no-use'
      requestId: string
      termination: 'INAPPLICABLE' | 'ERROR'
      failure: ChaosRepeatFailure | null
      lastRejection: ChaosRepeatRejection | null
    }
  | {
      kind: 'rejected'
      requestId: string
      source: 'client' | 'server'
      status?: number
    }
  | {
      kind: 'uncertain'
      requestId: string
      reason: 'NETWORK' | 'ABORTED' | 'HTTP_ERROR'
      status?: number
    }
  | { kind: 'invalid-response'; requestId: string }
