import type { Definition, Outcome } from './craftingApi'

export function modifierSummary(text: string, stats?: Definition['stats']) {
  // Fixed-value tiers can be normalized only when every number in the text
  // is accounted for by a declared fixed stat. Preserve unrelated constants.
  const numbers = [...text.matchAll(/\d+(?:\.\d+)?/g)].map((m) => Number(m[0]))
  if (
    stats?.length &&
    stats.every((s) => s.min === s.max) &&
    numbers.length === stats.length &&
    numbers.every((n, i) => n === Math.abs(stats[i]!.min))
  ) {
    return text.replace(/\d+(?:\.\d+)?/g, '#')
  }
  return text.replace(
    /\([+-]?\d+(?:\.\d+)?[?\u2013\u2014-][+-]?\d+(?:\.\d+)?\)/g,
    '#',
  )
}

// Only aggregate one conditional transition distribution. Missing identity
// metadata keeps a definition separate rather than guessing from its label.
function modifierKey(id: string, definitions: Record<string, Definition>) {
  const d = definitions[id]
  if (!d?.stats?.length || !d.familyIds.length || !d.layer || !d.tags) return id
  if (
    !d.stats.every(
      (s) =>
        typeof s.id === 'string' &&
        Number.isFinite(s.min) &&
        Number.isFinite(s.max),
    )
  )
    return id
  return JSON.stringify([
    d.layer,
    d.affixType,
    [...d.familyIds].sort(),
    d.stats.map((s) => s.id).sort(),
    [...d.tags].sort(),
    modifierSummary(d.text, d.stats),
  ])
}

export function groupOutcomes(
  outcomes: Outcome[],
  definitions: Record<string, Definition>,
) {
  const groups = new Map<
    string,
    { key: string; probability: number; outcomes: Outcome[] }
  >()
  for (const outcome of outcomes) {
    const { modifierIds, ...context } = outcome.state
    const key = JSON.stringify([
      context,
      modifierIds.map((id) => modifierKey(id, definitions)).sort(),
    ])
    const group = groups.get(key)
    if (group) {
      group.probability += outcome.probability
      group.outcomes.push(outcome)
    } else
      groups.set(key, {
        key,
        probability: outcome.probability,
        outcomes: [outcome],
      })
  }
  return [...groups.values()]
}
