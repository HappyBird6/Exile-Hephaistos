import { useContext, useMemo, useState } from 'react'
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
  ],
  en: [
    'Crafting methods',
    'By success chance',
    'Start in workbench',
    'Choose outcome',
    'Could not open the crafting session.',
  ],
  'zh-CN': [
    '制作方法',
    '按成功率',
    '在工作台开始',
    '选择结果',
    '无法打开制作会话。',
  ],
  'zh-TW': [
    '製作方法',
    '依成功率',
    '在工作臺開始',
    '選擇結果',
    '無法開啟製作工作階段。',
  ],
  ja: [
    '製作方法',
    '成功率順',
    '作業台で開始',
    '結果を選択',
    '製作セッションを開けません。',
  ],
  es: [
    'Métodos de fabricación',
    'Por probabilidad de éxito',
    'Empezar en el banco',
    'Elegir resultado',
    'No se pudo abrir la sesión.',
  ],
  ru: [
    'Способы создания',
    'По шансу успеха',
    'Начать на верстаке',
    'Выберите результат',
    'Не удалось открыть сессию.',
  ],
  th: [
    'วิธีคราฟต์',
    'ตามโอกาสสำเร็จ',
    'เริ่มที่โต๊ะคราฟต์',
    'เลือกผลลัพธ์',
    'เปิดเซสชันไม่ได้',
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
  const { locale } = useI18n(),
    copy = labels[locale],
    policy = policyCandidateMessages[locale]
  const [selected, setSelected] = useState('')
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
  if (!chosen) return null
  return (
    <section className="session-plans">
      <header>
        <h3>{copy[0]}</h3>
        <span>{copy[1]}</span>
      </header>
      <div className="session-plan-layout">
        <ol aria-label={copy[0]}>
          {plans.map((p, i) => (
            <li key={p.id}>
              <button
                aria-pressed={chosen.id === p.id}
                onClick={() => {
                  setSelected(p.id)
                  props.onRoute(previewRoute(props.view, p))
                }}
              >
                <span>{i + 1}</span>
                <strong>
                  {[...p.root.initial, ...p.root.cycle]
                    .map(props.action)
                    .join(' → ')}
                </strong>
                <span>
                  {p.success
                    ? probabilityText(p.success.lower, p.success)
                    : policy.unresolved}
                </span>
              </button>
            </li>
          ))}
        </ol>
        <SessionSteps key={chosen.id} {...props} plan={chosen} />
      </div>
    </section>
  )
}

function SessionSteps({
  view,
  plan,
  action,
  card,
  onRoute,
  identity,
  allowStart,
  start,
  route,
}: Props & { plan: Plan }) {
  const { locale } = useI18n(),
    guide = guideMessages[locale],
    copy = labels[locale],
    policy = policyCandidateMessages[locale]
  const [terminal, setTerminal] = useState<{
    source: number
    key: string
  } | null>(null)
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
    visited = new Set<number>(),
    instructions: string[] = []
  let method: AutomaticArrow | undefined = plan.root
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
    frames.push(
      <li key={current.id}>
        <h4>{actions.join(' → ')}</h4>
        <span>{repeat}</span>
        {removesModifier(current) && <p>{guide.risk}</p>}
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
        {!point?.complete && <p role="status">{policy.unresolved}</p>}
        {result && <ItemCard item={card(result)} />}
      </li>,
    )
    if (!outcome || !result || outcome.kind !== 'CHECKPOINT') break
    path.push({
      source: current.source,
      method: current.id,
      target: result.id,
      kind: outcome.kind,
    })
    method = methods.get(plan.decisions.get(result.id) ?? '')
  }
  return (
    <article className="session-plan-steps">
      <h3>{guide.route}</h3>
      {plan.candidate && <CandidateSummary candidate={plan.candidate} />}
      <h4>{guide.start}</h4>
      <ItemCard item={card(root)} />
      <ol>{frames}</ol>
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
      {error && <p role="alert">{copy[4]}</p>}
    </article>
  )
}
