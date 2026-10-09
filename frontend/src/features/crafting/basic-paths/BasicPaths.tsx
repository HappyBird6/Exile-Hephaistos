import { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { createStore, type StoreApi } from 'zustand/vanilla'
import { useStore } from 'zustand'
import type { Initial } from '../craftingApi'
import type { ConcreteItem } from '../workbenchApi'
import { localizedAction } from '../localizedCrafting'
import { localizedModifierText } from '../localizedModifiers'
import { useI18n } from '../../../shared/i18n/i18n'
import {
  basicActions,
  evaluatePath,
  loadProvenance,
  PathRequestError,
  percent,
  sameProvenance,
} from './api'
import type {
  BasicAction,
  Fraction,
  PathRequest,
  PathResult,
  State,
} from './api'
import './basic-paths.css'

type Calculation = { key: string; request: PathRequest; recovery: boolean }
type Draft = {
  target: string
  checkpoint: State | null
  failure: State | null
  recoveryTarget: 'modifier' | 'checkpoint'
  recoveryModifier: string
  actions: BasicAction[]
  recoveryActions: BasicAction[]
  mode: PathRequest['policy']['mode']
  recoveryMode: PathRequest['policy']['mode']
  main: Calculation | null
  recovery: Calculation | null
}
type Props = {
  active: boolean
  item: ConcreteItem | null
  initial: Initial | undefined
  valid: boolean
  revision: string
  activeOmens: string[]
}

export function BasicPaths(props: Props) {
  const { t } = useI18n()
  const [open, setOpen] = useState(false)
  const summary = useRef<HTMLElement>(null)
  const [store] = useState(() =>
    createStore<Draft>(() => ({
      target: '',
      checkpoint: null,
      failure: null,
      recoveryTarget: 'modifier',
      recoveryModifier: '',
      actions: ['CHAOS'],
      recoveryActions: ['CHAOS'],
      mode: 'REPEAT_CYCLE',
      recoveryMode: 'SINGLE_PASS',
      main: null,
      recovery: null,
    })),
  )
  return (
    <details
      className="basic-paths"
      onToggle={(e) => setOpen(e.currentTarget.open)}
    >
      <summary ref={summary}>{t('paths.title')}</summary>
      {open && props.active && (
        <Calculator
          {...props}
          store={store}
          close={() => {
            const details = summary.current
              ?.parentElement as HTMLDetailsElement | null
            details?.removeAttribute('open')
            setOpen(false)
            summary.current?.focus()
          }}
        />
      )}
    </details>
  )
}

function Calculator({
  item,
  initial,
  valid,
  revision,
  activeOmens,
  store,
  close,
}: Props & {
  store: StoreApi<Draft>
  close: () => void
}) {
  const { t } = useI18n()
  const draft = useStore(store)
  const update = (value: Partial<Draft>) =>
    store.setState({ ...value, main: null, recovery: null })
  const provenance = useQuery({
    queryKey: ['basic-paths', 'provenance', initial?.rulesetIdentity],
    queryFn: ({ signal }) => loadProvenance(signal),
    retry: false,
    staleTime: 0,
  })
  const ready =
    valid &&
    item?.baseItemId === 'Metadata/Items/Amulets/FourAmulet9' &&
    !!initial &&
    !!provenance.data &&
    initial.rulesetIdentity === provenance.data.rulesetIdentity &&
    activeOmens.length === 0
  const current: State | null =
    ready && item && provenance.data
      ? { item, provenance: provenance.data }
      : null
  const currentSaved = (state: State | null) =>
    !!state &&
    !!provenance.data &&
    sameProvenance(state.provenance, provenance.data)
  const context = JSON.stringify([
    item,
    initial?.rulesetIdentity,
    revision,
    activeOmens,
    provenance.data,
    valid,
  ])
  const key = JSON.stringify([
    context,
    draft.target,
    draft.checkpoint,
    draft.failure,
    draft.recoveryTarget,
    draft.recoveryModifier,
    draft.actions,
    draft.recoveryActions,
    draft.mode,
    draft.recoveryMode,
  ])
  const main = draft.main?.key === key && ready ? draft.main : null
  const recovery = draft.recovery?.key === key && ready ? draft.recovery : null
  useEffect(() => {
    if (
      (draft.main && (draft.main.key !== key || !ready)) ||
      (draft.recovery && (draft.recovery.key !== key || !ready))
    )
      store.setState({ main: null, recovery: null })
  }, [draft.main, draft.recovery, key, ready, store])
  const mainResult = usePathEvaluation(main, false)
  const recoveryResult = usePathEvaluation(recovery, true)
  const busy = mainResult.isFetching || recoveryResult.isFetching
  useEffect(() => () => store.setState({ main: null, recovery: null }), [store])
  const definitions = Object.values(initial?.modifiers ?? {}).filter(
    (d) => d.layer === 'EXPLICIT',
  )
  function modifierSelect(
    label: string,
    value: string,
    onChange: (id: string) => void,
  ) {
    return (
      <label>
        {label}
        <select value={value} onChange={(e) => onChange(e.target.value)}>
          <option value="">{t('paths.select')}</option>
          {definitions.map((d) => (
            <option key={d.id} value={d.id}>
              {d.name} — T{d.tier} — {localizedModifierText(d)}
            </option>
          ))}
        </select>
      </label>
    )
  }
  function policy(recovery: boolean) {
    const actions = recovery ? draft.recoveryActions : draft.actions
    const mode = recovery ? draft.recoveryMode : draft.mode
    return (
      <fieldset>
        <legend>
          {t(recovery ? 'paths.recoveryPolicy' : 'paths.mainPolicy')}
        </legend>
        <label>
          {t('paths.mode')}
          <select
            value={mode}
            onChange={(e) =>
              update(
                recovery
                  ? { recoveryMode: e.target.value as typeof mode }
                  : { mode: e.target.value as typeof mode },
              )
            }
          >
            <option value="SINGLE_PASS">{t('paths.single')}</option>
            <option value="REPEAT_CYCLE">{t('paths.repeat')}</option>
          </select>
        </label>
        <ol>
          {actions.map((action, index) => (
            <li key={index}>
              {localizedAction(action)}{' '}
              <button
                type="button"
                aria-label={`${t('paths.remove')} ${index + 1}`}
                onClick={(event) => {
                  const fieldset = event.currentTarget.closest('fieldset')
                  update(
                    recovery
                      ? {
                          recoveryActions: actions.filter(
                            (_, i) => i !== index,
                          ),
                        }
                      : { actions: actions.filter((_, i) => i !== index) },
                  )
                  queueMicrotask(() =>
                    fieldset?.querySelectorAll('select').item(1)?.focus(),
                  )
                }}
              >
                {t('paths.remove')}
              </button>
            </li>
          ))}
        </ol>
        <label>
          {t('paths.add')}
          <select
            value=""
            disabled={actions.length >= 32}
            onChange={(e) => {
              if (!basicActions.includes(e.target.value as BasicAction)) return
              const next = [...actions, e.target.value as BasicAction]
              update(recovery ? { recoveryActions: next } : { actions: next })
            }}
          >
            <option value="">{t('paths.select')}</option>
            {basicActions.map((a) => (
              <option key={a} value={a}>
                {localizedAction(a)}
              </option>
            ))}
          </select>
        </label>
      </fieldset>
    )
  }
  function calculate(recovery: boolean) {
    if (!current || busy) return
    if (store.getState()[recovery ? 'recovery' : 'main']?.key === key) {
      const previous = recovery ? recoveryResult : mainResult
      if (previous.error) void previous.refetch({ cancelRefetch: false })
      return
    }
    const start = recovery ? draft.failure : current
    const target =
      recovery && draft.recoveryTarget === 'checkpoint'
        ? { checkpoint: draft.checkpoint, explicitModifierIds: [] }
        : {
            checkpoint: null,
            explicitModifierIds: [
              recovery ? draft.recoveryModifier : draft.target,
            ],
          }
    if (
      !start ||
      !currentSaved(start) ||
      (target.checkpoint && !currentSaved(target.checkpoint))
    )
      return
    store.setState({
      [recovery ? 'recovery' : 'main']: {
        key,
        recovery,
        request: {
          start,
          target,
          policy: {
            actions: recovery ? draft.recoveryActions : draft.actions,
            mode: recovery ? draft.recoveryMode : draft.mode,
          },
          activeOmens: [],
          observations: [100, 300, 500],
        },
      },
    })
  }
  return (
    <div>
      <p>{t('paths.scope')}</p>
      <p>{t('paths.observations')}</p>
      <p>{t('paths.model')}</p>
      {!ready && <p role="status">{t('paths.notReady')}</p>}
      {provenance.error && <p role="alert">{t('paths.provenanceError')}</p>}
      <button
        type="button"
        onClick={() => {
          void provenance.refetch()
        }}
      >
        {t('paths.refresh')}
      </button>
      <div className="basic-paths-columns">
        <section aria-label={t('paths.main')}>
          <h3>{t('paths.main')}</h3>
          {modifierSelect(t('paths.target'), draft.target, (target) =>
            update({ target }),
          )}
          <p>{t('paths.exactTarget')}</p>
          {policy(false)}
          <button
            type="button"
            disabled={
              !ready ||
              !definitions.some((d) => d.id === draft.target) ||
              !draft.actions.length ||
              busy ||
              (!!main && !mainResult.error)
            }
            onClick={() => calculate(false)}
          >
            {t('paths.calculate')}
          </button>
        </section>
        <section aria-label={t('paths.recovery')}>
          <h3>{t('paths.recovery')}</h3>
          <p>{t('paths.recoveryNotice')}</p>
          <button
            type="button"
            disabled={!current}
            onClick={() => update({ checkpoint: structuredClone(current!) })}
          >
            {t('paths.captureCheckpoint')}
          </button>
          <button
            type="button"
            disabled={!current}
            onClick={() => update({ failure: structuredClone(current!) })}
          >
            {t('paths.captureFailure')}
          </button>
          {draft.checkpoint && (
            <SavedState
              state={draft.checkpoint}
              label={t('paths.checkpoint')}
              current={currentSaved(draft.checkpoint)}
              initial={initial}
            />
          )}
          {draft.failure && (
            <SavedState
              state={draft.failure}
              label={t('paths.failure')}
              current={currentSaved(draft.failure)}
              initial={initial}
            />
          )}
          <label>
            {t('paths.recoveryTarget')}
            <select
              value={draft.recoveryTarget}
              onChange={(e) =>
                update({
                  recoveryTarget: e.target.value as Draft['recoveryTarget'],
                })
              }
            >
              <option value="modifier">{t('paths.target')}</option>
              <option value="checkpoint">{t('paths.checkpoint')}</option>
            </select>
          </label>
          {draft.recoveryTarget === 'modifier' &&
            modifierSelect(
              t('paths.target'),
              draft.recoveryModifier,
              (recoveryModifier) => update({ recoveryModifier }),
            )}
          {policy(true)}
          <button
            type="button"
            disabled={
              !ready ||
              !currentSaved(draft.failure) ||
              !draft.recoveryActions.length ||
              busy ||
              (!!recovery && !recoveryResult.error) ||
              (draft.recoveryTarget === 'checkpoint'
                ? !currentSaved(draft.checkpoint)
                : !definitions.some((d) => d.id === draft.recoveryModifier))
            }
            onClick={() => calculate(true)}
          >
            {t('paths.calculateRecovery')}
          </button>
          <button
            type="button"
            onClick={() => update({ checkpoint: null, failure: null })}
          >
            {t('paths.clear')}
          </button>
        </section>
      </div>
      {busy && <p role="status">{t('paths.pending')}</p>}
      {main && mainResult.error && (
        <p role="alert">
          {mainResult.error instanceof PathRequestError
            ? t('paths.requestError', { status: mainResult.error.status })
            : t('paths.responseError')}
        </p>
      )}
      {recovery && recoveryResult.error && (
        <p role="alert">
          {recoveryResult.error instanceof PathRequestError
            ? t('paths.requestError', { status: recoveryResult.error.status })
            : t('paths.responseError')}
        </p>
      )}
      {main && mainResult.data && <Results result={mainResult.data} />}
      {recovery && recoveryResult.data && (
        <Results result={recoveryResult.data} />
      )}
      <button type="button" onClick={close}>
        {t('paths.close')}
      </button>
    </div>
  )
}
function usePathEvaluation(calculation: Calculation | null, recovery: boolean) {
  const client = useQueryClient()
  const queryKey = useMemo(
    () => ['basic-paths', 'evaluation', calculation?.key, recovery] as const,
    [calculation?.key, recovery],
  )
  const result = useQuery({
    queryKey,
    enabled: !!calculation,
    retry: false,
    staleTime: Infinity,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
    refetchOnMount: false,
    gcTime: 0,
    queryFn: ({ signal }) =>
      evaluatePath(calculation!.request, recovery, signal),
  })
  useEffect(
    () => () => {
      void client.cancelQueries({ queryKey, exact: true })
    },
    [client, queryKey],
  )
  return result
}
function SavedState({
  state,
  label,
  current,
  initial,
}: {
  state: State
  label: string
  current: boolean
  initial: Initial | undefined
}) {
  const { t } = useI18n()
  return (
    <details>
      <summary>
        {label} — {state.item.rarity}, {t('paths.level')} {state.item.itemLevel}
        {!current && ` — ${t('paths.stale')}`}
      </summary>
      <ul>
        {state.item.explicits.map((m, i) => (
          <li key={i}>
            {(current && initial?.modifiers[m.modifierId]?.name) ||
              m.modifierId}{' '}
            — {JSON.stringify(m.values)}
          </li>
        ))}
      </ul>
      <pre>{JSON.stringify(state, null, 2)}</pre>
    </details>
  )
}
function Probability({ value }: { value: Fraction }) {
  const { t } = useI18n()
  return (
    <div>
      {percent(value)}{' '}
      <details>
        <summary>{t('paths.fraction')}</summary>
        <code>
          {value.numerator}/{value.denominator}
        </code>
      </details>
    </div>
  )
}
function Results({ result }: { result: PathResult }) {
  const { t } = useI18n()
  return (
    <section aria-label={t('paths.result')} aria-live="polite">
      <h3>
        {t(
          result.purpose === 'CONDITIONAL_RECOVERY'
            ? 'paths.recovery'
            : 'paths.main',
        )}
      </h3>
      <p>
        {result.request.policy.mode}:{' '}
        {result.request.policy.actions.map(localizedAction).join(' → ')}
      </p>
      {result.purpose === 'CONDITIONAL_RECOVERY' && (
        <p>{t('paths.recoveryNotice')}</p>
      )}
      <p>{t('paths.statusNotice')}</p>
      {result.points.map((point) => (
        <article key={point.attempts} className="basic-paths-point">
          <h4>
            {point.attempts} — {point.status}
          </h4>
          <dl>
            <dt>
              {t(point.status === 'COMPLETE' ? 'paths.exact' : 'paths.lower')}
            </dt>
            <dd>
              <Probability value={point.lower} />
            </dd>
            {point.status !== 'COMPLETE' && (
              <>
                <dt>{t('paths.upper')}</dt>
                <dd>
                  <Probability value={point.upper} />
                </dd>
              </>
            )}
            <dt>{t('paths.unresolved')}</dt>
            <dd>
              <Probability value={point.unresolved} />
            </dd>
            <dt>{t('paths.active')}</dt>
            <dd>
              <Probability value={point.active} />
            </dd>
            <dt>{t('paths.dead')}</dt>
            <dd>
              <Probability value={point.dead} />
            </dd>
          </dl>
          <p>{point.reachability}</p>
        </article>
      ))}
      <p>{t('paths.rounding')}</p>
      <p>{result.interpretation}</p>
      {result.reason && <p>{result.reason}</p>}
      <ul>
        {result.blockers.map((b, i) => (
          <li key={i}>
            {localizedAction(b.action)} — {b.status}: {b.reason}
          </li>
        ))}
      </ul>
      {result.blockersTruncated && <p>{t('paths.truncated')}</p>}
      <details>
        <summary>{t('paths.evidence')}</summary>
        <p>{t('paths.evidenceNotice')}</p>
        <a href={result.provenance.sourceUrl} target="_blank" rel="noreferrer">
          {result.provenance.sourceUrl}
        </a>
        <pre>
          {JSON.stringify(
            {
              request: result.request,
              provenance: result.provenance,
              renewalProof: result.renewalProof,
              computationLimits: result.computationLimits,
              evaluations: result.evaluations,
              peakFrontier: result.peakFrontier,
              peakFractionBits: result.peakFractionBits,
              fractionMetricScope: result.fractionMetricScope,
              blockers: result.blockers,
            },
            null,
            2,
          )}
        </pre>
      </details>
    </section>
  )
}
