import { useId } from 'react'
import './session-continuation.css'
import { useQuery } from '@tanstack/react-query'
import { useI18n } from '../../../shared/i18n/i18n'
import type { Initial } from '../craftingApi'
import type { Film, Frame } from '../workbenchHistory'
import {
  checkSessionCompatibility,
  type SessionCompatibility,
} from '../workbenchReferenceApi'
import { referenceMessages } from '../referenceMessages'

export function SessionContinuation({
  initial,
  film,
  frame,
  disabled,
  compact = false,
  onContinue,
}: {
  initial: Initial
  film: Film
  frame: Frame
  disabled: boolean
  compact?: boolean
  onContinue: (result: SessionCompatibility) => void
}) {
  const descriptionId = useId()
  const { locale } = useI18n()
  const text = referenceMessages[locale]
  const check = useQuery({
    queryKey: [
      'crafting',
      'session-compatibility',
      initial.rulesetIdentity,
      film.id,
      frame,
      film.compatibilityId,
    ],
    queryFn: ({ signal }) =>
      checkSessionCompatibility(
        initial,
        frame.state,
        frame.abyss,
        film.compatibilityId,
        signal,
      ),
    retry: false,
    staleTime: 60_000,
  })
  function issue(code: string) {
    if (code === 'UNKNOWN_MODIFIER') return text.unknownModifier
    if (code === 'UNSUPPORTED_BASE') return text.unsupportedBase
    if (
      ['VALUE_OUT_OF_RANGE', 'STAT_SET_MISMATCH', 'INVALID_IMPLICIT'].includes(
        code,
      )
    )
      return text.invalidRoll
    if (code === 'ITEM_LEVEL_TOO_LOW') return text.reasons.ITEM_LEVEL
    if (code === 'AFFIX_CAPACITY_EXCEEDED') return text.reasons.AFFIX_FULL
    if (['DUPLICATE_MODIFIER', 'CONFLICTING_MODIFIERS'].includes(code))
      return text.reasons.FAMILY_OCCUPIED
    return text.invalidItem
  }
  return (
    <section
      className={`wb-session-continuation${compact ? ' wb-session-continuation--compact' : ''}`}
      aria-live="polite"
    >
      <p
        id={descriptionId}
        className={compact && check.data?.compatible ? 'sr-only' : undefined}
      >
        {check.isPending
          ? text.checking
          : check.isError
            ? text.failed
            : !check.data?.compatible
              ? text.blocked
              : check.data.sameRules
                ? text.same
                : text.changed}
      </p>
      {check.isSuccess && !check.data.compatible && (
        <ul>
          {[...new Set(check.data.reasons.map(issue))].map((reason) => (
            <li key={reason}>{reason}</li>
          ))}
        </ul>
      )}
      {check.isError && (
        <button
          type="button"
          onClick={() => void check.refetch()}
          disabled={check.isFetching}
        >
          {text.retry}
        </button>
      )}
      {check.isSuccess && check.data.compatible && (
        <button
          type="button"
          className="primary-action"
          aria-describedby={descriptionId}
          disabled={disabled || check.isFetching}
          onClick={() => onContinue(check.data)}
        >
          {text.continue}
        </button>
      )}
    </section>
  )
}
