import type { Locale } from '../../../shared/i18n/i18n'
export const chaosFirstAffixHints: Record<Locale, string> = {
  en: 'Automatic Chaos starts on Rare equipment with exactly one mutable explicit modifier. Fractured modifiers stay fixed and do not count toward this limit. Two mutable modifiers or later re-entry are excluded. The default probability covers 100 attempts.',
  ko: '자동 카오스는 분열을 제외한 변경 가능한 명시 속성이 1개인 희귀 아이템에서 시작합니다. 분열은 보존하며 분열 목표를 달성했어도 첫 일반 목표 속성 획득을 허용합니다. 일반 속성 2개 이상과 후반 재진입은 제외합니다. 기본 확률은 100회 이내입니다.',
  'zh-CN':
    '自动混沌从只有一个可变显式词缀的稀有装备开始。破裂词缀保持不变且不计入限制。两个可变词缀及后期重新进入不适用。默认概率涵盖100次尝试。',
  'zh-TW':
    '自動混沌從只有一個可變顯式詞綴的稀有裝備開始。破裂詞綴保持不變且不計入限制。兩個可變詞綴及後期重新進入不適用。預設機率涵蓋100次嘗試。',
  ja: '自動カオスは変更可能な明示モッドが1個のレア装備から開始します。フラクチャーモッドは固定され、この制限に数えません。変更可能なモッドが2個以上、または後半の再入は対象外です。既定の確率は100回以内です。',
  es: 'El Caos automático empieza con un modificador explícito modificable en equipo raro. Los fracturados permanecen fijos y no cuentan para el límite. Se excluyen dos modificadores modificables y el reingreso posterior. La probabilidad predeterminada cubre 100 intentos.',

  ru: 'Автоматический хаос начинается на редком оборудовании только с одним изменяемым явным модификатором. Модификаторы переломов остаются фиксированными и не учитываются в этом пределе. Два изменяемых модификатора или более поздний повторный ввод исключаются. Вероятность по умолчанию охватывает 100 попыток.',
  th: 'ความโกลาหลอัตโนมัติเริ่มต้นบนอุปกรณ์ระดับแรร์โดยมีม็อดที่ชัดเจนที่ไม่แน่นอนเพียงตัวเดียว ม็อดที่ร้าวจะยังคงอยู่และไม่นับรวมในขีดจำกัดนี้ ไม่รวมม็อดที่ไม่แน่นอนสองตัวหรือการกลับรายการใหม่ในภายหลัง ความน่าจะเป็นเริ่มต้นครอบคลุมความพยายาม 100 ครั้ง',
}
