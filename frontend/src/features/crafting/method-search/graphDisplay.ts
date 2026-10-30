import type { AutomaticNode } from './automaticApi'
import type { Catalog, GoalFilter } from '../goal-filter/types'
import type { Definition } from '../craftingApi'
import type { ItemCardData } from '../itemCardData'
import type { Locale } from '../../../shared/i18n/i18n'
import { canonical } from '../path-tree/validation'

export function goalSources(
  goal: GoalFilter,
  catalog: Catalog,
): string[] | undefined {
  const result = new Set<string>(),
    visited = new Set<string>()
  function visit(id: string): boolean {
    if (visited.has(id)) return false
    visited.add(id)
    const stat = catalog.stats.find((s) => s.statId === id)
    if (!stat) return false
    if (stat.kind === 'PSEUDO') {
      if (!stat.contributions.length) return false
      for (const source of stat.contributions)
        if (!visit(source.statId)) return false
    } else {
      if (!stat.sourceStatIds.length) return false
      stat.sourceStatIds.forEach((s) => result.add(s))
    }
    visited.delete(id)
    return true
  }
  for (const group of goal.groups.filter((g) => !g.disabled))
    for (const entry of group.entries.filter((e) => !e.disabled))
      if (!visit(entry.statId)) return undefined
  return [...result]
}
export type JunkClassifier = (
  node: AutomaticNode,
  id: string,
) => 'PREFIX' | 'SUFFIX' | undefined
export function junkClassifier(
  sources: string[] | undefined,
  definitions: Record<string, Definition>,
): JunkClassifier {
  const targets = new Set(sources)
  return (node, id) => {
    if (!sources || node.item.desecrated?.some((s) => s.modifierId === id))
      return undefined
    const m = node.item.explicits.find((m) => m.modifierId === id),
      d = definitions[id]
    if (!m || !d || (d.affixType !== 'PREFIX' && d.affixType !== 'SUFFIX'))
      return undefined
    const stats = [
      ...Object.keys(m.values),
      ...(node.attributes?.[id]?.map((v) => v.stat) ?? []),
    ]
    if (!stats.length || stats.some((s) => targets.has(s))) return undefined
    return d.affixType
  }
}
const labels: Record<Locale, string> = {
  en: 'Non-target modifier (junk)',
  ko: '비유효옵(잡옵)',
  'zh-CN': '非目标词缀（杂项）',
  'zh-TW': '非目標詞綴（雜項）',
  ja: '非目標モッド（不要）',
  es: 'Modificador no objetivo (relleno)',
}
export function junkCard(
  node: AutomaticNode,
  card: ItemCardData,
  junk: JunkClassifier,
  locale: Locale,
): ItemCardData {
  const counts = new Map<string, number>()
  const modifiers = card.modifiers.filter((line) => {
    const side = junk(node, line.id)
    if (!side) return true
    const key = side + (line.fractured ? ':fractured' : '')
    counts.set(key, (counts.get(key) ?? 0) + 1)
    return false
  })
  for (const [key, count] of counts)
    modifiers.push({
      id: 'junk:' + key,
      kind: 'explicit',
      fractured: key.endsWith(':fractured'),
      affixLabel: key.startsWith('PREFIX') ? 'P' : 'S',
      text: `${labels[locale]} ×${count}`,
    })
  return { ...card, modifiers }
}
/** Display candidates only. Original states, target predicates and transitions stay untouched. */
export function tierCandidates(nodes: AutomaticNode[], junk?: JunkClassifier) {
  const groups = new Map<string, AutomaticNode[]>()
  for (const n of nodes) {
    const key =
      canonical(n.item.desecrated ?? []) +
      canonical(
        n.item.explicits
          .map((m) => ({
            modifier: junk?.(n, m.modifierId) ?? m.modifierId,
            fractured: m.fractured,
            // Distribution support is not interchangeable with a concrete roll.
            support: junk?.(n, m.modifierId)
              ? null
              : (n.attributes?.[m.modifierId]?.map((v) => [
                  v.modifierId,
                  v.stat,
                  v.tier,
                  v.role,
                ]) ??
                n.tiers?.[m.modifierId]?.variants.map((v) => [
                  v.modifierId,
                  v.stat,
                ]) ??
                (n.rolls?.[m.modifierId] ? 'roll' : null)),
          }))
          .sort((a, b) => canonical(a).localeCompare(canonical(b))),
      )
    groups.set(key, [...(groups.get(key) ?? []), n])
  }
  return [...groups.values()]
}
