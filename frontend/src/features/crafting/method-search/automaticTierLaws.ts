import type { AutomaticNode } from './automaticApi'
import type { PathTreeProps } from '../path-tree/PathTree'
import { presentRollCell } from './rollPresentation'
import { compactAttribute } from './compactAttribute'
import { jointAttributeRenderings } from './jointAttributePresentation'
import { probabilityText } from './probabilityPresentation'

export function validateTierLaws(node: AutomaticNode) {
  if (node.tiers === undefined) return
  if (
    !node.tiers ||
    typeof node.tiers !== 'object' ||
    Array.isArray(node.tiers)
  )
    throw new Error('Invalid tier law')
  for (const [id, law] of Object.entries(node.tiers)) {
    const modifier = node.item.explicits.find((m) => m.modifierId === id)
    if (
      !modifier ||
      !law ||
      !Array.isArray(law.variants) ||
      !law.variants.length ||
      law.variants.length > 4096
    )
      throw new Error('Invalid tier law')
    let numerator = 0n,
      denominator = 1n
    const seen = new Set<string>()
    for (const v of law.variants) {
      if (
        !v ||
        typeof v.modifierId !== 'string' ||
        !v.modifierId ||
        typeof v.stat !== 'string' ||
        !v.stat ||
        !Number.isSafeInteger(v.min) ||
        !Number.isSafeInteger(v.max) ||
        v.min > v.max ||
        !v.probability ||
        typeof v.probability.numerator !== 'string' ||
        typeof v.probability.denominator !== 'string' ||
        !/^[1-9]\d{0,4095}$/.test(v.probability.numerator) ||
        !/^[1-9]\d{0,4095}$/.test(v.probability.denominator)
      )
        throw new Error('Invalid tier law')
      const key = JSON.stringify([v.modifierId, v.min, v.max])
      if (seen.has(key)) throw new Error('Duplicate tier interval')
      seen.add(key)
      const n = BigInt(v.probability.numerator),
        d = BigInt(v.probability.denominator)
      numerator = numerator * d + n * denominator
      denominator *= d
      const gcd = (a: bigint, b: bigint): bigint => (b ? gcd(b, a % b) : a)
      const factor = gcd(numerator, denominator)
      numerator /= factor
      denominator /= factor
    }
    if (numerator !== denominator)
      throw new Error('Tier law must conserve mass')
    const first = law.variants[0]!
    if (first.modifierId !== id || modifier.values[first.stat] !== first.min)
      throw new Error('Invalid tier representative')
  }
}

/** An origin is provenance, not by itself uncertainty about the actual item. */
export function automaticItemIsDistribution(node: AutomaticNode) {
  if (Object.keys(node.attributes ?? {}).length)
    return Object.values(node.attributes!).some((options) => {
      const fields = new Map<string, Set<string>>()
      for (const option of options) {
        if (option.min !== option.max) return true
        const values = fields.get(option.stat) ?? new Set<string>()
        values.add(JSON.stringify([option.modifierId, option.min]))
        fields.set(option.stat, values)
      }
      return [...fields.values()].some((values) => values.size > 1)
    })
  return (
    Object.keys(node.tiers ?? {}).length > 0 ||
    Object.keys(node.rolls ?? {}).length > 0 ||
    (node.joint?.length ?? 0) > 0
  )
}

export function presentAutomaticItem(
  node: AutomaticNode,
  locale: Parameters<PathTreeProps['presentItem']>[1],
  present: PathTreeProps['presentItem'],
  nodes: AutomaticNode[] = [node],
): ReturnType<PathTreeProps['presentItem']> {
  const base = present(node.item, locale)
  const support =
    node.attributes ??
    Object.fromEntries(
      Object.entries(node.tiers ?? {}).map(([id, law]) => [
        id,
        law.variants.map((v) => ({ ...v, tier: 0 })),
      ]),
    )
  if (!Object.keys(support).length)
    return presentRollCell(node.item, node.rolls, locale, present)
  return {
    ...base,
    modifiers: base.modifiers.map((line) => {
      const options = support[line.id]
        ?.slice()
        .sort(
          (a, b) =>
            a.tier - b.tier ||
            a.min - b.min ||
            a.max - b.max ||
            a.modifierId.localeCompare(b.modifierId),
        )
      if (!options?.length) return line
      const primary = node.item.explicits.find((m) => m.modifierId === line.id)
      if (primary && Object.keys(primary.values).length > 1)
        return compactAttribute(
          {
            ...line,
            ...(options[0] && 'role' in options[0]
              ? { attributeKey: options[0].role as string }
              : {}),
          },
          jointAttributeRenderings(node, line.id, nodes, locale, present),
          locale,
        )
      const conditional =
        (node.members && node.members.length > 1) ||
        node.joint?.some((o) => Object.hasOwn(o.retained, line.id))
      const variants = options.map((v) => {
        const render = (value: number) =>
          present(
            {
              ...node.item,
              explicits: node.item.explicits.map((m) =>
                m.modifierId === line.id
                  ? {
                      ...m,
                      modifierId: v.modifierId,
                      values: { ...m.values, [v.stat]: value },
                    }
                  : m,
              ),
            },
            locale,
          ).modifiers.find((m) => m.id === v.modifierId)
        const low = render(v.min),
          high = render(v.max)
        const weight = !conditional
          ? node.tiers?.[line.id]?.variants.find(
              (t) =>
                t.modifierId === v.modifierId &&
                t.min === v.min &&
                t.max === v.max,
            )?.probability
          : undefined
        return {
          label: v.tier ? `T${v.tier}` : (low?.affixLabel ?? '—'),
          low: low?.text ?? line.text,
          high: high?.text ?? line.text,
          ...(weight
            ? {
                chance: probabilityText(weight),
                chanceKey: JSON.stringify(weight),
              }
            : {}),
        }
      })
      return compactAttribute(
        {
          ...line,
          ...(options[0] && 'role' in options[0]
            ? { attributeKey: options[0].role as string }
            : {}),
        },
        variants,
        locale,
      )
    }),
  }
}

export const distributionEditMessages = {
  en: 'This state represents several possible tiers. Choose a concrete item in the Workbench to edit.',
  ko: '이 상태는 여러 티어의 확률분포입니다. 작업대에서 실제 티어를 선택한 아이템을 편집하세요.',
  'zh-CN': '此状态包含多个可能阶级。请在工作台选择具体物品后编辑。',
  'zh-TW': '此狀態包含多個可能階級。請在工作台選擇具體物品後編輯。',
  ja: 'この状態は複数のティアの分布です。編集するには作業台で具体的なアイテムを選んでください。',
  es: 'Este estado representa varios niveles posibles. Elige un objeto concreto en el taller para editarlo.',
} as const
