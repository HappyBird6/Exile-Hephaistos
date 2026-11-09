import { revealAction } from '../abyss-workbench/revealAction'
import { augments } from '../socketAugments'
import { isBone } from '../desecrationCurrency'
import { workbenchCurrencyActions, workbenchOmens } from '../workbenchApi'
import type { Film, Frame } from '../workbenchHistory'
import { validChaosRepeatFrame } from '../chaos-repeat/chaosRepeatReceipt'

export type NotationStep = {
  kind?: 'reveal'

  number: number
  frameIndex: number
  currencyId: string
  consumedOmenIds: string[]
  omensKnown: boolean
  uses: number
  repeated: boolean
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

/** Only a successful, receipted transition from a hidden slot to a known modifier is a reveal. */
function completedReveal(frame: Frame, before: Frame | undefined): boolean {
  if (
    !before ||
    ![revealAction, 'ABYSS_SELECT', 'ABYSS_CHOOSE'].includes(
      frame.action ?? '',
    ) ||
    !receipt(frame)
  )
    return false
  if (
    !Array.isArray(frame.state.desecrated) ||
    !Array.isArray(before.state.desecrated) ||
    !Array.isArray(frame.state.explicits)
  )
    return false
  return !!frame.state.desecrated.some(
    (slot) =>
      !!slot &&
      typeof slot.modifierId === 'string' &&
      before.state.desecrated?.some(
        (previous) => previous.id === slot.id && previous.modifierId === null,
      ) &&
      frame.state.explicits.some(
        (modifier) => modifier.modifierId === slot.modifierId,
      ),
  )
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
    if (!validChaosRepeatFrame(frame)) {
      result.omittedFrames++
      continue
    }
    const reveal = completedReveal(frame, film.frames[frameIndex - 1])
    if (frame.action === null || (!reveal && isObservation(frame.action)))
      continue
    const previous = film.frames[frameIndex - 1]?.state.installedAugments ?? []
    const changed = (frame.state.installedAugments ?? []).filter(
      (a) =>
        !previous.some(
          (p) => p.slotIndex === a.slotIndex && p.augmentId === a.augmentId,
        ),
    )
    const id = reveal
      ? revealAction
      : frame.action === 'INSTALL_AUGMENT' &&
          frame.evidence?.ruleVersion === 'socket-augments-v1' &&
          changed.length === 1 &&
          augments.some((a) => a.id === changed[0]!.augmentId)
        ? changed[0]!.augmentId
        : currencyId(frame.action)
    const recorded = receipt(frame)
    if (!id || !recorded) {
      result.omittedFrames++
      continue
    }
    const consumed: unknown = recorded.consumedOmens
    const validArray =
      Array.isArray(consumed) &&
      consumed.every((id): id is string => typeof id === 'string') &&
      (frame.chaosRepeat !== undefined ||
        new Set(consumed).size === consumed.length)
    const consumedOmenIds = validArray ? [...consumed] : []
    const omensKnown =
      validArray && consumedOmenIds.every((id) => omenIds.has(id))
    result.incompleteOmens ||= !omensKnown
    result.steps.push({
      ...(reveal ? { kind: 'reveal' as const } : {}),
      number: result.steps.length + 1,
      frameIndex,
      currencyId: id,
      consumedOmenIds,
      omensKnown,
      uses: frame.chaosRepeat?.uses ?? 1,
      repeated: frame.chaosRepeat !== undefined,
    })
  }
  return result
}
