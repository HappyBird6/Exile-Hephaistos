import type { AutomaticView } from './automaticApi'
import { useI18n } from '../../../shared/i18n/i18n'
const labels = {
  en: [
    'Calculation states',
    'Method calculations',
    'Stored records',
    'Concrete item outcomes',
    'Limit reached',
  ],
  ko: [
    '계산 상태',
    '제작 방법 계산',
    '보관 항목',
    '구체 아이템 결과',
    '도달한 한도',
  ],
  'zh-CN': [
    '计算状态',
    '制作方法计算',
    '存储记录',
    '具体物品结果',
    '已达到上限',
  ],
  'zh-TW': [
    '計算狀態',
    '製作方法計算',
    '儲存記錄',
    '具體物品結果',
    '已達到上限',
  ],
  ja: [
    '計算状態',
    '製作方法の計算',
    '保存項目',
    '具体的なアイテム結果',
    '到達した上限',
  ],
  es: [
    'Estados de cálculo',
    'Cálculos de métodos',
    'Registros guardados',
    'Resultados concretos',
    'Límite alcanzado',
  ],
} as const
const detailLabels = {
  en: ['Calculation details', 'Primitive outcomes'],
  ko: ['계산 세부 정보', '단일 화폐 전이 결과'],
  'zh-CN': ['计算详情', '单次货币转换结果'],
  'zh-TW': ['計算詳情', '單次通貨轉換結果'],
  ja: ['計算の詳細', '単一通貨の遷移結果'],
  es: ['Detalles del cálculo', 'Resultados de una transición'],
} as const
export function SearchResources({ view }: { view: AutomaticView }) {
  const { locale } = useI18n()
  const r = view.resources
  if (!r) return null
  const copy = labels[locale]
  return (
    <details className="search-resource-details">
      <summary>{detailLabels[locale][0]}</summary>
      <div className="search-resources">
        <span>
          {copy[0]}: {r.computationStates} / {r.stateLimit}
        </span>
        <span>
          {copy[1]}: {r.methodExecutions} / {r.methodLimit}
        </span>
        <span>
          {copy[2]}: {r.retainedRecords} / {r.retainedLimit}
        </span>
        <span>
          {copy[3]}: {r.concreteStates}
        </span>
        {r.blockers.length > 0 && (
          <span>
            {copy[4]}:{' '}
            {r.blockers
              .map((b) =>
                b === 'STATES'
                  ? copy[0]
                  : b === 'METHODS'
                    ? copy[1]
                    : b === 'RETAINED_RECORDS'
                      ? copy[2]
                      : detailLabels[locale][1],
              )
              .join(', ')}
          </span>
        )}
      </div>
    </details>
  )
}
