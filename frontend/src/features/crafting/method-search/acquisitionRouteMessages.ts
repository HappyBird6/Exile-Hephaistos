import type { Locale } from '../../../shared/i18n/i18n'

export const acquisitionRouteMessages: Record<
  Locale,
  {
    card: string
    order: string
    save: string
    transferError: string
    chance: string
    example: string
    empty: string
  }
> = {
  en: {
    save: 'Save to session library',
    transferError: 'Could not open this session. Please try again.',
    card: 'Target {n}',
    order: 'Modifier acquisition order',
    example: 'Example item',
    chance: 'Route success chance',
    empty: 'No matching crafting route found.',
  },
  ko: {
    save: '세션 보관함에 저장',
    transferError: '세션을 열 수 없습니다. 다시 시도해 주세요.',
    card: '목표 {n}',
    order: '옵션 획득 순서',
    example: '아이템 예시',
    chance: '경로 성공 확률',
    empty: '조건에 맞는 제작 경로를 찾지 못했습니다.',
  },
  'zh-CN': {
    save: '保存到会话库',
    transferError: '无法打开会话，请重试。',
    card: '目标 {n}',
    order: '词缀获取顺序',
    example: '物品示例',
    chance: '路线成功率',
    empty: '未找到符合条件的制作路线。',
  },
  'zh-TW': {
    save: '儲存至工作階段庫',
    transferError: '無法開啟工作階段，請再試一次。',
    card: '目標 {n}',
    order: '詞綴取得順序',
    example: '物品範例',
    chance: '路線成功率',
    empty: '未找到符合條件的製作路線。',
  },
  ja: {
    save: 'セッション一覧に保存',
    transferError: 'セッションを開けませんでした。もう一度お試しください。',
    card: '目標 {n}',
    order: 'モッドの獲得順',
    example: 'アイテム例',
    chance: '経路の成功率',
    empty: '条件に合う製作経路が見つかりませんでした。',
  },
  es: {
    save: 'Guardar en la biblioteca de sesiones',
    transferError: 'No se pudo abrir la sesión. Vuelve a intentarlo.',
    card: 'Objetivo {n}',
    order: 'Orden de obtención de modificadores',
    example: 'Objeto de ejemplo',
    chance: 'Probabilidad de éxito de la ruta',
    empty:
      'No se ha encontrado ninguna ruta de fabricación para estos criterios.',
  },
  ru: {
    save: 'Сохранить в библиотеку сессий',
    transferError: 'Не удалось открыть сессию. Попробуйте ещё раз.',
    card: 'Цель {n}',
    order: 'Порядок получения свойств',
    example: 'Пример предмета',
    chance: 'Вероятность успеха пути',
    empty: 'Подходящий путь создания не найден.',
  },
  th: {
    save: 'บันทึกลงคลังเซสชัน',
    transferError: 'เปิดเซสชันไม่ได้ โปรดลองอีกครั้ง',
    card: 'เป้าหมาย {n}',
    order: 'ลำดับการได้รับม็อด',
    example: 'ตัวอย่างไอเทม',
    chance: 'โอกาสสำเร็จของเส้นทาง',
    empty: 'ไม่พบเส้นทางการคราฟต์ตามเงื่อนไข',
  },
}
