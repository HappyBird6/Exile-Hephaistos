import type { AttributeRendering } from './compactAttribute'
import { probabilityText } from './probabilityPresentation'

/** Only contiguous scalar rolls of the same catalog modifier/tier are combined. */
export function mergeTierRenderings(
  variants: (AttributeRendering & { groupKey: string })[],
): AttributeRendering[] {
  const result: (AttributeRendering & { groupKey: string })[] = []
  const number = /[+-]?\d[\d,.]*/g
  const scalar = (text: string) => {
    const matches = [...text.matchAll(number)]
    return matches.length === 1 && /^[+-]?\d+$/.test(matches[0]![0])
      ? Number(matches[0]![0].replaceAll(',', ''))
      : NaN
  }
  const order = [...new Set(variants.map((v) => v.groupKey))]
  for (const v of [...variants].sort(
    (a, b) =>
      order.indexOf(a.groupKey) - order.indexOf(b.groupKey) ||
      scalar(a.low) - scalar(b.low),
  )) {
    const p = result.at(-1)
    if (
      !p ||
      p.groupKey !== v.groupKey ||
      p.label !== v.label ||
      p.low.replace(number, '#') !== v.low.replace(number, '#') ||
      !Number.isFinite(scalar(p.high)) ||
      scalar(v.low) > scalar(p.high) + 1 ||
      scalar(v.low) <= scalar(p.high)
    ) {
      result.push({ ...v })
      continue
    }
    p.high = v.high
    if (p.chanceKey && v.chanceKey) {
      const a = JSON.parse(p.chanceKey),
        b = JSON.parse(v.chanceKey)
      const n =
          BigInt(a.numerator) * BigInt(b.denominator) +
          BigInt(b.numerator) * BigInt(a.denominator),
        d = BigInt(a.denominator) * BigInt(b.denominator)
      const gcd = (x: bigint, y: bigint): bigint => (y ? gcd(y, x % y) : x)
      const g = gcd(n, d),
        weight = { numerator: String(n / g), denominator: String(d / g) }
      p.chanceKey = JSON.stringify(weight)
      p.chance = probabilityText(weight)
    } else {
      delete p.chance
      delete p.chanceKey
    }
  }
  return result
}
