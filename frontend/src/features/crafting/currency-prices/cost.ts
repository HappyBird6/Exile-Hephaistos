import { isBone } from '../desecrationCurrency'
import { costAccumulator } from './decimalAmount'
import { boundedChaosCurrency, type Film } from '../workbenchHistory'
import {
  extractSessionNotation,
  type NotationStep,
} from '../session-notation/extractSessionNotation'
import { currencyNames } from '../currencyNames'
import { materialNames } from '../catalogData'
import type { CurrencyQuote, PriceSnapshot } from './api'
const names: Record<string, string> = currencyNames
export function currencyQuote(
  snapshot: PriceSnapshot | null | undefined,
  id: string,
): CurrencyQuote | undefined {
  const name = names[id] ?? materialNames[id]
  return name && snapshot && Object.hasOwn(snapshot.prices, name)
    ? snapshot.prices[name]
    : undefined
}
export type CraftingCost = {
  divine: number
  exalted: number
  unknown: string[]
  incomplete: boolean
  hasCosts: boolean
  unknownUses?: boolean
  /** 쓰지 못하고 중단할 수 있어 총량 대신 재시도 1회의 재료만 확정된 단계들. 숨은 시작의 뼈 종류는 null이다. */
  retryMaterials?: {
    currencyId: string
    omenIds: string[]
    bone?: string | null
  }[]
}
/** The current cursor is the paid path. Reveal observations cost no currency. */
export function craftingCost(
  film: Film | undefined,
  cursor: number,
  snapshot: PriceSnapshot | null | undefined,
): CraftingCost {
  const visible = film
    ? { ...film, frames: film.frames.slice(0, cursor + 1) }
    : undefined
  const notation = extractSessionNotation(visible, cursor)
  return craftingStepCost(
    notation.steps,
    snapshot,
    notation.omittedFrames > 0 || notation.incompleteOmens,
  )
}

/** Sum the currencies and omens on either a recorded history or a verified route. */
export function craftingStepCost(
  steps: Iterable<
    Pick<
      NotationStep,
      | 'currencyId'
      | 'consumedOmenIds'
      | 'uses'
      | 'expectedUses'
      | 'omensPerUse'
      | 'maxUses'
      | 'kind'
      | 'work'
    >
  >,
  snapshot: PriceSnapshot | null | undefined,
  incomplete = false,
): CraftingCost {
  const divine = costAccumulator(),
    exalted = costAccumulator()
  const unknown = new Set<string>()
  let hasCosts = false
  let unknownRepeatCount = false
  let lastBone: string | null = null
  const retryMaterials: NonNullable<CraftingCost['retryMaterials']> = []
  for (const step of steps) {
    if (step.kind === 'reveal') {
      if (step.maxUses !== undefined && step.maxUses > 1) {
        retryMaterials.push({
          currencyId: 'Orb_of_Annulment',
          omenIds: ['Omen_of_Light'],
          bone: lastBone,
        })
        unknownRepeatCount = true
        hasCosts = true
      }
      lastBone = null
      continue
    }
    lastBone = isBone(step.currencyId) ? step.currencyId : null
    if (step.work) continue
    hasCosts = true
    unknownRepeatCount ||=
      (step.maxUses !== undefined && step.expectedUses == null) ||
      step.expectedUses === null
    if (
      step.expectedUses === null &&
      boundedChaosCurrency(step.currencyId, step.maxUses)
    ) {
      retryMaterials.push({
        currencyId: step.currencyId,
        omenIds: step.consumedOmenIds,
      })
      continue
    }
    const uses = step.expectedUses ?? step.uses
    const consumed = [
      [step.currencyId, uses] as const,
      ...step.consumedOmenIds.map(
        (id) => [id, step.omensPerUse ? uses : 1] as const,
      ),
    ]
    for (const [id, count] of consumed) {
      const quote = currencyQuote(snapshot, id)
      if (!quote) unknown.add(id)
      else {
        divine.add(quote.divine, count)
        exalted.add(quote.exalted, count)
      }
    }
  }
  return {
    divine: divine.total(),
    exalted: exalted.total(),
    unknown: [...unknown],
    incomplete: incomplete || unknownRepeatCount || unknown.size > 0,
    hasCosts,
    ...(unknownRepeatCount ? { unknownUses: true } : {}),
    ...(retryMaterials.length ? { retryMaterials } : {}),
  }
}
