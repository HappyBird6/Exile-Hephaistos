import type { PathTreeProps } from '../path-tree/PathTree'
import type { Item } from '../path-tree/types'
import type { RollBands, TierVariants } from './types'

export function rangeText(low: string, high: string) {
  if (low === high) return low
  const pattern = /[+-]?\d[\d,.]*/g
  const a = low.match(pattern) ?? []
  const b = high.match(pattern) ?? []
  if (
    a.length !== b.length ||
    low.replace(pattern, '#') !== high.replace(pattern, '#')
  )
    return `${low} – ${high}`
  let index = 0
  return low.replace(pattern, () => {
    const i = index++
    return a[i] === b[i] ? a[i]! : `${a[i]}–${b[i]}`
  })
}

export function presentRollCell(
  item: Item,
  bands: RollBands | undefined,
  locale: Parameters<PathTreeProps['presentItem']>[1],
  present: PathTreeProps['presentItem'],
  tiers?: TierVariants,
) {
  const low = present(item, locale)
  if (
    (!bands || Object.keys(bands).length === 0) &&
    (!tiers || Object.keys(tiers).length === 0)
  )
    return low
  const high = present(
    {
      ...item,
      explicits: item.explicits.map((modifier) => {
        const band = bands?.[modifier.modifierId]
        return band
          ? {
              ...modifier,
              values: { ...modifier.values, [band.stat]: band.max },
            }
          : modifier
      }),
    },
    locale,
  )
  const upper = new Map(high.modifiers.map((line) => [line.id, line.text]))
  return {
    ...low,
    modifiers: low.modifiers.map((line) => {
      const variants = tiers?.[line.id]
      if (!variants)
        return {
          ...line,
          text: rangeText(line.text, upper.get(line.id) ?? line.text),
        }
      const texts = variants.map((variant) => {
        const render = (value: number) =>
          present(
            {
              ...item,
              explicits: item.explicits.map((m) =>
                m.modifierId === line.id
                  ? {
                      ...m,
                      modifierId: variant.modifierId,
                      values: { [variant.stat]: value },
                    }
                  : m,
              ),
            },
            locale,
          ).modifiers.find((m) => m.id === variant.modifierId)?.text ??
          line.text
        return rangeText(render(variant.min), render(variant.max))
      })
      return { ...line, text: [...new Set(texts)].join(' / ') }
    }),
  }
}
