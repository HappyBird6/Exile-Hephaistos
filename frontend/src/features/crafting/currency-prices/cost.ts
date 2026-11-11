import type { Film } from '../workbenchHistory'
import { extractSessionNotation } from '../session-notation/extractSessionNotation'
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
}
/** The current cursor is the paid path. Reveal observations cost no currency. */
export function craftingCost(
  film: Film | undefined,
  cursor: number,
  snapshot: PriceSnapshot | null | undefined,
): CraftingCost {
  let divine = 0,
    exalted = 0
  const unknown = new Set<string>()
  const visible = film
    ? { ...film, frames: film.frames.slice(0, cursor + 1) }
    : undefined
  const notation = extractSessionNotation(visible, cursor)
  let hasCosts = false
  for (const step of notation.steps) {
    if (step.frameIndex > cursor || step.kind === 'reveal') continue
    hasCosts = true
    const consumed = [
      [step.currencyId, step.uses] as const,
      ...step.consumedOmenIds.map((id) => [id, 1] as const),
    ]
    for (const [id, count] of consumed) {
      const quote = currencyQuote(snapshot, id)
      if (!quote) unknown.add(id)
      else {
        divine += quote.divine * count
        exalted += quote.exalted * count
      }
    }
  }
  return {
    divine,
    exalted,
    unknown: [...unknown],
    incomplete:
      unknown.size > 0 ||
      notation.omittedFrames > 0 ||
      notation.incompleteOmens,
    hasCosts,
  }
}
