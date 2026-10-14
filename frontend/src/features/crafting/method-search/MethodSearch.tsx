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
import { acceptSnapshot } from './api'
import {
  fetchSnapshot,
  loadGraph,
  loadDistribution,
  mutateCertifiedJob,
} from './certifiedApi'
import { probabilityText, quantityText } from './probabilityPresentation'
import { methodSearchMessages } from './messages'
import { presentRollCell } from './rollPresentation'
import type { Graph, Snapshot } from './types'
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
    [cycles, setCycles] = useState(100),
    [continueSearch, setContinueSearch] = useState(true),
    [visiblePages, setVisiblePages] = useState(1)
  const key = ['certified-method-map-v1', request.clientRequestId] as const
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
      const next = await fetchSnapshot(request, old, ruleset, signal)
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
      const graph = await loadGraph(job, pageParam, ruleset, signal)
      if (graph.jobId !== job.jobId || graph.revision !== job.revision)
        throw new Error('STALE_GRAPH')
      return graph
    },
    getNextPageParam: (last) => last.nextCursor ?? undefined,
    retry: false,
  })
  const {
    hasNextPage,
    fetchNextPage,
    isFetchingNextPage,
    isError: pageError,
  } = pages
  const loadedPages = pages.data?.pages.length ?? 0
  useEffect(() => {
    if (
      loadedPages > 0 &&
      loadedPages < visiblePages &&
      hasNextPage &&
      !isFetchingNextPage &&
      !pageError
    )
      void fetchNextPage()
  }, [
    loadedPages,
    visiblePages,
    hasNextPage,
    isFetchingNextPage,
    pageError,
    fetchNextPage,
  ])
  const mutation = useMutation({
    mutationFn: async (operation: 'CANCEL' | 'RESUME') => {
      const current = latest.current
      if (!current) throw new Error('NO_JOB')
      const next = await mutateCertifiedJob(
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
  const { mutate, isPending, isError } = mutation
  useEffect(() => {
    if (!job || isPending || isError) return
    if (
      continueSearch &&
      job.status === 'PAUSED' &&
      job.reason === 'COMPUTE_BUDGET'
    )
      mutate('RESUME')
    else if (
      !continueSearch &&
      ['QUEUED', 'RUNNING', 'PAUSED'].includes(job.status)
    )
      mutate('CANCEL')
  }, [job, continueSearch, mutate, isPending, isError])
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
  const localRecommended = new Set(
    [
      ...(job?.localRecommendations ?? []),
      ...(pages.data?.pages.flatMap(
        (page) => page.localRecommendations ?? [],
      ) ?? []),
    ]
      .filter((r) => r.cycles === cycles)
      .flatMap((r) => r.methodIds),
  )
  return (
    <section className="method-map" aria-label={common.title}>
      <h2>{common.title}</h2>
      <p role="status">
        {query.isError || mutation.isError
          ? common.error
          : !job ||
              ['QUEUED', 'RUNNING'].includes(job.status) ||
              (continueSearch &&
                job.reason === 'COMPUTE_BUDGET' &&
                !mutation.isError)
            ? common.calculating
            : job.status === 'COMPLETED'
              ? copy.complete
              : job.status === 'FAILED'
                ? common.error
                : common.partial}
      </p>
      <div className="method-map__controls">
        {job &&
          (['QUEUED', 'RUNNING'].includes(job.status) ||
            (continueSearch && mutation.isPending)) && (
            <button
              onClick={() => {
                if (mutation.isError) mutation.reset()
                setContinueSearch(false)
              }}
            >
              {common.stop}
            </button>
          )}
        {job?.resumable && (
          <button
            disabled={mutation.isPending}
            onClick={() => {
              setContinueSearch(true)
              mutation.mutate('RESUME')
            }}
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
          recommended={new Set([...recommended, ...localRecommended])}
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
                b.representativeTiers,
                b.representativeOrbits,
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
          const local = localRecommended.has(method.id)
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
                    method.sourceTiers,
                    method.sourceOrbits,
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
                        : {probabilityText(exit.probability, exit.bounds)} ·{' '}
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
                  {BigInt(
                    (point.unresolvedBounds?.upper ?? point.unresolved)
                      .numerator,
                  ) > 0n && (
                    <p>
                      {common.unknown}:{' '}
                      {probabilityText(
                        point.unresolved,
                        point.unresolvedBounds,
                      )}
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
                              `${action(currency)} ${quantityText(p, (phase === 'initial' ? point.initialUseBounds : point.cycleUseBounds)?.[currency])}`,
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
          onClick={() => setVisiblePages((count) => count + 1)}
        >
          {common.more}
        </button>
      )}
      {pages.isError && <p role="alert">{common.error}</p>}
    </section>
  )
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
      const value = await loadDistribution(
        job,
        methodId,
        cycles,
        pageParam,
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
            <p>{probabilityText(s.probability, s.bounds)}</p>
            <ItemCard
              item={presentRollCell(
                s.item,
                s.rolls,
                locale,
                presentItem,
                s.tiers,
                s.orbits,
              )}
            />
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
  recommended,
}: {
  graph: Graph
  names: Map<string, string>
  cycles: number
  label: string
  recommended: Set<string>
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
                    {names.get(exit.bucketId)}:{' '}
                    {probabilityText(exit.probability, exit.bounds)}
                  </title>
                  <path
                    d={`M ${from.x + 90} ${from.y} C ${from.x + 150} ${from.y - lift}, ${to.x - 60} ${to.y - lift}, ${to.x} ${to.y}`}
                    fill="none"
                    stroke={recommended.has(method.id) ? '#e6c979' : '#a9905b'}
                    strokeWidth={recommended.has(method.id) ? 3 : 1}
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
