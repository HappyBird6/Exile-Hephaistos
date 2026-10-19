import type { ItemCardData } from '../itemCardData'
import { canonical } from '../path-tree/validation'
// A common card is explicitly a set of candidates, not a fabricated rolled item.
export function commonStateCard(cards: ItemCardData[]): ItemCardData {
  const first = cards[0]!
  const common = first.modifiers.filter((line) =>
    cards.every((card) =>
      card.modifiers.some((other) => canonical(other) === canonical(line)),
    ),
  )
  const groups = new Map<string, ItemCardData['modifiers'][number][]>()
  for (const card of cards)
    for (const line of card.modifiers) {
      if (common.some((c) => canonical(c) === canonical(line))) continue
      const key = canonical({
        kind: line.kind ?? null,
        fractured: line.fractured ?? false,
        text: line.text.replace(/[+-]?\d[\d,.]*/g, '#'),
      })
      const values = groups.get(key) ?? []
      if (!values.some((v) => canonical(v) === canonical(line)))
        values.push(line)
      groups.set(key, values)
    }
  return {
    ...first,
    modifiers: [
      ...common,
      ...[...groups.values()].map((lines, i) => ({
        id: `candidates:${i}`,
        kind: lines[0]!.kind ?? 'unknown',
        text: lines[0]!.text.replace(
          /[+-]?\d[\d,.]*(?:[~–—][+-]?\d[\d,.]*)?/g,
          '…',
        ),
        detail: lines
          .map((line) => `${line.affixLabel ?? ''} ${line.text}`)
          .join('\n'),
      })),
    ],
  }
}
