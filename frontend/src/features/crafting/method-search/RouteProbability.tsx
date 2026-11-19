import { useI18n } from '../../../shared/i18n/i18n'
import type { Fraction } from '../path-tree/types'
import type { Bounds } from './types'
import { probabilityText } from './probabilityPresentation'

const labels = {
  ko: [
    '단계별 확률',
    '선택 경로의 도달 확률',
    '목표 달성 확률 = 모든 성공 경로 확률의 합',
    '첫 목표 옵션 획득 100% · 이 옵션이 먼저 나올 확률',
    '다른 성공 경로',
  ],
  en: [
    'Step probabilities',
    'Chance of reaching this point',
    'Goal chance = sum of all successful path probabilities',
    'First target guaranteed · chance of this target appearing first',
    'Other successful paths',
  ],
  ja: [
    '段階別の確率',
    '選択した経路の到達確率',
    '目標達成率 = 全成功経路の確率の和',
    '最初の目標獲得は100% · この目標が先に出る確率',
    'その他の成功経路',
  ],
  'zh-CN': [
    '各步骤概率',
    '所选路线的到达概率',
    '目标达成率 = 所有成功路线的概率之和',
    '必得首个目标 · 此目标先出现的概率',
    '其他成功路线',
  ],
  'zh-TW': [
    '各步驟機率',
    '所選路線的到達機率',
    '目標達成率 = 所有成功路線的機率總和',
    '必得首個目標 · 此目標先出現的機率',
    '其他成功路線',
  ],
  es: [
    'Probabilidad por paso',
    'Probabilidad de llegar a este punto',
    'Probabilidad del objetivo = suma de las probabilidades de las rutas exitosas',
    'Primer objetivo garantizado · probabilidad de que este salga primero',
    'Otras rutas exitosas',
  ],
  ru: [
    'Вероятность по шагам',
    'Шанс дойти до этого шага',
    'Шанс достижения цели = сумма вероятностей всех успешных путей',
    'Первая цель гарантирована · шанс получить эту цель первой',
    'Другие успешные пути',
  ],
  th: [
    'โอกาสในแต่ละขั้น',
    'โอกาสมาถึงขั้นนี้',
    'โอกาสบรรลุเป้าหมาย = ผลรวมโอกาสของทุกเส้นทางที่สำเร็จ',
    'ได้เป้าหมายแรกแน่นอน · โอกาสที่เป้าหมายนี้จะออกก่อน',
    'เส้นทางสำเร็จอื่น',
  ],
} as const

export type ProbabilityStep = {
  first: number
  last: number
  label: string
  probability: Pick<Bounds, 'lower' | 'upper'>
  firstChaos?: boolean
}

function remaining(total: Fraction, selected: Fraction): Fraction {
  const top =
    BigInt(total.numerator) * BigInt(selected.denominator) -
    BigInt(selected.numerator) * BigInt(total.denominator)
  return {
    numerator: String(top > 0n ? top : 0n),
    denominator: String(
      BigInt(total.denominator) * BigInt(selected.denominator),
    ),
  }
}

export function RouteProbability({
  steps,
  success,
  completed,
}: {
  steps: ProbabilityStep[]
  completed: boolean
  success: Bounds | undefined
}) {
  const { locale } = useI18n()
  if (!steps.length) return null
  const [heading, routeLabel, totalLabel, chaosLabel, otherLabel] =
    labels[locale]
  const product = (key: 'lower' | 'upper') => {
    let numerator = 1n,
      denominator = 1n
    for (const step of steps) {
      numerator *= BigInt(step.probability[key].numerator)
      denominator *= BigInt(step.probability[key].denominator)
    }
    return { numerator: String(numerator), denominator: String(denominator) }
  }
  const route = { lower: product('lower'), upper: product('upper') }
  const routeChance = probabilityText(route.lower, route)
  const totalChance = success && probabilityText(success.lower, success)
  const other =
    completed &&
    success &&
    routeChance !== totalChance &&
    BigInt(success.upper.numerator) * BigInt(route.lower.denominator) >=
      BigInt(route.lower.numerator) * BigInt(success.upper.denominator)
      ? {
          lower: remaining(success.lower, route.upper),
          upper: remaining(success.upper, route.lower),
        }
      : null
  const factors = steps.map(({ probability }) =>
    probabilityText(probability.lower, probability),
  )
  return (
    <section className="route-probability" aria-label={heading}>
      <h4>{heading}</h4>
      <ul>
        {steps.map((s, i) => (
          <li key={s.first}>
            <span>
              {s.first === s.last ? s.first : s.first + '–' + s.last}. {s.label}
              {s.firstChaos && <> · {chaosLabel}</>}
            </span>
            <strong>{factors[i]}</strong>
          </li>
        ))}
      </ul>
      <p>
        {routeLabel}
        <br />
        {factors
          .map((f) => (f.includes(' – ') ? '(' + f + ')' : f))
          .join(' × ')}{' '}
        ≈ {routeChance}
      </p>
      {success && (
        <p>
          {totalLabel}
          {other && (
            <>
              <br />
              {routeChance} + {probabilityText(other.lower, other)} (
              {otherLabel})
            </>
          )}{' '}
          ≈ {totalChance}
        </p>
      )}
    </section>
  )
}
