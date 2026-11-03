import { isBone } from '../desecrationCurrency'
import { workbenchCurrencyActions, workbenchOmens } from '../workbenchApi'
import type { Film, Frame } from '../workbenchHistory'

export type NotationStep = {
  number: number
  frameIndex: number
  currencyId: string
  consumedOmenIds: string[]
  omensKnown: boolean
}

export type SessionNotationData = {
  steps: NotationStep[]
  /** Currency applications whose ID or successful-record evidence is missing. */
  omittedFrames: number
  /** Includes missing arrays and unrecognized/invalid recorded omen IDs. */
  incompleteOmens: boolean
}

const currencyIds = new Map<string, string>(
  Object.entries(workbenchCurrencyActions).map(([id, action]) => [action, id]),
)
const omenIds = new Set<string>(workbenchOmens.map((omen) => omen.id))
const nonCurrencyActions = new Set(['IMPORT', 'BASE_SELECTION'])

function currencyId(action: string): string | undefined {
  return isBone(action) ? action : currencyIds.get(action)
}

function isObservation(action: string): boolean {
  return (
    nonCurrencyActions.has(action) ||
    (action.startsWith('ABYSS_') && action !== 'ABYSS_APPLY')
  )
}

function receipt(frame: Frame) {
  const evidence = frame.evidence
  const manual = frame.manual
  // The two writers use different evidence fields; conflicting records are not proof.
  if (evidence && manual) return undefined
  const record = evidence ?? manual
  return record?.action === frame.action ? record : undefined
}

/**
 * Read-only projection of the complete active Film. Writers record only completed
 * applications; observations carry no inferred currency or omen consumption.
 * A branch is a separate Film and deliberately starts its own numbering.
 */
export function extractSessionNotation(
  film: Film | null | undefined,
  cursor: number,
): SessionNotationData {
  const result: SessionNotationData = {
    steps: [],
    omittedFrames: 0,
    incompleteOmens: false,
  }
  if (
    !film ||
    !Number.isInteger(cursor) ||
    cursor < 0 ||
    cursor >= film.frames.length
  )
    return result

  for (let frameIndex = 1; frameIndex < film.frames.length; frameIndex++) {
    const frame = film.frames[frameIndex]!
    if (frame.action === null || isObservation(frame.action)) continue
    const id = currencyId(frame.action)
    const recorded = receipt(frame)
    if (!id || !recorded) {
      result.omittedFrames++
      continue
    }
    const consumed: unknown = recorded.consumedOmens
    const validArray =
      Array.isArray(consumed) &&
      consumed.every((id): id is string => typeof id === 'string') &&
      new Set(consumed).size === consumed.length
    const consumedOmenIds = validArray ? [...consumed] : []
    const omensKnown =
      validArray && consumedOmenIds.every((id) => omenIds.has(id))
    result.incompleteOmens ||= !omensKnown
    result.steps.push({
      number: result.steps.length + 1,
      frameIndex,
      currencyId: id,
      consumedOmenIds,
      omensKnown,
    })
  }
  return result
}
