import type { Locale } from '../../../shared/i18n/i18n'

export const previewTabNames: Record<Locale, string> = {
  en: 'Test Workbench',
  ko: '테스트 제작작업대',
  'zh-CN': '测试制作台',
  'zh-TW': '測試製作台',
  ja: 'テスト作業台',
  es: 'Banco de pruebas',
  ru: 'Тестовый верстак',
  th: 'โต๊ะคราฟต์ทดสอบ',
}

export const previewGoalHints: Record<Locale, string> = {
  en: 'Choose a goal to see matching example items and where their modifiers come from.',
  ko: '목표를 설정하면 조건에 맞는 아이템 예시와 각 옵션의 획득처가 표시됩니다.',
  'zh-CN': '设置目标后可查看符合条件的物品示例和各词缀的获取途径。',
  'zh-TW': '設定目標即可查看符合條件的物品範例和每個詞綴的取得方式。',
  ja: '目標を設定すると、条件に合うアイテムの例と各モッドの取得方法が表示されます。',
  es: 'Elige un objetivo para ver objetos de ejemplo que lo cumplen y cómo obtener sus modificadores.',
  ru: 'Укажите цель, чтобы увидеть примеры подходящих предметов и источники их модификаторов.',
  th: 'ตั้งเป้าหมายเพื่อดูตัวอย่างไอเทมที่ตรงตามเงื่อนไขและวิธีรับม็อดแต่ละรายการ',
}
