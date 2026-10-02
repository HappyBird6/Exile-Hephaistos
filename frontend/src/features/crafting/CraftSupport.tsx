import { useQuery } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { loadInitial } from './craftingApi'
import type { Bucket } from './craftingApi'
import { mapSolarText } from './workbenchApi'
import { assessGoal, loadSupportFamilies, recommendGoal } from './supportApi'
import { workbenchActionNames, workbenchOmens } from './workbenchApi'
import type {
  GoalAssessment,
  GoalCondition,
  SupportGoal,
  SupportReport,
} from './supportApi'
import './craft-support.css'

export function CraftSupport({ active }: { active: boolean }) {
  const [source, setSource] = useState<'base' | 'text' | 'manual'>('base')
  const [level, setLevel] = useState('82')
  const [rarity, setRarity] = useState<Bucket['rarity']>('RARE')
  const [manual, setManual] = useState<string[]>([])
  const [text, setText] = useState('')
  const [mapped, setMapped] = useState<Bucket | null>(null)
  const [mappingIssues, setMappingIssues] = useState<string[]>([])
  const [goal, setGoal] = useState<SupportGoal>({
    required: [],
    candidates: [],
    candidateCount: 0,
  })
  const [assessment, setAssessment] = useState<GoalAssessment | null>(null)
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)
  const [report, setReport] = useState<SupportReport | null>(null)
  const [selectedRoute, setSelectedRoute] = useState<number | null>(null)
  const [activeOmens, setActiveOmens] = useState<string[]>([])
  const [maxMillis, setMaxMillis] = useState(2000)
  const [recovering, setRecovering] = useState(false)
  const request = useRef<AbortController | null>(null)
  const validLevel =
    Number.isInteger(Number(level)) &&
    Number(level) >= 1 &&
    Number(level) <= 100
  const validN =
    Number.isInteger(goal.candidateCount) &&
    goal.candidateCount >= 0 &&
    goal.candidateCount <= goal.candidates.length
  const initial = useQuery({
    queryKey: ['support', 'initial', Number(level)],
    queryFn: ({ signal }) => loadInitial(Number(level), signal),
    enabled: active && validLevel,
    retry: false,
    staleTime: 60000,
  })
  const families = useQuery({
    queryKey: ['support', 'families'],
    queryFn: ({ signal }) => loadSupportFamilies(signal),
    enabled: active,
    retry: false,
    staleTime: Infinity,
  })
  const state: Bucket | null =
    source === 'text'
      ? mapped
      : initial.data
        ? {
            ...initial.data.state,
            rarity: source === 'base' ? 'NORMAL' : rarity,
            modifierIds: source === 'base' ? [] : manual,
          }
        : null
  useEffect(() => () => request.current?.abort(), [])
  function invalidateAssessment() {
    request.current?.abort()
    request.current = null
    setPending(false)
    setAssessment(null)
    setError('')
    setReport(null)
    setSelectedRoute(null)
  }
  const used = new Set(
    [...goal.required, ...goal.candidates].map((c) => c.family),
  )
  function conditions(kind: 'required' | 'candidates') {
    return (
      <fieldset>
        <legend>
          {kind === 'required' ? 'All required families' : 'Candidate families'}
        </legend>
        {goal[kind].map((condition, index) => {
          const family = families.data?.find((f) => f.id === condition.family)
          return (
            <div className="support-condition" key={condition.family}>
              <label>
                {condition.family}
                <select
                  aria-label={`${kind} ${condition.family} minimum tier`}
                  value={condition.minimumTier}
                  onChange={(e) =>
                    setGoal((g) => ({
                      ...g,
                      [kind]: g[kind].map((c, i) =>
                        i === index
                          ? { ...c, minimumTier: Number(e.target.value) }
                          : c,
                      ),
                    }))
                  }
                >
                  {[...new Set(family?.tiers.map((t) => t.tier))]
                    .sort((a, b) => a - b)
                    .map((tier) => (
                      <option value={tier} key={tier}>
                        T{tier} or better (T1{tier > 1 ? `–T${tier}` : ''})
                      </option>
                    ))}
                </select>
                <small>
                  {family?.affix.toLowerCase()} family · Source example:{' '}
                  {family?.effectExamples[0]}
                </small>
                {family && family.effectExamples.length > 1 && (
                  <div className="support-family-variants">
                    <strong>Any effect in this family counts.</strong>
                    <p>
                      This goal does not select a specific gem or skill type.
                      Source examples:
                    </p>
                    <ul>
                      {family.effectExamples.map((example) => (
                        <li key={example}>{example}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </label>
              <button
                type="button"
                aria-label={`Remove ${kind} ${condition.family}`}
                onClick={() =>
                  setGoal((g) => ({
                    ...g,
                    [kind]: g[kind].filter((_, i) => i !== index),
                  }))
                }
              >
                Remove
              </button>
            </div>
          )
        })}
        <label>
          Add {kind === 'required' ? 'required' : 'candidate'} family
          <select
            aria-label={`Add ${kind} family`}
            value=""
            onChange={(e) => {
              const family = families.data?.find((f) => f.id === e.target.value)
              if (!family || used.has(family.id)) return
              const condition: GoalCondition = {
                family: family.id,
                minimumTier: family.tiers.at(-1)!.tier,
              }
              setGoal((g) => ({ ...g, [kind]: [...g[kind], condition] }))
            }}
          >
            <option value="">Select a distinct family</option>
            {families.data
              ?.filter((f) => !used.has(f.id))
              .map((f) => (
                <option key={f.id} value={f.id}>
                  {f.id} · {f.affix.toLowerCase()}
                </option>
              ))}
          </select>
        </label>
      </fieldset>
    )
  }
  async function validateText() {
    if (request.current || !initial.data) return
    const controller = new AbortController()
    request.current = controller
    setPending(true)
    setError('')
    setAssessment(null)
    try {
      const result = await mapSolarText(
        text,
        controller.signal,
        initial.data.modifiers,
      )
      if (controller.signal.aborted) return
      if (
        result.state &&
        (result.state.snapshotId !== initial.data.state.snapshotId ||
          result.state.baseItemId !== initial.data.state.baseItemId)
      )
        throw new Error(
          'The mapped item does not match the Solar catalog. Your text is preserved.',
        )
      setMapped(
        result.state
          ? {
              ...result.state,
              modifierIds: result.state.explicits.map((m) => m.modifierId),
            }
          : null,
      )
      setMappingIssues(
        result.issues.map((i) => `Line ${i.lineNumber}: ${i.message}`),
      )
    } catch (e) {
      if (!controller.signal.aborted)
        setError(e instanceof Error ? e.message : 'Text validation failed.')
    } finally {
      if (request.current === controller) {
        request.current = null
        setPending(false)
      }
    }
  }
  async function validateGoal() {
    if (!state || request.current) return
    const controller = new AbortController()
    request.current = controller
    setPending(true)
    setError('')
    setAssessment(null)
    try {
      const result = await assessGoal(state, goal, controller.signal)
      if (!controller.signal.aborted) setAssessment(result)
    } catch (e) {
      if (!controller.signal.aborted)
        setError(e instanceof Error ? e.message : 'Goal validation failed.')
    } finally {
      if (request.current === controller) {
        request.current = null
        setPending(false)
      }
    }
  }
  async function compareSequences() {
    if (!state || request.current) return
    const controller = new AbortController()
    request.current = controller
    setPending(true)
    setError('')
    setReport(null)
    setSelectedRoute(null)
    try {
      const checked = await assessGoal(state, goal, controller.signal)
      if (controller.signal.aborted) return
      setAssessment(checked)
      if (!checked.valid || !checked.feasible || checked.achieved) return
      const result = await recommendGoal(
        state,
        goal,
        activeOmens,
        maxMillis,
        controller.signal,
      )
      if (!controller.signal.aborted) setReport(result)
    } catch (e) {
      if (!controller.signal.aborted)
        setError(
          e instanceof Error
            ? e.message
            : 'Recommendation service failed. Your inputs are preserved.',
        )
    } finally {
      if (request.current === controller) {
        request.current = null
        setPending(false)
      }
    }
  }
  const percent = (value: number) =>
    `${(Math.max(0, Math.min(1, value)) * 100).toLocaleString('en-US', { maximumFractionDigits: 4 })}%`
  const chosen =
    selectedRoute === null ? null : report?.comparisons[selectedRoute]
  return (
    <section
      className="craft-support"
      aria-label="Craft Support"
      onChange={invalidateAssessment}
      onClickCapture={(event) => {
        if (
          (event.target as HTMLElement).closest('button[aria-label^="Remove"]')
        )
          invalidateAssessment()
      }}
    >
      <p className="support-intro">
        Set one family-and-tier goal for a separate Solar Amulet starting state.
        Workbench changes do not replace this state.
      </p>
      <div className="support-columns">
        <section className="support-box">
          <h2>1. Starting item</h2>
          <label>
            Input source
            <select
              aria-label="Support input source"
              value={source}
              onChange={(e) => setSource(e.target.value as typeof source)}
            >
              <option value="base">Server Solar base</option>
              <option value="text">Verified item text</option>
              <option value="manual">Manual modifier tiers</option>
            </select>
          </label>
          {source !== 'text' && (
            <label>
              Support item level
              <input
                type="number"
                min="1"
                max="100"
                value={level}
                onChange={(e) => setLevel(e.target.value)}
              />
            </label>
          )}
          {!validLevel && (
            <p role="alert">Item level must be an integer from 1 to 100.</p>
          )}
          {source === 'manual' && (
            <>
              <label>
                Manual rarity
                <select
                  value={rarity}
                  onChange={(e) => {
                    setRarity(e.target.value as Bucket['rarity'])
                    setManual([])
                  }}
                >
                  <option value="NORMAL">Normal</option>
                  <option value="MAGIC">Magic</option>
                  <option value="RARE">Rare</option>
                </select>
              </label>
              <p>
                Select actual modifier tiers. Numerical rolls are unspecified
                and do not affect this goal.
              </p>
              {manual.map((id, index) => (
                <p key={id}>
                  {initial.data?.modifiers[id]?.text} · T
                  {initial.data?.modifiers[id]?.tier}{' '}
                  <button
                    type="button"
                    aria-label={`Remove manual modifier ${index + 1}`}
                    onClick={() =>
                      setManual((ms) => ms.filter((_, i) => i !== index))
                    }
                  >
                    Remove
                  </button>
                </p>
              ))}
              <label>
                Add manual modifier
                <select
                  aria-label="Add manual modifier"
                  value=""
                  disabled={
                    rarity === 'NORMAL' ||
                    manual.length >= (rarity === 'MAGIC' ? 2 : 6)
                  }
                  onChange={(e) => {
                    if (e.target.value)
                      setManual((ms) => [...ms, e.target.value])
                  }}
                >
                  <option value="">Select a modifier tier</option>
                  {families.data?.flatMap((f) =>
                    f.tiers
                      .filter(
                        (t) =>
                          t.requiredItemLevel <= Number(level) &&
                          manual.filter(
                            (id) =>
                              initial.data?.modifiers[id]?.affixType ===
                              f.affix,
                          ).length < (rarity === 'MAGIC' ? 1 : 3) &&
                          !manual.some((id) =>
                            f.tiers.some((ft) => ft.modifierId === id),
                          ),
                      )
                      .map((t) => (
                        <option value={t.modifierId} key={t.modifierId}>
                          {f.id} · T{t.tier} · {t.exampleText}
                        </option>
                      )),
                  )}
                </select>
              </label>
            </>
          )}
          {source === 'text' && (
            <>
              <label>
                Support item text
                <textarea
                  value={text}
                  onChange={(e) => {
                    setText(e.target.value)
                    setMapped(null)
                    setMappingIssues([])
                  }}
                  placeholder="Paste Solar Amulet text copied from the game"
                />
              </label>
              <button
                type="button"
                onClick={validateText}
                disabled={pending || !text.trim() || !initial.data}
              >
                Validate Support text
              </button>
              {mapped && (
                <p>
                  Catalog mapping verified. Original text is retained above.
                </p>
              )}
              {mappingIssues.map((issue, i) => (
                <p role="alert" key={i}>
                  {issue}
                </p>
              ))}
            </>
          )}
          {state && (
            <p className="support-state-summary">
              Solar Amulet · Level {state.itemLevel} ·{' '}
              {state.rarity.toLowerCase()} · {state.modifierIds.length} explicit
              modifiers
            </p>
          )}
        </section>
        <section className="support-box">
          <h2>2. One goal</h2>
          <p>
            All required conditions <strong>AND</strong> at least N distinct
            candidate families. Each family counts once. T2 or better means
            T1–T2.
          </p>
          {conditions('required')}
          {conditions('candidates')}
          <label>
            Candidate N
            <input
              type="number"
              min="0"
              max={goal.candidates.length}
              step="1"
              value={goal.candidateCount}
              onChange={(e) =>
                setGoal((g) => ({
                  ...g,
                  candidateCount: Number(e.target.value),
                }))
              }
            />
          </label>
          {!validN && (
            <p role="alert">
              Candidate N must be a whole number from 0 to the number of
              distinct candidate families.
            </p>
          )}
          <p className="support-goal-summary">
            {goal.required
              .map((c) => `${c.family} T${c.minimumTier} or better`)
              .join(' AND ') || 'No required families'}{' '}
            AND {goal.candidateCount} of {goal.candidates.length} distinct
            candidate families.
          </p>
          <button
            type="button"
            onClick={validateGoal}
            disabled={
              pending || !state || !validLevel || !validN || !families.data
            }
          >
            {pending ? 'Validating…' : 'Check starting state and goal'}
          </button>
          <fieldset>
            <legend>Addition omens</legend>
            <p>
              One ordinary Exalted omen may restrict the added family.
              Greater/Perfect interactions and simultaneous matching omens are
              unverified and blocked.
            </p>
            {workbenchOmens
              .filter(
                (o) =>
                  o.trigger === 'EXALTED' &&
                  o.id !== 'Omen_of_Greater_Exaltation',
              )
              .map((o) => (
                <label key={o.id}>
                  <input
                    type="checkbox"
                    checked={activeOmens.includes(o.id)}
                    onChange={(e) =>
                      setActiveOmens((ids) =>
                        e.target.checked
                          ? [...ids, o.id]
                          : ids.filter((id) => id !== o.id),
                      )
                    }
                  />
                  {o.id.replaceAll('_', ' ')}: {o.effect}
                </label>
              ))}
          </fieldset>
          <label>
            Calculation time budget
            <select
              value={maxMillis}
              onChange={(e) => setMaxMillis(Number(e.target.value))}
            >
              <option value={2000}>2 seconds</option>
              <option value={5000}>5 seconds</option>
              <option value={10000}>10 seconds</option>
            </select>
          </label>
          <button
            type="button"
            onClick={compareSequences}
            disabled={
              pending || !state || !validLevel || !validN || !families.data
            }
          >
            {pending ? 'Calculating…' : 'Compare currency sequences'}
          </button>
          {pending && (
            <button type="button" onClick={invalidateAssessment}>
              Cancel this request
            </button>
          )}
        </section>
      </div>
      {(initial.isError || families.isError) && (
        <p role="alert">
          Could not load Support catalog data.{' '}
          <button
            onClick={() => {
              void initial.refetch()
              void families.refetch()
            }}
          >
            Retry catalog
          </button>
        </p>
      )}
      {(initial.isPending || families.isPending) && active && (
        <p role="status">Loading Support catalog…</p>
      )}
      {error && <p role="alert">{error}</p>}
      {assessment && (
        <section className="support-assessment" aria-live="polite">
          <h2>
            {assessment.status === 'ACHIEVED'
              ? 'Goal already achieved'
              : assessment.status === 'READY'
                ? 'Goal ready for sequence comparison'
                : assessment.status === 'IMPOSSIBLE'
                  ? 'Goal cannot be reached by normal additions'
                  : 'Goal needs correction'}
          </h2>
          <p>
            Required: {assessment.requiredMatched}/{goal.required.length}.
            Candidates: {assessment.candidatesMatched}/{goal.candidateCount}{' '}
            needed.
          </p>
          {assessment.achieved && (
            <p>
              The starting state already satisfies the goal. First-hit
              probability at step 0 is 100%; no currency is needed.
            </p>
          )}
          {assessment.issues.map((issue, i) => (
            <p key={i}>{issue}</p>
          ))}
          {!report && !assessment.achieved && assessment.feasible && (
            <p>
              No sequence probabilities calculated yet. Choose Compare currency
              sequences.
            </p>
          )}
        </section>
      )}
      {report && (
        <section className="support-results" aria-live="polite">
          <h2>
            {report.rankingCertified
              ? 'Calculated sequence comparison'
              : 'Partial comparison — ranking is not final'}
          </h2>
          <p>
            Compared {report.comparedSequences}/{report.totalSequences} eligible
            currency-grade sequences.
          </p>
          <p>
            {report.complete
              ? 'All listed sequences were fully calculated.'
              : `The calculation budget stopped before all work was complete. ${report.totalSequences - report.comparedSequences} unexamined sequences may have success probability anywhere from 0% to 100%. Unresolved mass is not failure.`}
          </p>
          {report.comparisons.length === 0 && (
            <p>
              No sequences were evaluated within this budget. Increase the
              budget and retry; this is not a 0% result.
            </p>
          )}
          {report.complete &&
            report.comparisons.length > 0 &&
            report.comparisons.every((c) => c.successLower <= 1e-12) && (
              <p>
                No successful sequence under the selected state and omen rules.
                Review active omens and the verified blocking reasons below.
              </p>
            )}
          <div className="support-comparisons">
            {report.comparisons.map((c, index) => (
              <article key={c.sequence.join(',')}>
                <h3>
                  {report.rankingCertified
                    ? `Rank ${index + 1}`
                    : `Provisional candidate ${index + 1}`}
                </h3>
                <p>
                  {c.sequence.map((a) => workbenchActionNames[a]).join(' → ')}
                </p>
                <p className="support-probability">
                  {c.complete
                    ? percent(c.successLower)
                    : `${percent(c.successLower)} – ${percent(c.successUpper)}`}{' '}
                  success
                </p>
                <p>
                  {c.complete ? 'Complete first-hit sum' : 'Lower–upper bound'};
                  verified failure {percent(c.failureProbability)}; unresolved{' '}
                  {percent(c.unresolvedProbability)}.
                </p>
                <button
                  type="button"
                  onClick={() => setSelectedRoute(index)}
                  aria-pressed={selectedRoute === index}
                >
                  Choose sequence {index + 1}
                </button>
              </article>
            ))}
          </div>
          <details className="support-calculation-details">
            <summary>Calculation details and sources</summary>
            <p>
              Published Solar modifier weights; numeric rolls are marginalized.
              Rule {report.ruleVersion}; ledger {report.ledgerVersion}.
            </p>
            <p>
              Expanded {report.expandedStates.toLocaleString('en-US')} states
              and {report.expandedEdges.toLocaleString('en-US')} edges in{' '}
              {report.elapsedMillis.toFixed(1)} ms. Computed pools:{' '}
              {report.cache.computedPools}; reused from memory:{' '}
              {report.cache.memoryHits}; reused from PostgreSQL:{' '}
              {report.cache.persistedHits}.
            </p>
          </details>
          {chosen && (
            <section className="support-route">
              <h3>Chosen sequence — stop as soon as the goal is met</h3>
              <ol>
                {chosen.sequence.map((action, index) => (
                  <li key={index}>
                    <strong>{workbenchActionNames[action]}</strong>
                    <p>
                      Check the family-and-tier goal after this craft. If
                      achieved, stop immediately. Otherwise proceed only if the
                      next currency is eligible.
                    </p>
                    {chosen.steps.find((s) => s.step === index + 1) && (
                      <p>
                        First hit at this step:{' '}
                        {percent(
                          chosen.steps.find((s) => s.step === index + 1)!
                            .firstHitProbability,
                        )}{' '}
                        of starting probability. Continuing/unresolved:{' '}
                        {percent(
                          chosen.steps.find((s) => s.step === index + 1)!
                            .continuingOrUnresolvedProbability,
                        )}
                        .
                      </p>
                    )}
                  </li>
                ))}
              </ol>
              {chosen.steps.flatMap((s) =>
                s.blockedReasons.map((reason) => (
                  <p key={`${s.step}-${reason}`}>
                    Step {s.step} rule block: {reason}
                  </p>
                )),
              )}
              <p>
                This guide shows the chosen fixed sequence; each craft can
                produce different eligible modifiers. It does not alter your
                Workbench item.
              </p>
            </section>
          )}
        </section>
      )}
      <section className="support-recovery">
        <h2>Recovery starts a new calculation</h2>
        <p>
          Annulment, Chaos and their recovery omens are outside these addition
          sequences. After recovery, enter the actual recovered modifiers or
          validate recovered text as a new starting state. The previous route
          probability is not carried or multiplied into the new result.
          Divine/Blessed rerolls are outside this family-and-tier goal.
        </p>
        <button
          type="button"
          disabled={!state || pending}
          onClick={() => {
            if (!state) return
            invalidateAssessment()
            setRecovering(true)
            setManual(state.modifierIds)
            setRarity(state.rarity)
            setLevel(String(state.itemLevel))
            setSource('manual')
          }}
        >
          Enter recovered state as a new root
        </button>
        {recovering && (
          <p>
            Edit the recovered modifier tiers in Starting item, then validate or
            compare again. The family-and-tier goal is retained; previous
            comparison results were cleared.
          </p>
        )}
      </section>
    </section>
  )
}
