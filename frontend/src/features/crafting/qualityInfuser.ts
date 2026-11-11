import type { AppliedItem, ConcreteItem, WorkbenchAction } from './workbenchApi'
import type { Definition } from './craftingApi'
import { maximumQuality } from './qualityLimit'
import { sameItemState } from './crafting-state/itemIdentity'
export const qualityInfuserPolicy = 'quality-infuser-once-plus-ten-v1'
export const isQualityInfuser = (action: string) =>
  /^VAAL_(ARMOURERS|BLACKSMITHS|ARCANISTS|CATALYSING)_INFUSER$/.test(action)
export function infuserSupportsBase(id: string, base: string) {
  const caster = /\/(Wands|Staves|Sceptres)\//.test(base)
  if (id === 'Vaal_Armourers_Infuser')
    return base.startsWith('Metadata/Items/Armours/')
  if (id === 'Vaal_Blacksmiths_Infuser')
    return base.startsWith('Metadata/Items/Weapons/') && !caster
  if (id === 'Vaal_Arcanists_Infuser') return caster
  if (id === 'Vaal_Catalysing_Infuser')
    return /^Metadata\/Items\/(Rings|Amulets)\//.test(base)
  return false
}
export function infuserTransitionMatches(
  before: ConcreteItem,
  result: AppliedItem,
  action: WorkbenchAction,
  definitions: Record<string, Definition>,
) {
  if (!isQualityInfuser(action) || !result.applied)
    return (
      Boolean(before.qualityInfused) === Boolean(result.state.qualityInfused)
    )
  const jewellery = action === 'VAAL_CATALYSING_INFUSER'
  const current = jewellery
    ? before.catalystQuality?.amount
    : before.equipmentQuality
  const cap = maximumQuality(before, definitions)
  if (
    before.qualityInfused ||
    before.conditions.length ||
    current == null ||
    cap == null ||
    current >= cap + 10
  )
    return false
  const amount = Math.min(current + 10, cap + 10)
  const expected = {
    ...before,
    qualityInfused: true,
    ...(jewellery
      ? { catalystQuality: { ...before.catalystQuality!, amount } }
      : { equipmentQuality: amount }),
  }
  return (
    sameItemState(expected, result.state) &&
    result.events.length === 0 &&
    result.consumedOmens.length === 0 &&
    result.assumptions.length === 1 &&
    result.assumptions[0]?.id === qualityInfuserPolicy &&
    result.assumptions[0]?.min === amount &&
    result.assumptions[0]?.max === amount
  )
}
export const infuserNotes = {
  en: 'Simulator: no corruption; once per item, +10% quality (up to maximum quality +10%).',
  ko: '사이트 규칙: 타락 없이 아이템당 1회, 품질 +10%p (최대 품질 +10%p까지).',
  'zh-CN':
    '本站规则：不腐化，每件物品限一次，品质 +10%（上限为最大品质 +10%）。',
  'zh-TW':
    '本站規則：不汙染，每件物品限一次，品質 +10%（上限為最大品質 +10%）。',
  ja: 'サイト仕様：コラプトなし。アイテムごとに1回、品質+10%（最大品質+10%まで）。',
  es: 'Simulador: sin corrupción; una vez por objeto, +10% de calidad (hasta el máximo +10%).',
  ru: 'Правило сайта: без осквернения; один раз на предмет, +10% качества (не выше максимума +10%).',
  th: 'กฎเว็บไซต์: ไม่ทำให้มีมลทิน ใช้ได้ครั้งเดียวต่อไอเทม เพิ่มคุณภาพ 10% (ไม่เกินคุณภาพสูงสุด +10%)',
}
