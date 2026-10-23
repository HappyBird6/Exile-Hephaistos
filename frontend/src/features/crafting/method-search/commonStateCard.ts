import {
  compactAttribute,
  tierLabel,
  attributeHeading,
} from './compactAttribute'
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
        attribute: line.attributeKey ?? attributeHeading(line.text),
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
      ...[...groups.values()].map((lines, i) => {
        const combined = { ...lines[0]!, id: `candidates:${i}` }
        if (lines.some((l) => l.tierRows?.length)) {
          const variants = lines.flatMap((l) =>
            l.tierRows?.length
              ? l.tierRows.map((r) => ({
                  label: r.label,
                  low: r.low ?? l.text,
                  high: r.high ?? l.text,
                }))
              : [{ label: tierLabel(l.affixLabel), low: l.text, high: l.text }],
          )
          const compacted = compactAttribute(combined, variants)
          if (compacted !== combined) return compacted
          const rows = lines.flatMap((l) =>
            l.tierRows?.length
              ? l.tierRows.map((row) => ({
                  label: row.label,
                  value: row.value,
                  ...(row.low ? { low: row.low } : {}),
                  ...(row.high ? { high: row.high } : {}),
                }))
              : [
                  {
                    label: tierLabel(l.affixLabel),
                    value: (
                      l.text.match(
                        /[+-]?\d[\d,.]*(?:[–~?][+-]?\d[\d,.]*)?\s*%?/g,
                      ) ?? []
                    ).join(' / '),
                    low: l.text,
                    high: l.text,
                  },
                ],
          )
          return {
            ...combined,
            tierRows: rows.filter(
              (r, i) =>
                rows.findIndex(
                  (o) => o.label === r.label && o.value === r.value,
                ) === i,
            ),
          }
        }
        return compactAttribute(
          combined,
          lines.map((l) => ({
            label: tierLabel(l.affixLabel),
            low: l.text,
            high: l.text,
          })),
        )
      }),
    ],
  }
}
