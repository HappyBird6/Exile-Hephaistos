import type { Locale } from '../../../shared/i18n/i18n'

export const chaosFirstAffixHints: Record<Locale, string> = {
  en: 'Automatic Chaos is only used at the start on a rare item with exactly one explicit modifier, repeating to obtain the first desired modifier. The default probability is within 100 attempts. Manual Workbench use is separate.',
  ko: '자동 카오스는 제작 시작에 명시 속성이 정확히 1개인 희귀 아이템에서 원하는 첫 속성을 얻기 위해 반복할 때만 사용합니다. 기본 확률은 100회 안의 성공 확률이며 수동 Workbench 사용은 별개입니다.',
  'zh-CN':
    '自动搜索仅在制作开始时，对恰好有一个显式词缀的稀有物品重复使用混沌石，以获得第一个所需词缀。默认显示100次内的成功概率。手动工作台另行处理。',
  'zh-TW':
    '自動搜尋僅在製作開始時，對恰好有一個明確詞綴的稀有物品重複使用混沌石，以取得第一個所需詞綴。預設顯示100次內的成功機率。手動工作台另行處理。',
  ja: '自動探索のカオスは、製作開始時に明示的モッドがちょうど1個あるレアアイテムで、最初の目的モッドを得るための反復にのみ使用します。既定は100回以内の成功確率です。手動Workbenchは別です。',
  es: 'El Caos automático solo se usa al inicio, en un objeto raro con exactamente un modificador explícito, repitiendo hasta obtener el primero deseado. Se muestra por defecto la probabilidad en 100 intentos. El Workbench manual es independiente.',
}
