import { useQueryClient } from '@tanstack/react-query'
import { craftingCost } from '../currency-prices/cost'
import type { PriceSnapshot } from '../currency-prices/api'
import { useCurrencyMarket } from '../currency-prices/context'
import { AppLink } from '../../../shared/navigation/AppLink'
import { HistoryWorkspaceLayout } from '../session-library/HistoryWorkspaceLayout'
import { SessionDetail } from '../session-detail/SessionDetail'
import { SessionOverviewPage } from '../session-history/SessionOverviewPage'
import type { Film, Frame } from '../workbenchHistory'
import type { ConcreteItem } from '../workbenchApi'
import { historyResources } from '../session-history/resources'
import { projectedFrames } from './projectedFrame'
import '../session-history/history.css'
import { SessionListCard } from '../session-library/SessionListCard'
import { sessionLibraryMessages } from '../session-library/sessionLibraryMessages'
import { historyMessages } from '../session-history/messages'
import { useContext, useMemo, useRef, useState } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import type { ItemCardData } from '../itemCardData'
import { OpenCraftingSession } from './openCraftingSession'
import type { Item } from '../path-tree/types'
import { rankedPlans, previewRoute, type Plan } from './rankedPlans'
import { CandidateSummary } from './CandidateSummary'
import type {
  AutomaticAction,
  AutomaticArrow,
  AutomaticNode,
  AutomaticView,
} from './automaticApi'
import { removesModifier } from './methodRisk'
import { chaosMeanLabel, methodPoint, isRepeatable } from './methodObservation'
import { policyCandidateMessages } from './policyCandidateMessages'
import { guideMessages } from './guideMessages'
import { probabilityText } from './probabilityPresentation'
import type { Selection } from './supportStorage'

const labels = {
  ko: [
    '제작 방법',
    '성공 확률순',
    '작업대에서 시작',
    '결과 선택',
    '제작 세션을 열 수 없습니다.',
    '선택한 경로에서 나올 수 있는 최종 아이템 예시입니다. 성공 확률은 이 방법으로 목표에 도달하는 모든 결과를 합산합니다.',
  ],
  en: [
    'Crafting methods',
    'By success chance',
    'Start in workbench',
    'Choose outcome',
    'Could not open the crafting session.',
    'One possible final item on the selected route. The success chance includes every result that meets the goal.',
  ],
  'zh-CN': [
    '制作方法',
    '按成功率',
    '在工作台开始',
    '选择结果',
    '无法打开制作会话。',
    '展示所选路线的一种最终物品。成功率包含所有达成目标的结果。',
  ],
  'zh-TW': [
    '製作方法',
    '依成功率',
    '在工作臺開始',
    '選擇結果',
    '無法開啟製作工作階段。',
    '顯示所選路線的一種最終物品。成功率包含所有達成目標的結果。',
  ],
  ja: [
    '製作方法',
    '成功率順',
    '作業台で開始',
    '結果を選択',
    '製作セッションを開けません。',
    '選択した経路の最終アイテムの一例です。成功率は目標を達成するすべての結果を含みます。',
  ],
  es: [
    'Métodos de fabricación',
    'Por probabilidad de éxito',
    'Empezar en el banco',
    'Elegir resultado',
    'No se pudo abrir la sesión.',
    'Un objeto final posible en la ruta elegida. La probabilidad incluye todos los resultados que cumplen el objetivo.',
  ],
  ru: [
    'Способы создания',
    'По шансу успеха',
    'Начать на верстаке',
    'Выберите результат',
    'Не удалось открыть сессию.',
    'Пример итогового предмета на выбранном пути. Шанс успеха учитывает все исходы, достигающие цели.',
  ],
  th: [
    'วิธีคราฟต์',
    'ตามโอกาสสำเร็จ',
    'เริ่มที่โต๊ะคราฟต์',
    'เลือกผลลัพธ์',
    'เปิดเซสชันไม่ได้',
    'ตัวอย่างไอเทมสุดท้ายในเส้นทางที่เลือก โอกาสสำเร็จรวมทุกผลลัพธ์ที่บรรลุเป้าหมาย',
  ],
} as const

