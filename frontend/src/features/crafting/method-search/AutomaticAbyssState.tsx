import { useState } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import { ItemCard } from '../ItemCard'
import type { PathTreeProps } from '../path-tree/PathTree'
import { abyssCard, flowMessages } from './craftingFlow'
import {
  mass,
  type AutomaticAbyssRoute,
  type ConditionalState,
} from './automaticAbyss'
import type { AutomaticAction } from './automaticApi'

const noRemoval = {
  en: 'No removal',
  ko: '제거 없음',
  'zh-CN': '不移除',
  'zh-TW': '不移除',
  ja: '除去なし',
  es: 'Sin eliminación',

  ru: 'Без удаления',
  th: 'ไม่มีการกำจัด',
}
const labels = {
  en: [
    'Automatically planned desecration',
    'Complete one side',
    'Block a high-weight modifier',
    'Share removal risk',
    'Conditional state',
    'Removal',
    'Modifier selection',
    'Result states',
    'Available next actions',
    'Ranges are not observed rolls. Reveal chance and total success are unknown.',
  ],
  ko: [
    '자동 탐색 훼손',
    '한쪽 속성 채우기',
    '고가중치 옵션 차단',
    '제거 위험 분산',
    '조건부 상태',
    '제거',
    '속성 종류 추첨',
    '결과 상태',
    '가능한 후속 작업',
    '범위는 관측된 수치가 아닙니다. 드러내기 확률과 전체 성공률은 미확정입니다.',
  ],
  'zh-CN': [
    '自动规划亵渎',
    '填满一侧',
    '阻挡高权重词缀',
    '分担移除风险',
    '条件状态',
    '移除',
    '词缀抽取',
    '结果状态',
    '可用后续操作',
    '范围不是已观察数值。揭示概率和总成功率未知。',
  ],
  'zh-TW': [
    '自動規劃褻瀆',
    '填滿一側',
    '阻擋高權重詞綴',
    '分擔移除風險',
    '條件狀態',
    '移除',
    '詞綴抽取',
    '結果狀態',
    '可用後續操作',
    '範圍不是已觀察數值。揭示機率和總成功率未知。',
  ],
  ja: [
    '自動探索の冒涜',
    '片側を埋める',
    '高ウェイトのモッドをブロック',
    '除去リスクを分散',
    '条件付き状態',
    '除去',
    'モッド抽選',
    '結果の状態',
    '可能な次の操作',
    '範囲は観測値ではありません。公開確率と全体成功率は不明です。',
  ],
  es: [
    'Profanación planificada',
    'Completar un lado',
    'Bloquear un modificador de peso alto',
    'Repartir el riesgo de eliminación',
    'Estado condicional',
    'Eliminación',
    'Selección de modificador',
    'Estados resultantes',
    'Acciones siguientes disponibles',
    'Los rangos no son valores observados. Se desconocen la probabilidad de revelado y el éxito total.',
  ],

  ru: [
    'Автоматически запланированное осквернение',
    'Завершить одну сторону',
    'Блокировать модификатор с большим весом',
    'Риск удаления доли',
    'Условное состояние',
    'Удаление',
    'Выбор модификатора',
    'Результаты состояния',
    'Доступные следующие действия',
    'На полигонах не наблюдаются перекаты. Шанс раскрытия и общий успех неизвестны.',
  ],
  th: [
    'การดูหมิ่นตามแผนโดยอัตโนมัติ',
    'เสร็จด้านหนึ่ง',
    'บล็อกม็อดที่มีน้ำหนักสูง',
    'แบ่งปันความเสี่ยงในการถอดออก',
    'สภาวะที่มีเงื่อนไข',
    'การกำจัด',
    'การเลือกม็อด',
    'สถานะผลลัพธ์',
    'การดำเนินการต่อไปที่มีอยู่',
    'ระยะจะไม่สังเกตการม้วน ไม่ทราบโอกาสและความสำเร็จทั้งหมดเปิดเผย',
  ],
}
export function AutomaticAbyssState({
  route,
  presentItem,
  action,
}: {
  route: AutomaticAbyssRoute
  presentItem: PathTreeProps['presentItem']
  action: (a: AutomaticAction) => string
}) {
  const { locale } = useI18n()
  const t = labels[locale],
    flow = flowMessages[locale]
  const [opened, setOpened] = useState(false)
  const card = (s: ConditionalState) => {
    const low = abyssCard(s.item, presentItem, locale)
    const high = abyssCard(
      {
        ...s.item,
        slots: s.item.slots.map((slot) => {
          const ranges = slot.modifier && s.ranges[slot.modifier.modifierId]
          return ranges && slot.modifier
            ? {
                ...slot,
                modifier: {
                  ...slot.modifier,
                  values: {
                    ...slot.modifier.values,
                    ...Object.fromEntries(ranges.map((r) => [r.stat, r.max])),
                  },
                },
              }
            : slot
        }),
      },
      presentItem,
      locale,
    )
    return {
      ...low,
      modifiers: low.modifiers.map((line, index) => {
        const end = high.modifiers[index]
        return end && end.text !== line.text
          ? { ...line, text: `${line.text} — ${end.text}` }
          : line
      }),
    }
  }
  const pct = (n: number) => `${(n * 100).toFixed(2)}%`
  const comparison = route.strategy.modifierSelection
  return (
    <section
      className="graph-item-state"
      data-automatic-abyss={route.strategy.kind}
    >
      <strong>
        {t[0]} ·{' '}
        {
          t[
            1 +
              [
                'COMPLETE_SIDE',
                'BLOCK_HIGH_WEIGHT',
                'SHARE_REMOVAL_RISK',
              ].indexOf(route.strategy.kind)
          ]
        }
      </strong>
      <p>{t[9]}</p>
      <ItemCard item={card(route.prepared)} />
      {comparison && (
        <p>
          {t[6]}:{' '}
          {pct(
            comparison.baseline.targetWeight / comparison.baseline.totalWeight,
          )}
          (~
          {pct(
            comparison.afterBlocking.targetWeight /
              comparison.afterBlocking.totalWeight,
          )}
          )
        </p>
      )}
      {route.assumed && (
        <>
          <strong>
            {t[4]} · {flow.choose}
          </strong>
          <p>{flow.unknown}</p>
          <ItemCard item={card(route.assumed)} />
        </>
      )}
      <button aria-expanded={opened} onClick={() => setOpened(!opened)}>
        {t[7]}
      </button>
      {opened &&
        route.transitions.map((transition, i) => (
          <div key={i}>
            <strong>
              {action({
                currency: transition.currency,
                omens: transition.omens,
                purpose: 'ORDINARY',
              })}{' '}
              ·{' '}
              {transition.source.condition === 'IF_REVEAL_CHOOSES_MODIFIER'
                ? t[4]
                : flow.prepare}
            </strong>
            {transition.removals.map((removal, j) => (
              <details key={j}>
                <summary>
                  {removal.slotId === null
                    ? noRemoval[locale]
                    : `${t[5]} ${j + 1}`}
                  : {pct(mass(removal.probability))} ·{' '}
                  {removal.candidates.length}
                </summary>
                <ItemCard item={card(removal.remaining)} />
                {removal.candidates.map((candidate) => (
                  <details key={candidate.modifierId}>
                    <summary>
                      {t[6]}: {pct(mass(candidate.selection))} ·{' '}
                      {
                        card(candidate.result).modifiers.find(
                          (line) => line.id === candidate.modifierId,
                        )?.text
                      }
                    </summary>
                    <ItemCard item={card(candidate.result)} />
                    <p>
                      {t[8]}:{' '}
                      {candidate.next
                        .map((n) => action({ ...n, purpose: 'ORDINARY' }))
                        .join(' · ')}
                    </p>
                  </details>
                ))}
              </details>
            ))}
          </div>
        ))}
    </section>
  )
}
