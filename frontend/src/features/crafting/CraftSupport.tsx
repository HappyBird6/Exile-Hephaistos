import { useI18n, formatPercent, formatNumber } from '../../shared/i18n/i18n'
import { useQuery } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { loadInitial } from './craftingApi'
import type { Bucket } from './craftingApi'
import { mapSolarText } from './workbenchApi'
import { assessGoal, loadSupportFamilies, recommendGoal } from './supportApi'
import { localizedAction } from './localizedCrafting'
import { workbenchOmens } from './workbenchApi'
import type {
  GoalAssessment,
  GoalCondition,
  SupportGoal,
  SupportReport,
} from './supportApi'
import './craft-support.css'

export function CraftSupport({ active }: { active: boolean }) {
  const { t, name, locale } = useI18n()
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
          {t(
            kind === 'required'
              ? 'ui.all_required_families'
              : 'ui.candidate_families',
          )}
        </legend>
        {goal[kind].map((condition, index) => {
          const family = families.data?.find((f) => f.id === condition.family)
          return (
            <div className="support-condition" key={condition.family}>
              <label>
                {condition.family}
                <select
                  aria-label={t('support.minimum_tier', {
                    kind: t(
                      kind === 'required'
                        ? 'support.required'
                        : 'support.candidate',
                    ),
                    family: condition.family,
                  })}
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
                        T{tier} {t('ui.tier_or_better')}
                        {tier > 1 ? `–T${tier}` : ''})
                      </option>
                    ))}
                </select>
                <small>
                  {family?.affix.toLowerCase()} {t('ui.family_source_example')}{' '}
                  {family?.effectExamples[0]}
                </small>
                {family && family.effectExamples.length > 1 && (
                  <div className="support-family-variants">
                    <strong>{t('ui.any_effect_in_this_family_counts')}</strong>
                    <p>{t('notice.family_examples')}</p>
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
                data-remove-condition
                aria-label={t('support.remove_family', {
                  kind: t(
                    kind === 'required'
                      ? 'support.required'
                      : 'support.candidate',
                  ),
                  family: condition.family,
                })}
                onClick={() =>
                  setGoal((g) => ({
                    ...g,
                    [kind]: g[kind].filter((_, i) => i !== index),
                  }))
                }
              >
                {t('ui.remove')}
              </button>
            </div>
          )
        })}
        <label>
          {t('support.add_family', {
            kind: t(
              kind === 'required' ? 'support.required' : 'support.candidate',
            ),
          })}
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
            <option value="">{t('ui.select_a_distinct_family')}</option>
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
    formatPercent(Math.max(0, Math.min(1, value)), 4, locale)
  const chosen =
    selectedRoute === null ? null : report?.comparisons[selectedRoute]
  return (
    <section
      className="craft-support"
      aria-label={t('ui.craft_support')}
      onChange={invalidateAssessment}
      onClickCapture={(event) => {
        if (
          (event.target as HTMLElement).closest('button[data-remove-condition]')
        )
          invalidateAssessment()
      }}
    >
      <p className="support-intro">{t('notice.support_root')}</p>
      <div className="support-columns">
        <section className="support-box">
          <h2>{t('ui.1_starting_item')}</h2>
          <label>
            {t('ui.input_source')}
            <select
              aria-label={t('ui.support_input_source')}
              value={source}
              onChange={(e) => setSource(e.target.value as typeof source)}
            >
              <option value="base">{t('ui.server_solar_base')}</option>
              <option value="text">{t('ui.verified_item_text')}</option>
              <option value="manual">{t('ui.manual_modifier_tiers')}</option>
            </select>
          </label>
          {source !== 'text' && (
            <label>
              {t('ui.support_item_level')}
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
            <p role="alert">
              {t('ui.item_level_must_be_an_integer_from_1_to_100')}
            </p>
          )}
          {source === 'manual' && (
            <>
              <label>
                {t('ui.manual_rarity')}
                <select
                  value={rarity}
                  onChange={(e) => {
                    setRarity(e.target.value as Bucket['rarity'])
                    setManual([])
                  }}
                >
                  <option value="NORMAL">{t('ui.normal')}</option>
                  <option value="MAGIC">{t('ui.magic')}</option>
                  <option value="RARE">{t('ui.rare')}</option>
                </select>
              </label>
              <p>{t('notice.manual_rolls')}</p>
              {manual.map((id, index) => (
                <p key={id}>
                  {initial.data?.modifiers[id]?.text} · T
                  {initial.data?.modifiers[id]?.tier}{' '}
                  <button
                    type="button"
                    data-remove-condition
                    aria-label={t('support.remove_manual', {
                      index: index + 1,
                    })}
                    onClick={() =>
                      setManual((ms) => ms.filter((_, i) => i !== index))
                    }
                  >
                    {t('ui.remove')}
                  </button>
                </p>
              ))}
              <label>
                {t('ui.add_manual_modifier')}
                <select
                  aria-label={t('ui.add_manual_modifier')}
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
                  <option value="">{t('ui.select_a_modifier_tier')}</option>
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
                {t('ui.support_item_text')}
                <textarea
                  value={text}
                  onChange={(e) => {
                    setText(e.target.value)
                    setMapped(null)
                    setMappingIssues([])
                  }}
                  placeholder={t(
                    'ui.paste_solar_amulet_text_copied_from_the_game',
                  )}
                />
              </label>
              <button
                type="button"
                onClick={validateText}
                disabled={pending || !text.trim() || !initial.data}
              >
                {t('ui.validate_support_text')}
              </button>
              {mapped && <p>{t('notice.mapping_verified')}</p>}
              {mappingIssues.map((issue, i) => (
                <p role="alert" key={i}>
                  {issue}
                </p>
              ))}
            </>
          )}
          {state && (
            <p className="support-state-summary">
              {' '}
              {t('ui.solar_level')} {state.itemLevel} ·{' '}
              {state.rarity.toLowerCase()} · {state.modifierIds.length}{' '}
              {t('ui.explicit_modifiers')}{' '}
            </p>
          )}
        </section>
        <section className="support-box">
          <h2>{t('ui.2_one_goal')}</h2>
          <p>
            {t('ui.all_required_conditions')}
            <strong>{t('ui.and')}</strong>
            {t('notice.candidate_logic')}
          </p>
          {conditions('required')}
          {conditions('candidates')}
          <label>
            {t('ui.candidate_n')}
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
          {!validN && <p role="alert">{t('notice.candidate_count')}</p>}
          <p className="support-goal-summary">
            {goal.required
              .map((c) => `${c.family} T${c.minimumTier} or better`)
              .join(' AND ') || 'No required families'}{' '}
            {t('ui.and')} {goal.candidateCount} {t('ui.of')}{' '}
            {goal.candidates.length} {t('ui.distinct_candidate_families')}{' '}
          </p>
          <button
            type="button"
            onClick={validateGoal}
            disabled={
              pending || !state || !validLevel || !validN || !families.data
            }
          >
            {pending
              ? t('ui.validating')
              : t('ui.check_starting_state_and_goal')}
          </button>
          <fieldset>
            <legend>{t('ui.addition_omens')}</legend>
            <p>{t('notice.addition_omen_scope')}</p>
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
                  {name(o.id, o.id.replaceAll('_', ' '))}: {o.effect}
                </label>
              ))}
          </fieldset>
          <label>
            {t('ui.calculation_time_budget')}
            <select
              value={maxMillis}
              onChange={(e) => setMaxMillis(Number(e.target.value))}
            >
              <option value={2000}>{t('ui.2_seconds')}</option>
              <option value={5000}>{t('ui.5_seconds')}</option>
              <option value={10000}>{t('ui.10_seconds')}</option>
            </select>
          </label>
          <button
            type="button"
            onClick={compareSequences}
            disabled={
              pending || !state || !validLevel || !validN || !families.data
            }
          >
            {pending ? t('ui.calculating') : t('ui.compare_currency_sequences')}
          </button>
          {pending && (
            <button type="button" onClick={invalidateAssessment}>
              {t('ui.cancel_this_request')}
            </button>
          )}
        </section>
      </div>
      {(initial.isError || families.isError) && (
        <p role="alert">
          {' '}
          {t('ui.support_catalog_load_error')}{' '}
          <button
            onClick={() => {
              void initial.refetch()
              void families.refetch()
            }}
          >
            {t('ui.retry_catalog')}
          </button>
        </p>
      )}
      {(initial.isPending || families.isPending) && active && (
        <p role="status">{t('ui.loading_support_catalog')}</p>
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
            {' '}
            {t('ui.required_colon')} {assessment.requiredMatched}/
            {goal.required.length}
            {t('ui.candidates_colon')} {assessment.candidatesMatched}/
            {goal.candidateCount} {t('ui.needed')}{' '}
          </p>
          {assessment.achieved && <p>{t('notice.goal_met')}</p>}
          {assessment.issues.map((issue, i) => (
            <p key={i}>{issue}</p>
          ))}
          {!report && !assessment.achieved && assessment.feasible && (
            <p>{t('notice.no_calculation')}</p>
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
            {' '}
            {t('ui.compared')} {report.comparedSequences}/
            {report.totalSequences} {t('ui.eligible_sequences')}{' '}
          </p>
          <p>
            {report.complete
              ? 'All listed sequences were fully calculated.'
              : `The calculation budget stopped before all work was complete. ${report.totalSequences - report.comparedSequences} unexamined sequences may have success probability anywhere from 0% to 100%. Unresolved mass is not failure.`}
          </p>
          {report.comparisons.length === 0 && <p>{t('notice.empty_budget')}</p>}
          {report.complete &&
            report.comparisons.length > 0 &&
            report.comparisons.every((c) => c.successLower <= 1e-12) && (
              <p>{t('notice.no_success')}</p>
            )}
          <div className="support-comparisons">
            {report.comparisons.map((c, index) => (
              <article key={c.sequence.join(',')}>
                <h3>
                  {report.rankingCertified
                    ? `Rank ${index + 1}`
                    : `Provisional candidate ${index + 1}`}
                </h3>
                <p>{c.sequence.map((a) => localizedAction(a)).join(' → ')}</p>
                <p className="support-probability">
                  {c.complete
                    ? percent(c.successLower)
                    : `${percent(c.successLower)} – ${percent(c.successUpper)}`}{' '}
                  {t('ui.success')}{' '}
                </p>
                <p>
                  {c.complete ? 'Complete first-hit sum' : 'Lower–upper bound'}
                  {t('ui.verified_failure')} {percent(c.failureProbability)}
                  {t('ui.unresolved_status')} {percent(c.unresolvedProbability)}
                  .
                </p>
                <button
                  type="button"
                  onClick={() => setSelectedRoute(index)}
                  aria-pressed={selectedRoute === index}
                >
                  {' '}
                  {t('ui.choose_sequence')} {index + 1}
                </button>
              </article>
            ))}
          </div>
          <details className="support-calculation-details">
            <summary>{t('ui.calculation_details_and_sources')}</summary>
            <p>
              {' '}
              {t('ui.published_solar_weights')} {report.ruleVersion}
              {t('ui.ledger')} {report.ledgerVersion}.
            </p>
            <p>
              {' '}
              {t('ui.expanded')} {formatNumber(report.expandedStates)}{' '}
              {t('ui.states_and')} {formatNumber(report.expandedEdges)}{' '}
              {t('ui.edges_in')}{' '}
              {formatNumber(report.elapsedMillis, {
                minimumFractionDigits: 1,
                maximumFractionDigits: 1,
              })}{' '}
              {t('ui.computed_pools')} {report.cache.computedPools}
              {t('ui.memory_hits')} {report.cache.memoryHits}
              {t('ui.database_hits')} {report.cache.persistedHits}.
            </p>
          </details>
          {chosen && (
            <section className="support-route">
              <h3>{t('ui.chosen_sequence_stop_as_soon_as_the_goal_is_met')}</h3>
              <ol>
                {chosen.sequence.map((action, index) => (
                  <li key={index}>
                    <strong>{localizedAction(action)}</strong>
                    <p>{t('notice.stop_goal')}</p>
                    {chosen.steps.find((s) => s.step === index + 1) && (
                      <p>
                        {' '}
                        {t('ui.first_hit_step')}{' '}
                        {percent(
                          chosen.steps.find((s) => s.step === index + 1)!
                            .firstHitProbability,
                        )}{' '}
                        {t('ui.continuing_probability')}{' '}
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
                    {' '}
                    {t('ui.step')} {s.step} {t('ui.rule_block')} {reason}
                  </p>
                )),
              )}
              <p>{t('notice.fixed_sequence')}</p>
            </section>
          )}
        </section>
      )}
      <section className="support-recovery">
        <h2>{t('ui.recovery_starts_a_new_calculation')}</h2>
        <p>{t('notice.recovery_scope')}</p>
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
          {t('ui.enter_recovered_state_as_a_new_root')}
        </button>
        {recovering && <p>{t('notice.recovery_reset')}</p>}
      </section>
    </section>
  )
}
