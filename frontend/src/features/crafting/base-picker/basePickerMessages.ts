import type { Locale } from '../../../shared/i18n/i18n'

type Messages = {
  all: string
  category: string
  subtype: string
  weapons: string
  armour: string
  accessories: string
  jewels: string
  empty: string
  preview: string
  armourValue: string
  evasionValue: string
  energyShieldValue: string
}
export const basePickerMessages: Record<Locale, Messages> = {
  en: {
    all: 'All',
    category: 'Equipment category',
    subtype: 'Equipment type',
    weapons: 'Weapons',
    armour: 'Armour',
    accessories: 'Accessories',
    jewels: 'Jewels',
    empty: 'No matching bases.',
    preview: 'Base information',
    armourValue: 'Armour',
    evasionValue: 'Evasion Rating',
    energyShieldValue: 'Energy Shield',
  },
  ko: {
    all: '전체',
    category: '장비 카테고리',
    subtype: '장비 소분류',
    weapons: '무기',
    armour: '방어구',
    accessories: '장신구',
    jewels: '주얼',
    empty: '일치하는 베이스가 없습니다.',
    preview: '베이스 정보',
    armourValue: '방어도',
    evasionValue: '회피',
    energyShieldValue: '에너지 보호막',
  },
  'zh-CN': {
    all: '全部',
    category: '装备类别',
    subtype: '装备类型',
    weapons: '武器',
    armour: '护甲',
    accessories: '饰品',
    jewels: '珠宝',
    empty: '没有匹配的基底。',
    preview: '基底信息',
    armourValue: '护甲',
    evasionValue: '闪避值',
    energyShieldValue: '能量护盾',
  },
  'zh-TW': {
    all: '全部',
    category: '裝備類別',
    subtype: '裝備類型',
    weapons: '武器',
    armour: '護甲',
    accessories: '飾品',
    jewels: '珠寶',
    empty: '沒有符合的基底。',
    preview: '基底資訊',
    armourValue: '護甲',
    evasionValue: '閃避值',
    energyShieldValue: '能量護盾',
  },
  ja: {
    all: 'すべて',
    category: '装備カテゴリー',
    subtype: '装備タイプ',
    weapons: '武器',
    armour: '防具',
    accessories: '装飾品',
    jewels: 'ジュエル',
    empty: '一致するベースがありません。',
    preview: 'ベース情報',
    armourValue: 'アーマー',
    evasionValue: '回避力',
    energyShieldValue: 'エナジーシールド',
  },
  es: {
    all: 'Todos',
    category: 'Categoría de equipo',
    subtype: 'Tipo de equipo',
    weapons: 'Armas',
    armour: 'Armadura',
    accessories: 'Accesorios',
    jewels: 'Joyas',
    empty: 'No hay bases coincidentes.',
    preview: 'Información de la base',
    armourValue: 'Armadura',
    evasionValue: 'Evasión',
    energyShieldValue: 'Escudo de energía',
  },

  ru: {
    all: 'Все',
    category: 'Категория предмета',
    subtype: 'Тип предмета',
    weapons: 'Оружие',
    armour: 'Броня',
    accessories: 'Аксессуары',
    jewels: 'Самоцветы',
    empty: 'Нет подходящих баз.',
    preview: 'Сведения о базе',
    armourValue: 'Броня',
    evasionValue: 'Рейтинг уклонения',
    energyShieldValue: 'Энергетический щит',
  },
  th: {
    all: 'ทั้งหมด',
    category: 'หมวดอุปกรณ์',
    subtype: 'ประเภทอุปกรณ์',
    weapons: 'อาวุธ',
    armour: 'ชุดเกราะ',
    accessories: 'อุปกรณ์เสริม',
    jewels: 'จิวเวล',
    empty: 'ไม่พบเบสที่ตรงกัน',
    preview: 'ข้อมูลเบส',
    armourValue: 'เกราะ',
    evasionValue: 'การหลบหลีก',
    energyShieldValue: 'โล่พลังงาน',
  },
}
