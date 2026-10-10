import { useEffect, useId, useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useI18n } from '../../../shared/i18n/i18n'
import { ItemCard } from '../ItemCard'
import { localizedAction } from '../localizedCrafting'
import { workbenchActionNames } from '../workbenchApi'
import type { WorkbenchAction } from '../workbenchApi'
import type { ItemPresentation } from './presentation'
import type { CreateRequest, GraphPage, JobSnapshot } from './types'
import type { PathSearchAdapter } from './api'
import { createHttpPathSearchAdapter } from './api'
import { PathSearchSession } from './session'
import { usePathSearch } from './usePathSearch'
import { pathTreeMessages, numbered } from './messages'
import { ancestors, stateLayers } from './graph'
import { canonical, percent } from './validation'
import './path-tree.css'

const http = createHttpPathSearchAdapter()
export type PathTreeProps = {
  /** Immutable start + goal + provenance, committed by the parent start button. */
  request: CreateRequest
  /** Change whenever the editor or current ruleset/catalog changes; unmount while editing. */
  inputGeneration: string | number
  presentItem: ItemPresentation
  adapter?: PathSearchAdapter
  onRecalculate: () => void
}
export function PathTree(props: PathTreeProps) {
  // A new committed input owns new sessions/cache keys. Unmount invalidates both server jobs.
  return (
    <Run key={canonical([props.inputGeneration, props.request])} {...props} />
  )
}
function Run({
  request,
  presentItem,
  adapter = http,
  onRecalculate,
}: PathTreeProps) {
  const client = useQueryClient()
  const [main] = useState(() => new PathSearchSession(client, adapter))
  const [recovery] = useState(() => new PathSearchSession(client, adapter))
  const frozen = useRef(structuredClone(request))
  const state = usePathSearch(main)
  const recoveryState = usePathSearch(recovery)
  const { locale } = useI18n(),
    copy = pathTreeMessages[locale]
  const heading = useRef<HTMLHeadingElement>(null)
  const [attempts, setAttempts] = useState('100')
  useEffect(() => {
    let active = true
    queueMicrotask(() => {
      if (active) {
        void main.start(frozen.current)
        heading.current?.focus()
      }
    })
    return () => {
      active = false
      main.invalidate()
      recovery.invalidate()
    }
  }, [main, recovery])
  useEffect(() => {
    recovery.invalidate()
  }, [recovery, state.job?.revision])
  return (
    <section className="path-tree" aria-label={copy.title}>
      <h2 tabIndex={-1} ref={heading}>
        {copy.title}
      </h2>
      <SearchStatus
        session={main}
        state={state}
        onRecalculate={onRecalculate}
      />
      {state.job && (
        <>
          <fieldset className="path-tree__observations">
            <legend>{numbered(copy.within, attempts)}</legend>
            {['100', '300', '500'].map((n) => (
              <label key={n}>
                <input
                  type="radio"
                  name={`observations-${main.id}`}
                  checked={attempts === n}
                  onChange={() => setAttempts(n)}
                />
                {n}
              </label>
            ))}
          </fieldset>
          <Results
            job={state.job}
            graph={state.graph}
            attempts={attempts}
            presentItem={presentItem}
          />
          {state.graph?.nextCursor && (
            <button onClick={() => void main.more()}>{copy.more}</button>
          )}
          {state.graph && state.job.capabilities.conditionalRecovery && (
            <RecoverySelection
              key={`${state.job.jobId}:${state.job.revision}`}
              job={state.job}
              graph={state.graph}
              presentItem={presentItem}
              session={recovery}
            />
          )}
        </>
      )}
      {(recoveryState.pending || recoveryState.job || recoveryState.error) && (
        <section className="path-tree__recovery" aria-label={copy.recovery}>
          <h3>{copy.recovery}</h3>
          <p>{copy.conditional}</p>
          <SearchStatus
            session={recovery}
            state={recoveryState}
            onRecalculate={() => recovery.invalidate()}
          />
          {recoveryState.job && (
            <>
              <h4>{numbered(copy.conditionalWithin, attempts)}</h4>
              <Results
                job={recoveryState.job}
                graph={recoveryState.graph}
                attempts={attempts}
                presentItem={presentItem}
              />
              {recoveryState.graph?.nextCursor && (
                <button onClick={() => void recovery.more()}>
                  {copy.more}
                </button>
              )}
            </>
          )}
        </section>
      )}
      <p className="path-tree__note">{copy.model}</p>
    </section>
  )
}
function SearchStatus({
  session,
  state,
  onRecalculate,
}: {
  session: PathSearchSession
  state: ReturnType<typeof usePathSearch>
  onRecalculate: () => void
}) {
  const { locale } = useI18n(),
    copy = pathTreeMessages[locale]
  const running =
    state.pending || ['RUNNING', 'QUEUED'].includes(state.job?.status ?? '')
  const controls = useRef<HTMLDivElement>(null)
  const ownedFocus = useRef(false)
  useEffect(() => {
    if (ownedFocus.current && document.activeElement === document.body)
      controls.current?.querySelector<HTMLButtonElement>('button')?.focus()
  }, [running, state.operation, state.error, state.job?.status])
  const expired =
    state.error === 'JOB_EXPIRED' || state.error === 'REVISION_EXPIRED'
  const changed = [
    'VERSION_CHANGED',
    'CATALOG_VERSION_MISMATCH',
    'RULESET_IDENTITY_MISMATCH',
    'SNAPSHOT_MISMATCH',
    'UNKNOWN_STAT',
    'INVALID_ITEM',
  ].includes(state.error ?? '')
  return (
    <div
      className="path-tree__status"
      ref={controls}
      onFocus={() => {
        ownedFocus.current = true
      }}
      onBlur={(event) => {
        if (
          event.relatedTarget &&
          !event.currentTarget.contains(event.relatedTarget)
        )
          ownedFocus.current = false
      }}
    >
      <p role={state.error ? 'alert' : 'status'}>
        {state.error
          ? expired
            ? copy.expired
            : changed
              ? copy.changed
              : copy.error
          : state.job?.status === 'UNSUPPORTED'
            ? copy.unsupported
            : state.job?.status === 'FAILED'
              ? copy.error
              : running
                ? copy.calculating
                : ['PAUSED', 'CANCELLED'].includes(state.job?.status ?? '')
                  ? copy.stopped
                  : ''}
      </p>
      {running && (
        <button
          disabled={state.operation}
          onClick={() =>
            state.job ? void session.command('CANCEL') : session.invalidate()
          }
        >
          {copy.stop}
        </button>
      )}
      {state.job?.resumable && !state.error && (
        <button
          disabled={state.operation}
          onClick={() => void session.command('RESUME')}
        >
          {copy.resume}
        </button>
      )}
      {(state.error ||
        state.job?.status === 'FAILED' ||
        state.job?.status === 'EXPIRED') && (
        <button onClick={onRecalculate}>{copy.recalculate}</button>
      )}
    </div>
  )
}
function Results({
  job,
  graph,
  attempts,
  presentItem,
}: {
  job: JobSnapshot
  graph: GraphPage | undefined
  attempts: string
  presentItem: ItemPresentation
}) {
  const { locale } = useI18n(),
    copy = pathTreeMessages[locale]
  const [selected, setSelected] = useState<string | null>(null)
  const ranking = job.rankings.find((r) => r.attempts === attempts)
  const ordered = (
    ranking?.entries.length
      ? ranking.entries.map((e) =>
          job.recommendations.find((r) => r.policy.id === e.policyId)!,
        )
      : job.recommendations
  ).slice(0, 5)
  const policy =
    ordered.find((r) => r.policy.id === selected)?.policy.id ??
    ordered[0]?.policy.id ??
    graph?.executions[0]?.policyId
  const rootHit = graph?.nodes[0]?.goalStatus === 'MATCH'
  if (rootHit)
    return (
      <>
        <p role="status">{copy.achieved}</p>
        <ItemCard
          item={presentItem(graph.nodes[0]!.item, locale)}
          baseItemId={graph.nodes[0]!.item.baseItemId}
        />
      </>
    )
  return (
    <>
      {ranking?.status === 'PROVISIONAL' && <p>{copy.partial}</p>}
      {!!ordered.length && (
        <div className="path-tree__candidates" aria-label={copy.candidates}>
          {ordered.map((r) => {
            const point = r.points.find((p) => p.attempts === attempts),
              rank = ranking?.entries.find(
                (e) => e.policyId === r.policy.id,
              )?.rank
            const recommended =
              ranking?.status === 'CERTIFIED_WITHIN_CANDIDATES' && rank === 1
            return (
              <button
                key={r.policy.id}
                aria-pressed={policy === r.policy.id}
                className={recommended ? 'path-tree__recommended' : ''}
                onClick={() => setSelected(r.policy.id)}
              >
                <strong>
                  {numbered(
                    copy.path,
                    job.recommendations.findIndex(
                      (candidate) => candidate.policy.id === r.policy.id,
                    ) + 1,
                  )}
                  {rank && ranking?.status === 'CERTIFIED_WITHIN_CANDIDATES'
                    ? ` · ${numbered(copy.rank, rank)}`
                    : ''}
                </strong>
                <span>
                  {r.policy.actions
                    .map((action) => actionLabel(action, copy.action))
                    .join(' → ')}
                </span>
                <span>
                  {point
                    ? point.status === 'COMPLETE'
                      ? percent(point.lower)
                      : `${percent(point.lower)} – ${percent(point.upper)} · ${copy.partial}`
                    : copy.unknown}
                </span>
                {recommended && <em>{copy.recommended}</em>}
              </button>
            )
          })}
        </div>
      )}
      {graph && policy && (
        <StateGraph graph={graph} policy={policy} presentItem={presentItem} />
      )}
      {graph?.nodes[0] && !policy && (
        <ItemCard
          item={presentItem(graph.nodes[0].item, locale)}
          baseItemId={graph.nodes[0].item.baseItemId}
        />
      )}
    </>
  )
}
function actionLabel(action: string, fallback: string) {
  if (!Object.hasOwn(workbenchActionNames, action)) return fallback
  const label = localizedAction(action as WorkbenchAction)
  return label === action ? fallback : label
}
function StateGraph({
  graph,
  policy,
  presentItem,
}: {
  graph: GraphPage
  policy: string
  presentItem: ItemPresentation
}) {
  const { locale } = useI18n(),
    copy = pathTreeMessages[locale],
    prefix = useId()
  const layers = stateLayers(graph, policy),
    nodes = new Map(graph.nodes.map((n) => [n.id, n])),
    executions = new Map(graph.executions.map((e) => [e.id, e]))
  const index = (id: string) => graph.nodes.findIndex((n) => n.id === id) + 1
  const anchor = (id: string) => `${prefix}-state-${index(id)}`
  return (
    <div className="path-tree__graph">
      <p className="path-tree__note">{copy.graphPartial}</p>
      {layers.map((layer, i) => (
        <div className="path-tree__layer" key={i}>
          {layer.map((id) => {
            const node = nodes.get(id)!
            const contexts = graph.executions.filter(
              (e) => e.policyId === policy && e.stateId === id,
            )
            const edges = graph.edges.filter((e) =>
              contexts.some((x) => x.id === e.from),
            )
            const shared =
              graph.executions.filter((e) => e.stateId === id).length > 1 ||
              graph.edges.filter(
                (e) =>
                  executions.get(e.to)?.stateId === id && e.kind === 'FORWARD',
              ).length > 1
            const unresolved = contexts.some((x) => {
              const expansion = graph.expansions.find(
                (e) => e.executionId === x.id,
              )
              return (
                node.goalStatus !== 'MATCH' &&
                (!expansion ||
                  expansion.status === 'PARTIAL' ||
                  expansion.status === 'UNSUPPORTED')
              )
            })
            return (
              <section
                key={id}
                id={anchor(id)}
                tabIndex={-1}
                className="path-tree__node"
                aria-label={numbered(copy.state, index(id))}
              >
                <h3>
                  {numbered(copy.state, index(id))}
                  {shared && <small> · {copy.join}</small>}
                </h3>
                <p>
                  {node.goalStatus === 'MATCH'
                    ? copy.hit
                    : node.goalStatus === 'NO_MATCH'
                      ? copy.noMatch
                      : copy.unknown}
                </p>
                <ItemCard
                  item={presentItem(node.item, locale)}
                  baseItemId={node.item.baseItemId}
                />
                {unresolved && <p>{copy.unknown}</p>}
                {!!edges.length && (
                  <ul className="path-tree__edges">
                    {edges.map((edge) => {
                      const destination = executions.get(edge.to)!.stateId
                      return (
                        <li key={edge.id}>
                          <span aria-hidden="true">
                            {edge.kind === 'REPEAT' ? '↻' : '↓'}{' '}
                          </span>
                          {actionLabel(edge.action, copy.action)} · {copy.edge}:{' '}
                          {percent(edge.probability)}
                          <br />
                          <a
                            href={`#${anchor(destination)}`}
                            onClick={(event) => {
                              event.preventDefault()
                              document
                                .getElementById(anchor(destination))
                                ?.focus()
                            }}
                          >
                            {edge.kind === 'REPEAT' ? copy.repeat + ' · ' : ''}
                            {numbered(copy.state, index(destination))}
                          </a>
                        </li>
                      )
                    })}
                  </ul>
                )}
              </section>
            )
          })}
        </div>
      ))}
    </div>
  )
}
function RecoverySelection({
  job,
  graph,
  presentItem,
  session,
}: {
  job: JobSnapshot
  graph: GraphPage
  presentItem: ItemPresentation
  session: PathSearchSession
}) {
  const { locale } = useI18n(),
    copy = pathTreeMessages[locale]
  const [failure, setFailure] = useState(''),
    [checkpoint, setCheckpoint] = useState('')
  const nodes = new Map(graph.nodes.map((n) => [n.id, n]))
  const failures = graph.executions.filter(
    (e) =>
      nodes.get(e.stateId)?.goalStatus === 'NO_MATCH' &&
      ancestors(graph, e.id).length,
  )
  const checkpoints = ancestors(graph, failure)
  const chosen = nodes.get(
    graph.executions.find((e) => e.id === failure)?.stateId ?? '',
  )
  const target = nodes.get(checkpoint)
  const label = (stateId: string) =>
    numbered(copy.state, graph.nodes.findIndex((n) => n.id === stateId) + 1)
  if (!failures.length) return null
  return (
    <details className="path-tree__recovery-picker">
      <summary>{copy.recovery}</summary>
      <p>{copy.conditional}</p>
      <label>
        {copy.failure}
        <select
          value={failure}
          onChange={(e) => {
            setFailure(e.target.value)
            setCheckpoint('')
            session.invalidate()
          }}
        >
          <option value="">{copy.choose}</option>
          {failures.map((e) => (
            <option key={e.id} value={e.id}>
              {label(e.stateId)} ·{' '}
              {numbered(
                copy.path,
                job.recommendations.findIndex(
                  (r) => r.policy.id === e.policyId,
                ) + 1,
              )}{' '}
              ·{' '}
              {actionLabel(
                job.recommendations.find((r) => r.policy.id === e.policyId)
                  ?.policy.actions[e.phase] ?? '',
                copy.action,
              )}
            </option>
          ))}
        </select>
      </label>
      {chosen && (
        <ItemCard
          item={presentItem(chosen.item, locale)}
          baseItemId={chosen.item.baseItemId}
        />
      )}
      <label>
        {copy.checkpoint}
        <select
          value={checkpoint}
          disabled={!failure}
          onChange={(e) => {
            setCheckpoint(e.target.value)
            session.invalidate()
          }}
        >
          <option value="">{copy.choose}</option>
          {checkpoints.map((id) => (
            <option key={id} value={id}>
              {label(id)}
            </option>
          ))}
        </select>
      </label>
      {target && (
        <ItemCard
          item={presentItem(target.item, locale)}
          baseItemId={target.item.baseItemId}
        />
      )}
      <button
        disabled={!chosen || !target}
        onClick={() =>
          void session.recover(job, {
            version: 1,
            clientRequestId: crypto.randomUUID(),
            parentRevision: job.revision,
            failureExecutionId: failure,
            checkpointStateId: checkpoint,
            observations: ['100', '300', '500'],
          })
        }
      >
        {copy.recover}
      </button>
    </details>
  )
}