type Props = {
  view: AutomaticView
  cycles: number
  action: (a: AutomaticAction) => string
  card: (n: AutomaticNode) => ItemCardData
  identity: string
  allowStart: boolean
  start: Item
  route: Selection[]
  onRoute: (route: Selection[]) => void
}
export function RankedCraftingSessions(props: Props) {
  const { locale, name } = useI18n(),
    copy = labels[locale],
    policy = policyCandidateMessages[locale]
  const market = useCurrencyMarket()
  const [terminal, setTerminal] = useState<{
    source: number
    key: string
  } | null>(null)
  const [selected, setSelected] = useState(() => {
    const params = new URLSearchParams(location.search)
    return params.get('job') === props.view.id ? (params.get('plan') ?? '') : ''
  })
  const preview = useRef<HTMLElement>(null)
  const baseName = name(
    props.start.baseItemId,
    props.view.nodes[0]
      ? props.card(props.view.nodes[0]).name
      : props.start.baseItemId,
  )
  const [edited, setEdited] = useState(false)
  const cache = useQueryClient()
  type SelectionSnapshot = {
    view: AutomaticView
    plan: Plan
    route: Selection[]
  }
  const snapshotKey = ['inspected-crafting-plan', props.view.id]
  const [pinned, updatePinned] = useState<SelectionSnapshot | null>(
    () => cache.getQueryData<SelectionSnapshot>(snapshotKey) ?? null,
  )
  const setPinned = (value: SelectionSnapshot) => {
    updatePinned(value)
    cache.setQueryData(snapshotKey, value)
  }
  const active = pinned?.view.id === props.view.id ? pinned : null
  const choose = (plan: Plan) => {
    setPinned({ view: props.view, plan, route: examples.get(plan.id) ?? [] })
    setTerminal(null)
    setEdited(false)
    setSelected(plan.id)
    props.onRoute(examples.get(plan.id) ?? [])
  }
  const routes = useMemo(() => {
    const ranked = rankedPlans(props.view, props.cycles)
    const examples = new Map(
      ranked.map((plan) => [plan.id, previewRoute(props.view, plan)]),
    )
    const complete = ranked.filter(
      (plan) => examples.get(plan.id)?.at(-1)?.kind === 'GOAL',
    )
    const nodes = new Map(props.view.nodes.map((n) => [n.id, n]))
    const methods = new Map(props.view.methods.map((m) => [m.id, m]))
    const films = new Map<string, Film>()
    const seen = new Set<string>()
    const plans = (complete.length ? complete : ranked).filter((plan) => {
      const film = projectedRoute(
        props.view.id,
        props.start,
        props.identity,
        examples.get(plan.id) ?? [],
        plan.cycles,
        methods,
        nodes,
      )
      const frames = film.frames
      films.set(plan.id, film)
      const key =
        frames.length > 1
          ? JSON.stringify(
              frames
                .slice(1)
                .map((frame) => [
                  frame.projectedCurrencies?.map((c) => [
                    c.currencyId,
                    [...c.omenIds].sort(),
                  ]),
                  frame.projectedWork,
                ]),
            )
          : plan.root.id
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })
    return { examples, plans, films }
  }, [props.view, props.cycles, props.identity, props.start])
  const { plans, examples } = routes
  const chosen =
    active?.plan ??
    plans.find((p) => p.id === selected) ??
    plans.find(
      (p) =>
        props.route.length > 0 &&
        props.route.every(
          (edge) => p.decisions.get(edge.source) === edge.method,
        ),
    ) ??
    plans[0]
  const route =
    active?.route ??
    (chosen &&
    !edited &&
    (!props.route.length ||
      (examples.get(chosen.id)?.at(-1)?.kind === 'GOAL' &&
        props.route.at(-1)?.kind !== 'GOAL') ||
      props.route.some(
        (edge) => chosen.decisions.get(edge.source) !== edge.method,
      ))
      ? (examples.get(chosen.id) ?? [])
      : props.route)
  const detailProps = {
    ...props,
    view: active?.view ?? props.view,
    onInspect: () => {
      if (chosen && !active)
        setPinned({ view: props.view, plan: chosen, route })
    },
    terminal,
    setTerminal,
    route,
    onRoute: (value: Selection[]) => {
      if (chosen)
        setPinned({
          view: active?.view ?? props.view,
          plan: chosen,
          route: value,
        })
      setEdited(true)
      props.onRoute(value)
    },
  }
  if (!chosen) return null
  return (
    <section className="session-plans" ref={preview}>
      <HistoryWorkspaceLayout
        heading={
          <>
            <h2>{copy[0]}</h2>
            <span>{copy[1]}</span>
          </>
        }
        list={
          <div className="wb-history-list wb-scrollbar">
            <div
              className="wb-session-library"
              role="list"
              aria-label={copy[0]}
            >
              {plans.map((plan, index) => {
                const example = examples.get(plan.id) ?? []
                const probability = plan.success
                  ? probabilityText(plan.success.lower, plan.success)
                  : policy.unresolved
                const film =
                  active && active.plan.id === plan.id
                    ? projectedRoute(
                        active.view.id,
                        props.start,
                        props.identity,
                        active.route,
                        active.plan.cycles,
                        new Map(active.view.methods.map((m) => [m.id, m])),
                        new Map(active.view.nodes.map((n) => [n.id, n])),
                      )
                    : routes.films.get(plan.id)!
                const title = planTitle(
                  film,
                  market?.data?.snapshot,
                  locale,
                  probability,
                )
                return (
                  <div role="listitem" key={plan.id}>
                    <SessionListCard
                      title={title}
                      baseId={props.start.baseItemId}
                      baseName={baseName}
                      selected={chosen.id === plan.id}
                      selectLabel={`${index + 1}. ${title}`}
                      onSelect={() => {
                        choose(plan)
                        if (window.innerWidth <= 760)
                          preview.current
                            ?.querySelector('.wb-history-preview')
                            ?.scrollIntoView?.({ block: 'nearest' })
                      }}
                      metadata={<small>{baseName}</small>}
                      actions={
                        <AppLink
                          onClick={(event) =>
                            cache.setQueryData(
                              [
                                'plan-film',
                                event.currentTarget.pathname +
                                  event.currentTarget.search,
                              ],
                              { ...film, title },
                            )
                          }
                          className="wb-session-card__overview"
                          href={overviewHref(
                            props,
                            plan,
                            chosen.id === plan.id ? route : example,
                            chosen.id === plan.id ? terminal : null,
                          )}
                        >
                          <svg viewBox="0 0 20 20" aria-hidden="true">
                            <rect x="2.5" y="3" width="15" height="14" rx="2" />
                            <path d="M6 7h8M6 10h8M6 13h5" />
                          </svg>
                          {historyMessages[locale].title}
                        </AppLink>
                      }
                    />
                  </div>
                )
              })}
            </div>
          </div>
        }
        preview={
          <SessionSteps key={chosen.id} {...detailProps} plan={chosen} />
        }
      />
    </section>
  )
}

