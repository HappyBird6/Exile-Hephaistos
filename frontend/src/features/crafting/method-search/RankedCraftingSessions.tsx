import { AppLink } from '../../../shared/navigation/AppLink'
import { HistoryWorkspaceLayout } from '../session-library/HistoryWorkspaceLayout'
import { SessionDetail } from '../session-detail/SessionDetail'
import {
  SessionOverviewLayout,
  SessionFlowSurface,
} from '../session-history/SessionOverviewLayout'
import { NotationResources } from '../session-notation/SessionNotation'
import { historyResources } from '../session-history/resources'
import { currencyId } from '../session-notation/extractSessionNotation'
import { planOmenId } from './usePlanAction'
import { SessionFlowGrid } from '../session-history/SessionFlow'
import '../session-history/history.css'
import { SessionListCard } from '../session-library/SessionListCard'
import { sessionLibraryMessages } from '../session-library/sessionLibraryMessages'
import { historyMessages } from '../session-history/messages'
import { useContext, useMemo, useRef, useState } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import { ItemCard } from '../ItemCard'
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
import { methodPoint, isRepeatable } from './methodObservation'
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
    '예상 화폐 사용 이력',
    '선택한 경로에서 나올 수 있는 최종 아이템 예시입니다. 성공 확률은 이 방법으로 목표에 도달하는 모든 결과를 합산합니다.',
  ],
  en: [
    'Crafting methods',
    'By success chance',
    'Start in workbench',
    'Choose outcome',
    'Could not open the crafting session.',
    'Planned currency history',
    'One possible final item on the selected route. The success chance includes every result that meets the goal.',
  ],
  'zh-CN': [
    '制作方法',
    '按成功率',
    '在工作台开始',
    '选择结果',
    '无法打开制作会话。',
    '预计通货使用记录',
    '展示所选路线的一种最终物品。成功率包含所有达成目标的结果。',
  ],
  'zh-TW': [
    '製作方法',
    '依成功率',
    '在工作臺開始',
    '選擇結果',
    '無法開啟製作工作階段。',
    '預計通貨使用紀錄',
    '顯示所選路線的一種最終物品。成功率包含所有達成目標的結果。',
  ],
  ja: [
    '製作方法',
    '成功率順',
    '作業台で開始',
    '結果を選択',
    '製作セッションを開けません。',
    '通貨の使用予定',
    '選択した経路の最終アイテムの一例です。成功率は目標を達成するすべての結果を含みます。',
  ],
  es: [
    'Métodos de fabricación',
    'Por probabilidad de éxito',
    'Empezar en el banco',
    'Elegir resultado',
    'No se pudo abrir la sesión.',
    'Historial previsto de divisas',
    'Un objeto final posible en la ruta elegida. La probabilidad incluye todos los resultados que cumplen el objetivo.',
  ],
  ru: [
    'Способы создания',
    'По шансу успеха',
    'Начать на верстаке',
    'Выберите результат',
    'Не удалось открыть сессию.',
    'План расхода валюты',
    'Пример итогового предмета на выбранном пути. Шанс успеха учитывает все исходы, достигающие цели.',
  ],
  th: [
    'วิธีคราฟต์',
    'ตามโอกาสสำเร็จ',
    'เริ่มที่โต๊ะคราฟต์',
    'เลือกผลลัพธ์',
    'เปิดเซสชันไม่ได้',
    'ประวัติการใช้สกุลเงินตามแผน',
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
  const choose = (plan: Plan) => {
    setTerminal(null)
    setEdited(false)
    setSelected(plan.id)
    props.onRoute(previewRoute(props.view, plan))
  }
  const plans = useMemo(
    () => rankedPlans(props.view, props.cycles),
    [props.view, props.cycles],
  )
  const chosen =
    plans.find((p) => p.id === selected) ??
    plans.find(
      (p) =>
        props.route.length > 0 &&
        props.route.every(
          (edge) => p.decisions.get(edge.source) === edge.method,
        ),
    ) ??
    plans[0]
  const route = useMemo(
    () =>
      chosen &&
      !edited &&
      (!props.route.length ||
        props.route.some(
          (edge) => chosen.decisions.get(edge.source) !== edge.method,
        ))
        ? previewRoute(props.view, chosen)
        : props.route,
    [chosen, edited, props.route, props.view],
  )
  const detailProps = {
    ...props,
    terminal,
    setTerminal,
    route,
    onRoute: (value: Selection[]) => {
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
                const title = [...plan.root.initial, ...plan.root.cycle]
                  .map(props.action)
                  .join(' → ')
                const probability = plan.success
                  ? probabilityText(plan.success.lower, plan.success)
                  : policy.unresolved
                return (
                  <div role="listitem" key={plan.id}>
                    <SessionListCard
                      title={title}
                      baseId={props.start.baseItemId}
                      baseName={baseName}
                      selected={chosen.id === plan.id}
                      selectLabel={`${index + 1}. ${baseName} · ${title} · ${probability}`}
                      onSelect={() => {
                        choose(plan)
                        if (window.innerWidth <= 760)
                          preview.current
                            ?.querySelector('.wb-history-preview')
                            ?.scrollIntoView?.({ block: 'nearest' })
                      }}
                      metadata={
                        <small>
                          {baseName} · {policy.success} {probability}
                        </small>
                      }
                      actions={
                        <AppLink
                          className="wb-session-card__overview"
                          href={overviewHref(
                            props,
                            plan,
                            chosen.id === plan.id
                              ? route
                              : previewRoute(props.view, plan),
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
  card,
  onRoute,
  identity,
  allowStart,
  start,
  route,
  terminal,
  setTerminal,
  overview = false,
}: Props & {
  plan: Plan
  overview?: boolean
  terminal: { source: number; key: string } | null
  setTerminal: (value: { source: number; key: string } | null) => void
}) {
  const { locale, name } = useI18n(),
    guide = guideMessages[locale],
    copy = labels[locale],
    policy = policyCandidateMessages[locale]
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
  const notation = []
  const frames = [],
    path: Selection[] = [],
    visited = new Set<number>(),
    instructions: string[] = []
  let method: AutomaticArrow | undefined = plan.root
  function itemCard(node: AutomaticNode) {
    const presentation = card(node)
    return (
      <ItemCard
        baseItemId={node.item.baseItemId}
        item={{
          ...presentation,
          name: name(node.item.baseItemId, presentation.name),
          base: null,
        }}
      />
    )
  }
  const flow: {
    card: React.ReactNode
    label: string
    resources?: React.ReactNode
  }[] = [{ card: itemCard(root), label: guide.start }]
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
    const repeat = isRepeatable(current)
      ? guide.within.replace('{n}', String(plan.cycles))
      : guide.once
    instructions.push(...current.initial.map(action))
    if (current.cycle.length)
      instructions.push(current.cycle.map(action).join(' → ') + ' · ' + repeat)
    const groupIcons = []
    for (const craft of [...current.initial, ...current.cycle]) {
      const isCurrency =
        craft.purpose !== 'QUALITY_CHECKPOINT' &&
        craft.purpose !== 'QUALITY_BASE_CHECKPOINT'
      const id =
        isCurrency && craft.currency ? currencyId(craft.currency) : undefined
      const label = action(craft)
      const stepNumber = notation.length + 1
      const icons = id && (
        <NotationResources
          resources={historyResources}
          step={{
            number: stepNumber,
            frameIndex: stepNumber,
            currencyId: id,
            consumedOmenIds: craft.omens.map(planOmenId),
            omensKnown: true,
            uses: 1,
            repeated: false,
          }}
        />
      )
      const limit =
        isRepeatable(current) && current.cycle.includes(craft)
          ? ' · ' + repeat
          : ''
      notation.push(
        <li
          key={stepNumber}
          className="wb-session-notation__step"
          aria-label={label + limit}
          title={label + limit}
        >
          {icons ?? label}
          {limit}
        </li>,
      )
      groupIcons.push(
        <span key={stepNumber} title={label}>
          {icons ?? label}
        </span>,
      )
    }
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
      flow.push({
        card: itemCard(result),
        resources: <div className="session-flow-resources">{groupIcons}</div>,
        label:
          actions.join(' → ') +
          ' · ' +
          repeat +
          ' · ' +
          probabilityText(outcome.probability.lower, outcome.probability),
      })
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
  const title = [...plan.root.initial, ...plan.root.cycle]
    .map(action)
    .join(' → ')
  const chance =
    policy.success +
    ' ' +
    (plan.success
      ? probabilityText(plan.success.lower, plan.success)
      : policy.unresolved)
  if (overview)
    return (
      <SessionOverviewLayout
        title={title}
        backHref={
          '/?' +
          new URLSearchParams({
            workbench: 'support',
            job: view.id,
            plan: plan.id,
          })
        }
        backLabel={copy[0]}
        summary={<span className="history-session-summary">{chance}</span>}
      >
        <SessionFlowSurface>
          <SessionFlowGrid frames={flow} summary={<strong>{chance}</strong>} />
        </SessionFlowSurface>
      </SessionOverviewLayout>
    )
  return (
    <section
      className="wb-history-preview session-plan-steps"
      aria-label={sessionLibraryMessages[locale].finalState}
    >
      <div className="wb-history-preview__heading">
        <div className="wb-history-title">
          <h3>{title}</h3>
        </div>
      </div>
      <SessionDetail
        cursor={0}
        itemClassName="wb-history-preview__item"
        item={finalNode ? itemCard(finalNode) : <p>{policy.unresolved}</p>}
        projectedRecord={
          <>
            <p>{chance}</p>
            {removalRisk && <p>{guide.risk}</p>}
            {incomplete && <p role="status">{policy.unresolved}</p>}
            {manualReveal && <p>{policy.end}</p>}
            <section className="wb-session-notation" aria-label={copy[5]}>
              <div className="wb-session-notation__heading">
                <h3>{copy[5]}</h3>
              </div>
              <ol className="wb-session-notation__steps wb-scrollbar">
                {notation}
              </ol>
            </section>
            <details>
              <summary>{guide.details}</summary>
              {finalNode && <p>{copy[6]}</p>}
              {plan.candidate && (
                <CandidateSummary candidate={plan.candidate} />
              )}
              <ol>{frames}</ol>
            </details>
          </>
        }
        actions={
          <div className="wb-history-preview__actions">
            {allowStart && open && (
              <button
                disabled={opening}
                onClick={async () => {
                  setOpening(true)
                  setError(false)
                  try {
                    setError(!(await open(start, identity, instructions)))
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
            <AppLink
              href={overviewHref({ identity, view }, plan, route, terminal)}
            >
              {historyMessages[locale].title} <span aria-hidden="true">↗</span>
            </AppLink>
            {error && <p role="alert">{copy[4]}</p>}
          </div>
        }
      />
    </section>
  )
}
