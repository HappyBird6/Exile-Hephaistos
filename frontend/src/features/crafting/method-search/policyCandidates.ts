import type { AutomaticView } from './automaticApi'
import type { Bounds } from './types'
export type RareOutcomePolicy = {
  version: 'rare-conditional-groups-v1'
  enabled: boolean
  groupPartsPerMillion: number
  maximumMethodPartsPerMillion: number
}
// New-session preference. Older saved requests keep the server's disabled compatibility default.
export const defaultRareOutcomePolicy: RareOutcomePolicy = {
  version: 'rare-conditional-groups-v1',
  enabled: true,
  groupPartsPerMillion: 10000,
  maximumMethodPartsPerMillion: 50000,
}
export type PolicyCandidate = {
  id: string
  cycles: number
  decisions: { node: number; method: string }[]
  success: Bounds
  rebase: Bounds
  failure: Bounds
  unresolved: Bounds
  expectedUses: Record<
    string,
    { lower: Bounds['lower']; upper: Bounds['upper'] | null }
  >
  usageStatus: 'BOUNDED' | 'LOWER_BOUND_ONLY'
  rankStatus: 'OVERLAPPING' | 'SEPARATED_IN_CURRENT_SET'
  distinguishingDecisions: { node: number; method: string }[]
}
export function isRareOutcomePolicy(
  value: unknown,
): value is RareOutcomePolicy {
  const p = value as RareOutcomePolicy
  return (
    !!p &&
    p.version === 'rare-conditional-groups-v1' &&
    typeof p.enabled === 'boolean' &&
    [p.groupPartsPerMillion, p.maximumMethodPartsPerMillion].every(
      (v) => Number.isSafeInteger(v) && v >= 0 && v <= 1000000,
    )
  )
}
export function validatePolicies(
  view: AutomaticView,
  bound: (b: Bounds) => void,
) {
  const fail = () => {
    throw new Error('Invalid session policy')
  }
  const policy = view.rareOutcomePolicy
  if (policy !== undefined) {
    if (
      !policy ||
      policy.version !== 'rare-conditional-groups-v1' ||
      typeof policy.enabled !== 'boolean'
    )
      fail()
    for (const value of [
      policy.groupPartsPerMillion,
      policy.maximumMethodPartsPerMillion,
    ])
      if (!Number.isSafeInteger(value) || value < 0 || value > 1000000) fail()
  }
  if (view.candidates === undefined) return
  if (!Array.isArray(view.candidates) || view.candidates.length > 10) fail()
  const ids = new Set<string>()
  for (const c of view.candidates) {
    if (
      !c ||
      typeof c.id !== 'string' ||
      c.id.length > 128 ||
      ids.has(c.id) ||
      c.cycles !== 100
    )
      fail()
    ids.add(c.id)
    for (const value of [c.success, c.rebase, c.failure, c.unresolved])
      bound(value)
    if (
      !['BOUNDED', 'LOWER_BOUND_ONLY'].includes(c.usageStatus) ||
      !['OVERLAPPING', 'SEPARATED_IN_CURRENT_SET'].includes(c.rankStatus)
    )
      fail()
    if (
      !Array.isArray(c.decisions) ||
      c.decisions.length > view.nodes.length ||
      !Array.isArray(c.distinguishingDecisions)
    )
      fail()
    const nodes = new Set<number>()
    for (const d of c.decisions) {
      if (
        nodes.has(d.node) ||
        !view.methods.some((m) => m.id === d.method && m.source === d.node)
      )
        fail()
      nodes.add(d.node)
    }
    for (const d of c.distinguishingDecisions)
      if (!c.decisions.some((v) => v.node === d.node && v.method === d.method))
        fail()
    if (
      !c.expectedUses ||
      Array.isArray(c.expectedUses) ||
      Object.keys(c.expectedUses).length > 256
    )
      fail()
    for (const use of Object.values(c.expectedUses)) {
      if (!use || (c.usageStatus === 'BOUNDED' && !use.upper)) fail()
      for (const value of [use.lower, use.upper].filter((v) => v !== null)) {
        if (
          !value ||
          !/^\d{1,256}$/.test(value.numerator) ||
          !/^\d{1,256}$/.test(value.denominator) ||
          BigInt(value.denominator) === 0n
        )
          fail()
      }
      if (
        use.upper &&
        BigInt(use.lower.numerator) * BigInt(use.upper.denominator) >
          BigInt(use.upper.numerator) * BigInt(use.lower.denominator)
      )
        fail()
    }
  }
}
