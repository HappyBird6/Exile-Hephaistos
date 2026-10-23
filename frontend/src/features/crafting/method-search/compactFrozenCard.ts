import type { ItemCardData } from '../itemCardData'
import { attributeHeading } from './compactAttribute'
const numeric = /[+-]?\d[\d,.]*(?:[–~?][+-]?\d[\d,.]*)?\s*%?/g
/** Reformat immutable historical text using only its own rendered strings. */
export function compactFrozenCard(card: ItemCardData): ItemCardData {
  return {
    ...card,
    modifiers: card.modifiers.map((line) => {
      if (line.tierRows?.length) return line
      const parts = line.text.split(' / ')
      if (parts.length < 2 || !parts[0]) return line
      const skeleton = parts[0].replace(numeric, '#')
      if (parts.some((p) => p.replace(numeric, '#') !== skeleton)) return line
      const labels = (line.detail ?? '')
        .split('\n')
        .map(
          (row) =>
            row.match(/^([PS]\d+|T\d+)\b/)?.[1]?.replace(/^[PS]/, 'T') ?? '—',
        )
      const rows = parts.map((part, i) => ({
        label: labels[i] ?? '—',
        value: (part.match(numeric) ?? []).map((v) => v.trim()).join(' / '),
        low: part,
        high: part,
      }))
      if (rows.some((r) => !r.value)) return line
      const title = attributeHeading(parts[0])
        .replace(/[–~?]/g, '')
        .replace(/\s+/g, ' ')
        .trim()
      return title
        ? { ...line, text: title, tierRows: rows, affixLabel: undefined }
        : line
    }),
  }
}
