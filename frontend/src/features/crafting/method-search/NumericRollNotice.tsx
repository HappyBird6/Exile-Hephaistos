import { useI18n } from '../../../shared/i18n/i18n'
import type { Locale } from '../../../shared/i18n/i18n'

const messages: Record<Locale, [string, string]> = {
  en: [
    'Model probability: correlations between numeric rolls are not verified in the game.',
    'The existing assumed model shares one of 10,001 equally weighted ratio ticks (0–10000) across all numbers, with HALF_UP rounding. Rounded tuples have unequal weights; the numbers are not rolled independently.',
  ],
  ko: [
    '모델 확률: 복수 수치 롤의 상관관계는 게임에서 검증되지 않았습니다.',
    '기존 가정 모델은 모든 수치에 0~10000의 균등한 비율 틱 10001개 중 하나를 공유하고 HALF_UP 반올림합니다. 반올림된 수치 조합의 가중치는 서로 다르며, 각 수치를 독립적으로 뽑지 않습니다.',
  ],
  'zh-CN': [
    '模型概率：多个数值掷值之间的相关性未经游戏验证。',
    '现有假设模型对所有数值使用同一个等权比例刻度（0–10000，共10001个），并采用HALF_UP舍入。舍入后组合的权重不同，各数值并非独立抽取。',
  ],
  'zh-TW': [
    '模型機率：多個數值擲值之間的相關性未經遊戲驗證。',
    '既有假設模型對所有數值使用同一個等權比例刻度（0–10000，共10001個），並採用HALF_UP捨入。捨入後組合的權重不同，各數值並非獨立抽取。',
  ],
  ja: [
    'モデル上の確率：複数の数値ロール間の相関はゲーム内で未検証です。',
    '既存の仮定モデルでは、全数値に等しい重みの比率刻み（0～10000の10001個）を1つ共有し、HALF_UPで丸めます。丸めた組合せの重みは均一ではなく、数値を独立に抽選しません。',
  ],
  es: [
    'Probabilidad del modelo: la correlación entre tiradas numéricas no está verificada en el juego.',
    'El modelo supuesto existente comparte un único punto de proporción equiprobable (0–10000, 10001 puntos) entre todos los valores, con redondeo HALF_UP. Las combinaciones redondeadas tienen pesos distintos; los valores no se sortean de forma independiente.',
  ],
}

const titles: Record<Locale, string> = {
  en: 'Calculation assumptions',
  ko: '계산 가정',
  'zh-CN': '计算假设',
  'zh-TW': '計算假設',
  ja: '計算の仮定',
  es: 'Supuestos del cálculo',
}

export function NumericRollNotice({
  models,
}: {
  models?: string[] | undefined
}) {
  const { locale } = useI18n()
  if (!models?.includes('user-coupled-ratio-half-up-v1')) return null
  const [summary, detail] = messages[locale]
  return (
    <aside role="note" className="numeric-roll-notice">
      <p>{summary}</p>
      <details>
        <summary>{titles[locale]}</summary>
        <p>{detail}</p>
      </details>
    </aside>
  )
}
