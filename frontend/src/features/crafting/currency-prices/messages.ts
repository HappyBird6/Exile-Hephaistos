import type { Locale } from '../../../shared/i18n/i18n'
type Messages = {
  market: string
  updated: string
  refresh: string
  waiting: string
  unavailable: string
  stale: string
  spent: string
  incomplete: string
  unknown: string
  source: string
}
export const priceMessages: Record<Locale, Messages> = {
  en: {
    market: 'Current league exchange rate',
    updated: 'Last collected',
    refresh: 'Refresh prices',
    waiting: 'Next refresh',
    unavailable: 'Prices unavailable',
    stale: 'Last available prices',
    spent: 'Crafting cost through this step',
    incomplete: 'Partial total; unpriced materials',
    unknown: 'No price',
    source: 'Prices: poe.ninja',
  },
  ko: {
    market: '현재 리그 환율',
    updated: '마지막 수집',
    refresh: '시세 새로고침',
    waiting: '다음 갱신 가능',
    unavailable: '시세 없음',
    stale: '마지막 확인 시세',
    spent: '현재 단계까지의 제작 소모액',
    incomplete: '일부 재료의 시세를 제외한 합계',
    unknown: '시세 미확인',
    source: '시세: poe.ninja',
  },
  'zh-CN': {
    market: '当前赛季汇率',
    updated: '最后获取',
    refresh: '刷新价格',
    waiting: '下次可刷新',
    unavailable: '价格不可用',
    stale: '上次可用价格',
    spent: '截至此步骤的制作费用',
    incomplete: '部分材料无价格，合计不完整',
    unknown: '暂无价格',
    source: '价格：poe.ninja',
  },
  'zh-TW': {
    market: '目前賽季匯率',
    updated: '最後擷取',
    refresh: '重新整理價格',
    waiting: '下次可更新',
    unavailable: '價格無法取得',
    stale: '上次可用價格',
    spent: '截至此步驟的製作費用',
    incomplete: '部分材料無價格，合計不完整',
    unknown: '尚無價格',
    source: '價格：poe.ninja',
  },
  ja: {
    market: '現在のリーグの為替レート',
    updated: '最終取得',
    refresh: '価格を更新',
    waiting: '次回更新可能',
    unavailable: '価格情報なし',
    stale: '前回取得した価格',
    spent: 'この段階までの製作費用',
    incomplete: '価格不明の素材を除いた合計',
    unknown: '価格不明',
    source: '価格：poe.ninja',
  },
  es: {
    market: 'Cambio de la liga actual',
    updated: 'Última consulta',
    refresh: 'Actualizar precios',
    waiting: 'Próxima actualización',
    unavailable: 'Precios no disponibles',
    stale: 'Últimos precios disponibles',
    spent: 'Coste de fabricación hasta este paso',
    incomplete: 'Total parcial; materiales sin precio',
    unknown: 'Sin precio',
    source: 'Precios: poe.ninja',
  },
}
