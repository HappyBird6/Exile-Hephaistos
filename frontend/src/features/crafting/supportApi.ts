import type { Bucket } from './craftingApi'
import { baseWorkbenchAction } from './workbenchApi'
import type { WorkbenchAction } from './workbenchApi'
export interface GoalCondition {
  family: string
  minimumTier: number
}
export interface SupportGoal {
  required: GoalCondition[]
  candidates: GoalCondition[]
  candidateCount: number
}
export interface SupportFamily {
  id: string
  affix: 'PREFIX' | 'SUFFIX'
  tiers: {
    tier: number
    requiredItemLevel: number
    exampleText: string
    modifierId: string
  }[]
}
export interface GoalAssessment {
  status: 'READY' | 'ACHIEVED' | 'IMPOSSIBLE' | 'INVALID_GOAL'
  valid: boolean
  achieved: boolean
  feasible: boolean
  requiredMatched: number
  candidatesMatched: number
  matches: Record<string, boolean>
  issues: string[]
}
export interface SupportComparison {
  sequence: WorkbenchAction[]
  successLower: number
  successUpper: number
  failureProbability: number
  unresolvedProbability: number
  complete: boolean
  steps: {
    step: number
    action: WorkbenchAction | null
    firstHitProbability: number
    blockedProbability: number
    continuingOrUnresolvedProbability: number
    blockedReasons: string[]
  }[]
}
export interface SupportReport {
  ruleVersion: string
  ledgerVersion: string
  transitionNamespace: string
  assessment: GoalAssessment
  comparisons: SupportComparison[]
  complete: boolean
  rankingCertified: boolean
  comparedSequences: number
  totalSequences: number
  expandedStates: number
  expandedEdges: number
  elapsedMillis: number
  cache: { memoryHits: number; persistedHits: number; computedPools: number }
}
const probability = (value: unknown) =>
  typeof value === 'number' &&
  Number.isFinite(value) &&
  value >= -1e-10 &&
  value <= 1 + 1e-8
const addition = (value: unknown) =>
  typeof value === 'string' &&
  /^(?:(?:GREATER|PERFECT)_)?(?:TRANSMUTATION|AUGMENTATION|REGAL|EXALTED)$/.test(
    value,
  ) &&
  !['CHAOS', 'ANNULMENT', 'DIVINE'].includes(
    baseWorkbenchAction(value as WorkbenchAction),
  )
