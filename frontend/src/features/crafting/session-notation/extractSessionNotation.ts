import { revealAction } from '../abyss-workbench/revealAction'
import { augments } from '../socketAugments'
import { isBone } from '../desecrationCurrency'
import { workbenchCurrencyActions, workbenchOmens } from '../workbenchApi'
import {
  validProjectedCurrencies,
  type Film,
  type Frame,
} from '../workbenchHistory'
import { validChaosRepeatFrame } from '../chaos-repeat/chaosRepeatReceipt'

export type NotationStep = {
  repeatUntilHit?: true
  omensPerUse?: boolean
  expectedUses?: number | null
  maxUses?: number
  customExalt?: { modifierId: string; probability: number }
  work?: Frame['projectedWork']
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

export function currencyId(action: string): string | undefined {
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
 * A branch is a separate Film and retains the selected prefix before adding new steps.
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
    if (
      frame.action === 'SESSION_PLAN' &&
      validProjectedCurrencies(frame.projectedCurrencies)
    ) {
      for (const craft of frame.projectedCurrencies) {
        result.steps.push({
          number: result.steps.length + 1,
          frameIndex,
          currencyId: craft.currencyId,
          consumedOmenIds: craft.omenIds,
          omensKnown: true,
          uses: 1,
          repeated: false,
          ...(craft.expectedUses !== undefined
            ? { expectedUses: craft.expectedUses }
            : {}),
          ...(craft.maxUses ? { maxUses: craft.maxUses } : {}),
          ...(craft.repeatUntilHit ? { repeatUntilHit: true } : {}),
          ...(craft.omensPerUse ? { omensPerUse: true } : {}),
        })
      }
      if (frame.projectedWork)
        result.steps.push({
          number: result.steps.length + 1,
          frameIndex,
          work: frame.projectedWork,
          currencyId: '',
          uses: 0,
          consumedOmenIds: [],
          omensKnown: true,
          repeated: false,
        })
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
      ...(reveal && frame.projectedRevealLimit
        ? { maxUses: frame.projectedRevealLimit, expectedUses: null }
        : {}),
      ...(frame.customExalt &&
      ['EXALTED', 'GREATER_EXALTED', 'PERFECT_EXALTED'].includes(
        frame.action ?? '',
      ) &&
      Number.isFinite(frame.customExalt.probability) &&
      frame.customExalt.probability > 0 &&
      frame.customExalt.probability <= 1 &&
      frame.state.explicits.some(
        (m) => m.modifierId === frame.customExalt!.modifierId,
      ) &&
      !film.frames[frameIndex - 1]!.state.explicits.some(
        (m) => m.modifierId === frame.customExalt!.modifierId,
      )
        ? { customExalt: frame.customExalt }
        : {}),
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