function projectedRoute(
  id: string,
  start: Item,
  identity: string,
  route: Selection[],
  cycles: number,
  methods: Map<string, AutomaticArrow>,
  nodes: Map<number, AutomaticNode>,
): Film {
  const frames: Frame[] = [
    {
      state: start as ConcreteItem,
      action: null,
      rulesetIdentity: identity,
    },
  ]
  for (const edge of route) {
    const method = methods.get(edge.method)
    const node = nodes.get(edge.target)
    const outcome =
      method &&
      methodPoint(method, cycles)?.outcomes.find(
        (o) => o.kind === edge.kind && o.target === edge.target,
      )
    if (!method || !node || !outcome) break
    const steps = projectedFrames(
      frames.at(-1)!.state,
      method,
      outcome,
      node,
      cycles,
      identity,
    )
    if (!steps) break
    frames.push(...steps)
  }
  return {
    id: id,
    createdAt: '',
    frames,
    rulesetIdentity: identity,
  }
}

function planTitle(
  film: Film,
  snapshot: PriceSnapshot | null | undefined,
  locale: string,
  probability: string,
) {
  const cost = craftingCost(film, film.frames.length - 1, snapshot)
  const exalted = cost.incomplete
    ? '—'
    : cost.exalted.toLocaleString(locale, { maximumFractionDigits: 2 })
  return `${exalted} ex · ${probability}`
}