export async function recommendGoal(
  state: Bucket,
  goal: SupportGoal,
  activeOmens: string[],
  maxMillis: number,
  signal: AbortSignal,
): Promise<SupportReport> {
  const response = await fetch('/api/v1/crafting/support/recommend', {
    method: 'POST',
    signal,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      state,
      goal,
      activeOmens,
      limits: { maxStates: 200000, maxEdges: 5000000, maxMillis },
    }),
  })
  if (!response.ok)
    throw new Error(
      response.status === 422
        ? 'The Support state, goal or calculation budget was rejected. Your inputs are preserved.'
        : 'The recommendation service failed. Your inputs are preserved; please retry.',
    )
  const value = (await response.json()) as SupportReport
  const invalid = () => {
    throw new Error(
      'Could not verify recommendation probabilities. Your inputs are preserved.',
    )
  }
  if (
    !value ||
    !['ruleVersion', 'ledgerVersion', 'transitionNamespace'].every(
      (k) => typeof value[k as 'ruleVersion'] === 'string',
    ) ||
    !Array.isArray(value.comparisons) ||
    value.comparisons.length > 5 ||
    typeof value.complete !== 'boolean' ||
    typeof value.rankingCertified !== 'boolean' ||
    value.rankingCertified !== value.complete ||
    ![
      'comparedSequences',
      'totalSequences',
      'expandedStates',
      'expandedEdges',
    ].every(
      (k) =>
        Number.isInteger(value[k as 'expandedStates']) &&
        value[k as 'expandedStates'] >= 0,
    ) ||
    value.comparedSequences > value.totalSequences ||
    !Number.isFinite(value.elapsedMillis) ||
    value.elapsedMillis < 0 ||
    !value.cache ||
    !Object.values(value.cache).every((n) => Number.isInteger(n) && n >= 0) ||
    !value.assessment ||
    !['READY', 'ACHIEVED', 'IMPOSSIBLE'].includes(value.assessment.status)
  )
    return invalid()
  for (const comparison of value.comparisons) {
    if (
      !Array.isArray(comparison.sequence) ||
      comparison.sequence.length > 6 ||
      !comparison.sequence.every(addition) ||
      ![
        comparison.successLower,
        comparison.successUpper,
        comparison.failureProbability,
        comparison.unresolvedProbability,
      ].every(probability) ||
      comparison.successLower > comparison.successUpper + 1e-8 ||
      Math.abs(
        comparison.successLower +
          comparison.failureProbability +
          comparison.unresolvedProbability -
          1,
      ) > 1e-8 ||
      Math.abs(
        comparison.successUpper -
          Math.min(
            1,
            comparison.successLower + comparison.unresolvedProbability,
          ),
      ) > 1e-8 ||
      typeof comparison.complete !== 'boolean' ||
      (comparison.complete && comparison.unresolvedProbability > 1e-8) ||
      !Array.isArray(comparison.steps) ||
      !comparison.steps.every(
        (s) =>
          Number.isInteger(s.step) &&
          s.step >= 0 &&
          s.step <= comparison.sequence.length &&
          (s.action === null ? s.step === 0 : addition(s.action)) &&
          [
            s.firstHitProbability,
            s.blockedProbability,
            s.continuingOrUnresolvedProbability,
          ].every(probability) &&
          Array.isArray(s.blockedReasons) &&
          s.blockedReasons.every((r) => typeof r === 'string'),
      ) ||
      Math.abs(
        comparison.steps.reduce((sum, s) => sum + s.firstHitProbability, 0) -
          comparison.successLower,
      ) > 1e-8
    )
      return invalid()
  }
  if (
    value.complete &&
    (value.comparedSequences !== value.totalSequences ||
      value.comparisons.some((c) => !c.complete))
  )
    return invalid()
  return value
}
export async function loadSupportFamilies(
  signal: AbortSignal,
): Promise<SupportFamily[]> {
  const response = await fetch('/api/v1/crafting/support/families', { signal })
  if (!response.ok)
    throw new Error('Could not load goal families. Please retry.')
  const value = (await response.json()) as SupportFamily[]
  if (
    !Array.isArray(value) ||
    !value.length ||
    new Set(value.map((f) => f.id)).size !== value.length ||
    !value.every(
      (f) =>
        typeof f.id === 'string' &&
        ['PREFIX', 'SUFFIX'].includes(f.affix) &&
        Array.isArray(f.tiers) &&
        f.tiers.length &&
        f.tiers.every(
          (t) =>
            Number.isInteger(t.tier) &&
            t.tier > 0 &&
            Number.isInteger(t.requiredItemLevel) &&
            t.requiredItemLevel >= 1 &&
            t.requiredItemLevel <= 100 &&
            typeof t.exampleText === 'string' &&
            typeof t.modifierId === 'string',
        ),
    )
  )
    throw new Error('Could not verify goal family data.')
  return value
}
export async function assessGoal(
  state: Bucket,
  goal: SupportGoal,
  signal: AbortSignal,
): Promise<GoalAssessment> {
  const response = await fetch('/api/v1/crafting/support/assess', {
    method: 'POST',
    signal,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ state, goal }),
  })
  if (!response.ok)
    throw new Error(
      'Could not validate the Support state and goal. Your inputs are preserved.',
    )
  const value = (await response.json()) as GoalAssessment
  if (
    !['READY', 'ACHIEVED', 'IMPOSSIBLE', 'INVALID_GOAL'].includes(
      value?.status,
    ) ||
    !['valid', 'achieved', 'feasible'].every(
      (k) => typeof value[k as 'valid'] === 'boolean',
    ) ||
    !Number.isInteger(value.requiredMatched) ||
    !Number.isInteger(value.candidatesMatched) ||
    value.requiredMatched < 0 ||
    value.requiredMatched > goal.required.length ||
    value.candidatesMatched < 0 ||
    value.candidatesMatched > goal.candidates.length ||
    typeof value.matches !== 'object' ||
    value.matches === null ||
    Array.isArray(value.matches) ||
    !Object.values(value.matches).every((v) => typeof v === 'boolean') ||
    !Array.isArray(value.issues) ||
    !value.issues.every((v) => typeof v === 'string') ||
    (value.status === 'ACHIEVED') !== value.achieved ||
    (value.status === 'INVALID_GOAL') === value.valid ||
    (value.status === 'READY' || value.status === 'ACHIEVED') !== value.feasible
  )
    throw new Error(
      'Could not verify the goal assessment. Your inputs are preserved.',
    )
  return value
}
