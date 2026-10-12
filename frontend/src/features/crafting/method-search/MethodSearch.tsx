import { useEffect, useRef, useState } from 'react'
import {
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { useI18n } from '../../../shared/i18n/i18n'
import { ItemCard } from '../ItemCard'
import { localizedAction } from '../localizedCrafting'
import type { WorkbenchAction } from '../workbenchApi'
import type { PathTreeProps } from '../path-tree/PathTree'
import { pathTreeMessages } from '../path-tree/messages'
import { methodMessages } from '../path-tree/methodMessages'
import { canonical, percent } from '../path-tree/validation'
import {
  acceptSnapshot,
  call,
  createBody,
  mutateJob,
  validateGraph,
} from './api'
import { methodSearchMessages } from './messages'
import { presentRollCell } from './rollPresentation'
import type { Distribution, Graph, Snapshot } from './types'
import './method-search.css'

export function MethodSearch(props: PathTreeProps) {
  return (
    <Run key={canonical([props.inputGeneration, props.request])} {...props} />
  )
}
function Run({ request, presentItem, onRecalculate }: PathTreeProps) {
  const { locale } = useI18n(),
    common = pathTreeMessages[locale],
    copy = methodSearchMessages[locale],
    exits = methodMessages[locale]
  const client = useQueryClient(),
    [cycles, setCycles] = useState(100)
  const key = ['method-search-v2', request.clientRequestId] as const
  const latest = useRef<Snapshot | undefined>(undefined)
  const control = useRef(new AbortController())
  const ruleset = request.start.provenance.rulesetIdentity
  useEffect(() => {
    const controller = new AbortController()
    control.current = controller
    return () => controller.abort()
  }, [])
  const query = useQuery({
    queryKey: key,
    queryFn: async ({ signal }) => {
      const old = latest.current
      const next = await call<Snapshot>(
        old ? `/${old.jobId}` : '',
        ruleset,
        signal,
        old ? undefined : createBody(request),
      )
      if (next.clientRequestId !== request.clientRequestId)
        throw new Error('WRONG_REQUEST')
      latest.current = acceptSnapshot(latest.current, next)
      return latest.current
    },
    refetchInterval: (q) =>
      q.state.data && ['QUEUED', 'RUNNING'].includes(q.state.data.status)
        ? 1000
        : false,
    retry: 1,
  })
  const job = query.data
  const pages = useInfiniteQuery({
    queryKey: [...key, 'graph', job?.revision],
    initialPageParam: null as string | null,
    enabled: !!job,
    queryFn: async ({ pageParam, signal }) => {
      if (!job) throw new Error('NO_JOB')
      if (pageParam === null) return job.graph
      const graph = validateGraph(
        await call<Graph>(
          `/${job.jobId}/graph?revision=${job.revision}&cursor=${encodeURIComponent(pageParam)}`,
          ruleset,
          signal,
        ),
      )
      if (graph.jobId !== job.jobId || graph.revision !== job.revision)
        throw new Error('STALE_GRAPH')
      return graph
    },
    getNextPageParam: (last) => last.nextCursor ?? undefined,
    retry: false,
  })
  const mutation = useMutation({
    mutationFn: async (operation: 'CANCEL' | 'RESUME') => {
      const current = latest.current
      if (!current) throw new Error('NO_JOB')
      const next = await mutateJob(
        current,
        operation,
        ruleset,
        control.current.signal,
      )
      latest.current = acceptSnapshot(latest.current, next)
      client.setQueryData(key, latest.current)
    },
    onError: () => {
      void client.invalidateQueries({ queryKey: key })
    },
  })
  const graph = job?.graph
  const methods = [
    ...new Map(
      (pages.data?.pages ?? (graph ? [graph] : []))
        .flatMap((p) => p.methods)
        .map((m) => [m.id, m]),
    ).values(),
  ]
  const names = new Map(
    graph?.buckets.map((b, i) => [b.id, `${copy.state} ${i + 1}`]),
  )
  const action = (a: string) => localizedAction(a as WorkbenchAction)
  const final = job?.strategies.find(
    (s) => s.cycles === cycles && s.stateId === job.rootStateId,
  )
  const recommended = new Set(
    job?.strategies
      .filter((s) => s.cycles === cycles)
      .flatMap((s) => s.methodIds) ?? [],
  )
  return (
    <section className="method-map" aria-label={common.title}>
      <h2>{common.title}</h2>
      <p role="status">
        {query.isError || mutation.isError
          ? common.error
          : !job || ['QUEUED', 'RUNNING'].includes(job.status)
            ? common.calculating
            : job.status === 'COMPLETED'
              ? copy.complete
              : job.status === 'FAILED'
                ? common.error
                : common.partial}
      </p>
      <div className="method-map__controls">
        {job && ['QUEUED', 'RUNNING'].includes(job.status) && (
          <button
            disabled={mutation.isPending}
            onClick={() => mutation.mutate('CANCEL')}
          >
            {common.stop}
          </button>
        )}
        {job?.resumable && (
          <button
            disabled={mutation.isPending}
            onClick={() => mutation.mutate('RESUME')}
          >
            {common.resume}
          </button>
        )}
        <button onClick={onRecalculate}>{common.recalculate}</button>
        <fieldset>
          <legend>{copy.cycles}</legend>
          {[100, 300, 500].map((n) => (
            <label key={n}>
              <input
                type="radio"
                name={`cycles-${request.clientRequestId}`}
                checked={cycles === n}
                onChange={() => setCycles(n)}
              />
              {n}
            </label>
          ))}
        </fieldset>
      </div>
      {final && (
        <p>
          {copy.eventual}: {percent(final.lower)}
          {final.status !== 'EXACT' && ` – ${percent(final.upper)}`}
        </p>
      )}
      {graph && (
        <Overview
          graph={{ ...graph, methods }}
          names={names}
          cycles={cycles}
          label={common.title}
        />
      )}
      <div className="method-map__states">
        {graph?.buckets.map((b) => (
          <article
            key={b.id}
            id={`bucket-${b.id}`}
            className={b.goal === 'MATCH' ? 'method-map__goal' : ''}
          >
            <h3>{names.get(b.id)}</h3>
            <p>
              {copy.state}: {b.states}
            </p>
            <p>
              {b.goal === 'MATCH'
                ? common.hit
                : b.goal === 'NO_MATCH'
                  ? common.noMatch
                  : common.unknown}
            </p>
            <ItemCard
              item={presentRollCell(
                b.representative,
                b.representativeRolls,
                locale,
                presentItem,
              )}
            />
            <p>
              {b.entries
                .map((s) =>
                  s === 'MATCH' ? '✓' : s === 'NO_MATCH' ? '○' : '?',
                )
                .join(' · ')}
            </p>
          </article>
        ))}
      </div>
      <div className="method-map__methods">
        {methods.map((method) => {
          const point = method.points.find((p) => p.cycles === cycles)
          const local = job?.localRecommendations.some(
            (r) => r.cycles === cycles && r.methodIds.includes(method.id),
          )
          return (
            <article
              key={method.id}
              className={
                recommended.has(method.id) || local
                  ? 'method-map__recommended'
                  : ''
              }
            >
              <h3>
                {names.get(method.sourceBucketId)} →{' '}
                {method.cycleActions.map(action).join(' → ')} ↻
              </h3>
              {recommended.has(method.id) && <strong>{copy.final}</strong>}
              {job?.strategies.some(
                (strategy) =>
                  strategy.cycles === cycles &&
                  strategy.methodIds.length > 1 &&
                  strategy.methodIds.includes(method.id),
              ) && <span>{copy.tied}</span>}
              {local && <strong>{copy.local}</strong>}
              {method.initialActions.length > 0 && (
                <p>
                  {copy.initial}:{' '}
                  {method.initialActions.map(action).join(' → ')}
                </p>
              )}
              <details>
                <summary>{common.inspect}</summary>
                <ItemCard
                  item={presentRollCell(
                    method.sourceItem,
                    method.sourceRolls,
                    locale,
                    presentItem,
                  )}
                />
              </details>
              {!point ? (
                <p>{common.unknown}</p>
              ) : (
                <>
                  <ul>
                    {point.exits.map((exit) => (
                      <li key={`${exit.kind}:${exit.bucketId}`}>
                        <a href={`#bucket-${exit.bucketId}`}>
                          → {names.get(exit.bucketId)}
                        </a>
                        : {percent(exit.probability)} ·{' '}
                        {exit.kind === 'GOAL'
                          ? common.hit
                          : exit.kind === 'LOSS'
                            ? copy.loss
                            : exit.kind === 'DEAD'
                              ? exits.dead
                              : exit.kind === 'ACTIVE'
                                ? exits.active
                                : copy.checkpoint}
                      </li>
                    ))}
                  </ul>
                  {BigInt(point.unresolved.numerator) > 0n && (
                    <p>
                      {common.unknown}: {percent(point.unresolved)}
                    </p>
                  )}
                  {point && job && (
                    <OutcomeItems
                      key={`${job.revision}:${cycles}`}
                      job={job}
                      methodId={method.id}
                      cycles={cycles}
                      ruleset={ruleset}
                      presentItem={presentItem}
                    />
                  )}
                  <details>
                    <summary>{copy.uses}</summary>
                    {Object.entries({
                      initial: point.initialUses,
                      cycle: point.cycleUses,
                    }).map(([phase, uses]) => (
                      <p key={phase}>
                        {phase === 'initial' ? copy.initial : common.repeat}:{' '}
                        {Object.entries(uses)
                          .map(
                            ([currency, p]) =>
                              `${action(currency)} ${decimal(p.numerator, p.denominator)}`,
                          )
                          .join(' · ') || '0'}
                      </p>
                    ))}
                  </details>
                </>
              )}
            </article>
          )
        })}
      </div>
      {pages.hasNextPage && (
        <button
          disabled={pages.isFetchingNextPage}
          onClick={() => void pages.fetchNextPage()}
        >
          {common.more}
        </button>
      )}
      {pages.isError && <p role="alert">{common.error}</p>}
    </section>
  )
}
function decimal(n: string, d: string) {
  const scaled = (BigInt(n) * 100n) / BigInt(d)
  return `${scaled / 100n}.${(scaled % 100n).toString().padStart(2, '0')}`
}

function OutcomeItems({
  job,
  methodId,
  cycles,
  ruleset,
  presentItem,
}: {
  job: Snapshot
  methodId: string
  cycles: number
  ruleset: string
  presentItem: PathTreeProps['presentItem']
}) {
  const [open, setOpen] = useState(false),
    { locale } = useI18n(),
    copy = pathTreeMessages[locale]
  const results = useInfiniteQuery({
    queryKey: ['method-outcomes', job.jobId, job.revision, methodId, cycles],
    enabled: open,
    initialPageParam: null as string | null,
    queryFn: async ({ signal, pageParam }) => {
      const params = new URLSearchParams({
        revision: String(job.revision),
        methodId,
        cycles: String(cycles),
        ...(pageParam ? { cursor: pageParam } : {}),
      })
      const value = await call<Distribution>(
        `/${job.jobId}/distribution?${params}`,
        ruleset,
        signal,
      )
      if (
        value.version !== 2 ||
        value.jobId !== job.jobId ||
        value.revision !== job.revision ||
        value.methodId !== methodId ||
        value.cycles !== cycles
      )
        throw new Error('STALE_DISTRIBUTION')
      return value
    },
    getNextPageParam: (last) => last.nextCursor ?? undefined,
    retry: false,
  })
  return (
    <details onToggle={(e) => setOpen(e.currentTarget.open)}>
      <summary>
        {copy.inspect} · {copy.edge}
      </summary>
      {results.isLoading && <p>{copy.calculating}</p>}
      {results.isError && <p role="alert">{copy.error}</p>}
      {results.data?.pages
        .flatMap((p) => p.states)
        .map((s) => (
          <div key={`${s.stateId}:${s.kind}`}>
            <p>{percent(s.probability)}</p>
            <ItemCard item={presentItem(s.item, locale)} />
          </div>
        ))}
      {results.hasNextPage && (
        <button
          disabled={results.isFetchingNextPage}
          onClick={() => void results.fetchNextPage()}
        >
          {copy.more}
        </button>
      )}
    </details>
  )
}

function Overview({
  graph,
  names,
  cycles,
  label,
}: {
  graph: Graph
  names: Map<string, string>
  cycles: number
  label: string
}) {
  const columns = new Map<number, number>()
  const positions = new Map(
    graph.buckets.map((bucket) => {
      const row = columns.get(bucket.depth) ?? 0
      columns.set(bucket.depth, row + 1)
      return [
        bucket.id,
        { x: 70 + bucket.depth * 240, y: 55 + row * 120 },
      ] as const
    }),
  )
  const width = Math.max(420, ...[...positions.values()].map((p) => p.x + 180)),
    height = Math.max(150, ...[...positions.values()].map((p) => p.y + 70))
  const offsets = new Map<string, number>()
  return (
    <div className="method-map__overview">
      <svg width={width} height={height} role="img" aria-label={label}>
        {graph.methods.flatMap((method) =>
          (method.points.find((p) => p.cycles === cycles)?.exits ?? []).map(
            (exit) => {
              const from = positions.get(method.sourceBucketId)!,
                to = positions.get(exit.bucketId)!
              const pair = `${method.sourceBucketId}:${exit.bucketId}`,
                index = offsets.get(pair) ?? 0
              offsets.set(pair, index + 1)
              const lift = 30 + index * 16
              return (
                <g key={`${method.id}:${exit.kind}:${exit.bucketId}`}>
                  <title>
                    {names.get(method.sourceBucketId)} →{' '}
                    {names.get(exit.bucketId)}: {percent(exit.probability)}
                  </title>
                  <path
                    d={`M ${from.x + 90} ${from.y} C ${from.x + 150} ${from.y - lift}, ${to.x - 60} ${to.y - lift}, ${to.x} ${to.y}`}
                    fill="none"
                    stroke="#a9905b"
                  />
                  <text x={to.x - 10} y={to.y + 4} fill="#dec082">
                    ›
                  </text>
                </g>
              )
            },
          ),
        )}
        {graph.buckets.map((bucket) => {
          const p = positions.get(bucket.id)!
          return (
            <a key={bucket.id} href={`#bucket-${bucket.id}`}>
              <rect
                x={p.x}
                y={p.y - 20}
                width={130}
                height={42}
                rx={8}
                fill="#24211e"
                stroke={bucket.goal === 'MATCH' ? '#dec082' : '#777'}
              />
              <text x={p.x + 8} y={p.y + 6} fill="#eee">
                {names.get(bucket.id)}
              </text>
            </a>
          )
        })}
      </svg>
    </div>
  )
}
