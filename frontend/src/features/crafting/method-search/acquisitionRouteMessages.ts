import type { Locale } from '../../../shared/i18n/i18n'

export const acquisitionRouteMessages: Record<
  Locale,
  { card: string; order: string; chance: string; empty: string }
> = {
  en: {
    card: 'Target {n}',
    order: 'Modifier acquisition order',
    chance: 'Route success chance',
    empty: 'No matching crafting route found.',
  },
  ko: {
    card: '목표 {n}',
    order: '옵션 획득 순서',
    chance: '경로 성공 확률',
    empty: '조건에 맞는 제작 경로를 찾지 못했습니다.',
  },
  'zh-CN': {
    card: '目标 {n}',
    order: '词缀获取顺序',
    chance: '路线成功率',
    empty: '未找到符合条件的制作路线。',
  },
  'zh-TW': {
    card: '目標 {n}',
    order: '詞綴取得順序',
    chance: '路線成功率',
    empty: '未找到符合條件的製作路線。',
  },
  ja: {
    card: '目標 {n}',
    order: 'モッドの獲得順',
    chance: '経路の成功率',
    empty: '条件に合う製作経路が見つかりませんでした。',
  },
  es: {
    card: 'Objetivo {n}',
    order: 'Orden de obtención de modificadores',
    chance: 'Probabilidad de éxito de la ruta',
    empty:
      'No se ha encontrado ninguna ruta de fabricación para estos criterios.',
  },
  ru: {
    card: 'Цель {n}',
    order: 'Порядок получения свойств',
    chance: 'Вероятность успеха пути',
    empty: 'Подходящий путь создания не найден.',
  },
  th: {
    card: 'เป้าหมาย {n}',
    order: 'ลำดับการได้รับม็อด',
    chance: 'โอกาสสำเร็จของเส้นทาง',
    empty: 'ไม่พบเส้นทางการคราฟต์ตามเงื่อนไข',
  },
}