function overviewHref(
  props: Pick<Props, 'identity' | 'view'>,
  plan: Plan,
  route: Selection[],
  terminal: { source: number; key: string } | null,
) {
  const query = new URLSearchParams({
    plan: plan.id,
    rules: props.identity,
    cycles: String(plan.cycles),
    path: JSON.stringify(route.map((e) => [e.kind, e.target])),
  })
  if (terminal) query.set('terminal', JSON.stringify(terminal))
  return (
    '/crafting-plans/' +
    encodeURIComponent(props.view.id) +
    '/overview?' +
    query
  )
}

export function SessionSteps({
  view,
  plan,
  action,
  onRoute,
  identity,
  allowStart,
  route,
  terminal,
  setTerminal,
  overview = false,
  onInspect,
}: Omit<Props, 'start' | 'card'> & {
  plan: Plan
  overview?: boolean
  onInspect?: () => void
  terminal: { source: number; key: string } | null
  setTerminal: (value: { source: number; key: string } | null) => void
}) {
  const { locale } = useI18n(),
    guide = guideMessages[locale],
    copy = labels[locale],
    policy = policyCandidateMessages[locale]
  const cache = useQueryClient()
  const market = useCurrencyMarket()
  const [cursor, setCursor] = useState<number | null>(null)
  const [opening, setOpening] = useState(false),
    [error, setError] = useState(false)
  const open = useContext(OpenCraftingSession)
  const nodes = useMemo(
    () => new Map(view.nodes.map((n) => [n.id, n])),
    [view.nodes],
  )
  const methods = useMemo(
    () => new Map(view.methods.map((m) => [m.id, m])),
    [view.methods],
  )
  const root = nodes.get(0)
  if (!root) return null
  const frames = [],
    path: Selection[] = [],
    visited = new Set<number>()
  const states: Frame[] = [
    {
      state: root.item as ConcreteItem,
      action: null,
      rulesetIdentity: identity,
    },
  ]
  let method: AutomaticArrow | undefined = plan.root
  let completed = root.progress.goal === 'MATCH'
  let removalRisk = false
  let incomplete = false
  let manualReveal = false
  let finalNode: AutomaticNode | undefined
  while (method && frames.length < 64 && !visited.has(method.source)) {
    const current = method,
      index = frames.length
    visited.add(current.source)
    const actions = [...current.initial, ...current.cycle].map(action)
    const point = methodPoint(current, plan.cycles),
      outcomes = point?.outcomes ?? []
    const key = (o: (typeof outcomes)[number]) =>
      JSON.stringify([o.kind, o.target])
    const edge = route[index]
    const outcome =
      outcomes.find(
        (o) =>
          edge?.source === current.source &&
          edge.method === current.id &&
          edge.target === o.target &&
          edge.kind === o.kind,
      ) ??
      outcomes.find(
        (o) => terminal?.source === current.source && terminal.key === key(o),
      )
    const result =
      outcome?.target == null ? undefined : nodes.get(outcome.target)
    const outcomeName = (kind: string) =>
      kind === 'GOAL'
        ? policy.success
        : kind === 'CHECKPOINT'
          ? guide.result
          : kind === 'REBASE'
            ? policy.rebase
            : kind === 'RETRY'
              ? guide.within.replace('{n}', String(plan.cycles))
              : ['LOSS', 'DEAD'].includes(kind)
                ? policy.failure
                : policy.unresolved
    const prefix = [...path]
    const repeat =
      chaosMeanLabel(point, locale) ??
      (isRepeatable(current)
        ? guide.within.replace('{n}', String(plan.cycles))
        : guide.once)
    removalRisk ||= removesModifier(current)
    incomplete ||= !point?.complete
    frames.push(
      <li key={current.id}>
        <h4>{actions.join(' → ')}</h4>
        <span>{repeat}</span>

        {outcomes.length > 0 && (
          <label>
            {copy[3]}
            <select
              value={outcome ? key(outcome) : ''}
              onChange={(event) => {
                const o = outcomes.find(
                  (candidate) => key(candidate) === event.target.value,
                )
                setTerminal(
                  o && o.target === null
                    ? { source: current.source, key: key(o) }
                    : null,
                )
                onRoute(
                  o?.target != null
                    ? [
                        ...prefix,
                        {
                          source: current.source,
                          method: current.id,
                          target: o.target,
                          kind: o.kind,
                        },
                      ]
                    : prefix,
                )
              }}
            >
              <option value="">—</option>
              {outcomes.map((o, i) => (
                <option key={key(o)} value={key(o)}>
                  {outcomeName(o.kind)} {i + 1} ·{' '}
                  {probabilityText(o.probability.lower, o.probability)}
                </option>
              ))}
            </select>
          </label>
        )}
      </li>,
    )
    if (result && outcome) {
      const steps = projectedFrames(
        states.at(-1)!.state,
        current,
        outcome,
        result,
        plan.cycles,
        identity,
      )
      if (!steps) {
        incomplete = true
        break
      }
      states.push(...steps)
      completed = outcome.kind === 'GOAL'
      manualReveal =
        outcome.kind === 'CHECKPOINT' &&
        result.discoveryFinished &&
        !plan.decisions.has(result.id) &&
        !!result.item.desecrated?.some((slot) => slot.modifierId == null)
      if (
        manualReveal ||
        ['GOAL', 'LOSS', 'DEAD', 'REBASE'].includes(outcome.kind)
      )
        finalNode = result
    }
    if (!outcome || !result || outcome.kind !== 'CHECKPOINT') break
    path.push({
      source: current.source,
      method: current.id,
      target: result.id,
      kind: outcome.kind,
    })
    method = methods.get(plan.decisions.get(result.id) ?? '')
  }
  const chance =
    policy.success +
    ' ' +
    (plan.success
      ? probabilityText(plan.success.lower, plan.success)
      : policy.unresolved)
  const film: Film = {
    id: view.id,
    createdAt: new Date().toISOString(),
    rulesetIdentity: identity,
    frames: states,
  }
  const probability = plan.success
    ? probabilityText(plan.success.lower, plan.success)
    : policy.unresolved
  const title = planTitle(film, market?.data?.snapshot, locale, probability)
  film.title = title
  if (overview)
    return (
      <SessionOverviewPage
        id={view.id}
        source={{
          film,
          title,
          backHref:
            '/?' +
            new URLSearchParams({
              workbench: 'support',
              job: view.id,
              plan: plan.id,
            }),
          backLabel: copy[0],
        }}
      />
    )
  return (
    <section
      className="wb-history-preview session-plan-steps"
      onClickCapture={(event) => {
        const link = (event.target as Element).closest<HTMLAnchorElement>(
          'a.session-overview-entry',
        )
        if (link)
          cache.setQueryData(['plan-film', link.pathname + link.search], film)
      }}
      aria-label={sessionLibraryMessages[locale].finalState}
    >
      <div className="wb-history-preview__heading">
        <div className="wb-history-title">
          <h3>{title}</h3>
        </div>
      </div>
      <SessionDetail
        film={film}
        cursor={cursor ?? states.length - 1}
        onNavigate={(index) => {
          onInspect?.()
          setCursor(index)
        }}
        resources={historyResources}
        overviewHref={overviewHref({ identity, view }, plan, route, terminal)}
        itemClassName="wb-history-preview__item"
        actions={
          <div className="wb-history-preview__actions">
            {allowStart && open && (
              <button
                disabled={opening || !completed}
                onClick={async () => {
                  setOpening(true)
                  setError(false)
                  try {
                    setError(!(await open(film)))
                  } catch {
                    setError(true)
                  } finally {
                    setOpening(false)
                  }
                }}
              >
                {copy[2]}
              </button>
            )}
            {error && <p role="alert">{copy[4]}</p>}
          </div>
        }
      />
      <p>{chance}</p>
      {removalRisk && <p>{guide.risk}</p>}
      {incomplete && <p role="status">{policy.unresolved}</p>}
      {manualReveal && <p>{policy.end}</p>}
      <details>
        <summary>{guide.details}</summary>
        {finalNode && <p>{copy[5]}</p>}
        {plan.candidate && <CandidateSummary candidate={plan.candidate} />}
        <ol>{frames}</ol>
      </details>
    </section>
  )
}
