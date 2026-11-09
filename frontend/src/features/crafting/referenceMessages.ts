import type { Locale } from '../../shared/i18n/i18n'
import type { ExclusionReason } from './workbenchReferenceApi'
type Messages = {
  invalidItem: string
  unknownModifier: string
  invalidRoll: string
  unsupportedBase: string
  checking: string
  failed: string
  blocked: string
  same: string
  changed: string
  continue: string
  retry: string
  excluded: string
  reasons: Record<ExclusionReason, string>
}
export const referenceMessages: Record<Locale, Messages> = {
  en: {
    invalidItem:
      'The saved item contains unsupported or inconsistent properties.',
    unknownModifier: 'A saved modifier is not present in the current catalog.',
    invalidRoll:
      'Saved modifier values or stat sets do not match the current catalog.',
    unsupportedBase: 'This base item is not supported by the current catalog.',

    checking: 'Checking this checkpoint against current rules…',
    failed: 'The checkpoint could not be checked.',
    blocked:
      'This item cannot continue under current rules. The original session is preserved.',
    same: 'The rules for this item are unchanged. Continue in a new session to preserve the original record.',
    changed:
      'This item is valid under current rules. Future crafts use current rules; the original session is preserved.',
    continue: 'Continue in a new session',
    retry: 'Retry',
    excluded: 'Unavailable',
    reasons: {
      ITEM_LEVEL: 'Item level is too low',
      FAMILY_OCCUPIED: 'A modifier from this family is already present',
      AFFIX_FULL: 'No free affix slot',
      REVEAL_POOL: 'Outside the current reveal pool or level limit',
      STATE_RESTRICTION: 'Unavailable for the current item state',
    },
  },
  ko: {
    invalidItem:
      '저장된 아이템 속성 중 지원되지 않거나 서로 맞지 않는 정보가 있습니다.',
    unknownModifier: '현재 카탈로그에 없는 옵션이 포함되어 있습니다.',
    invalidRoll:
      '저장된 옵션 수치 또는 스탯 구성이 현재 카탈로그와 맞지 않습니다.',
    unsupportedBase: '현재 카탈로그에서 지원하지 않는 베이스입니다.',

    checking: '현재 규칙으로 아이템을 검증하고 있습니다…',
    failed: '아이템을 검증하지 못했습니다.',
    blocked:
      '현재 규칙으로 이어갈 수 없는 아이템입니다. 원본 세션은 보존됩니다.',
    same: '이 장비의 규칙은 동일합니다. 원본 기록을 보존하고 새 세션에서 이어갈 수 있습니다.',
    changed:
      '현재 규칙으로 검증된 아이템입니다. 이후 제작에는 현재 규칙이 적용되며 원본 세션은 보존됩니다.',
    continue: '새 세션으로 이어하기',
    retry: '다시 시도',
    excluded: '추가 불가',
    reasons: {
      ITEM_LEVEL: '아이템 레벨 부족',
      FAMILY_OCCUPIED: '같은 계열의 속성이 이미 존재함',
      AFFIX_FULL: '남은 속성 슬롯 없음',
      REVEAL_POOL: '현재 드러내기 후보군 또는 레벨 제한에 맞지 않음',
      STATE_RESTRICTION: '현재 아이템 상태에서 추가할 수 없음',
    },
  },
  'zh-CN': {
    invalidItem: '保存的物品包含不支持或不一致的属性。',
    unknownModifier: '保存的词缀不在当前目录中。',
    invalidRoll: '保存的词缀数值或属性组合与当前目录不符。',
    unsupportedBase: '当前目录不支持此基底。',

    checking: '正在按当前规则验证物品…',
    failed: '无法验证物品。',
    blocked: '此物品无法按当前规则继续。原会话已保留。',
    same: '此物品的规则未变。可在新会话中继续，保留原始记录。',
    changed: '物品已通过当前规则验证。后续制作使用当前规则，原会话保留。',
    continue: '在新会话中继续',
    retry: '重试',
    excluded: '不可添加',
    reasons: {
      ITEM_LEVEL: '物品等级不足',
      FAMILY_OCCUPIED: '已有同组词缀',
      AFFIX_FULL: '没有空余词缀位',
      REVEAL_POOL: '不在当前揭示候选池或等级范围内',
      STATE_RESTRICTION: '当前物品状态下不可用',
    },
  },
  'zh-TW': {
    invalidItem: '儲存的物品包含不支援或不一致的屬性。',
    unknownModifier: '儲存的詞綴不在目前目錄中。',
    invalidRoll: '儲存的詞綴數值或屬性組合與目前目錄不符。',
    unsupportedBase: '目前目錄不支援此基底。',

    checking: '正在依目前規則驗證物品…',
    failed: '無法驗證物品。',
    blocked: '此物品無法依目前規則繼續。原工作階段已保留。',
    same: '此物品的規則未變。可在新工作階段繼續並保留原始記錄。',
    changed: '物品已通過目前規則驗證。後續製作使用目前規則，原工作階段保留。',
    continue: '在新工作階段繼續',
    retry: '重試',
    excluded: '無法新增',
    reasons: {
      ITEM_LEVEL: '物品等級不足',
      FAMILY_OCCUPIED: '已有同組詞綴',
      AFFIX_FULL: '沒有空餘詞綴欄位',
      REVEAL_POOL: '不在目前揭露候選池或等級範圍內',
      STATE_RESTRICTION: '目前物品狀態下無法使用',
    },
  },
  ja: {
    invalidItem:
      '保存されたアイテムに未対応または不整合なプロパティがあります。',
    unknownModifier: '現在のカタログにないモッドが含まれています。',
    invalidRoll:
      '保存された数値またはステータス構成が現在のカタログと一致しません。',
    unsupportedBase: '現在のカタログではこのベースに対応していません。',

    checking: '現在のルールでアイテムを検証中…',
    failed: 'アイテムを検証できませんでした。',
    blocked: '現在のルールでは続行できません。元のセッションは保持されます。',
    same: 'この装備のルールは変わっていません。元の記録を保持し、新しいセッションで続行できます。',
    changed:
      '現在のルールで検証済みです。以後の製作には現在のルールが適用され、元のセッションは保持されます。',
    continue: '新しいセッションで続行',
    retry: '再試行',
    excluded: '追加不可',
    reasons: {
      ITEM_LEVEL: 'アイテムレベル不足',
      FAMILY_OCCUPIED: '同じグループのモッドが存在',
      AFFIX_FULL: '空きモッド枠なし',
      REVEAL_POOL: '現在のリビール候補またはレベル制限の対象外',
      STATE_RESTRICTION: '現在のアイテム状態では利用不可',
    },
  },
  es: {
    invalidItem:
      'El objeto guardado contiene propiedades no admitidas o incoherentes.',
    unknownModifier: 'Un modificador guardado no está en el catálogo actual.',
    invalidRoll:
      'Los valores o estadísticas guardados no coinciden con el catálogo actual.',
    unsupportedBase: 'El catálogo actual no admite esta base.',

    checking: 'Comprobando el objeto con las reglas actuales…',
    failed: 'No se pudo comprobar el objeto.',
    blocked:
      'Este objeto no puede continuar con las reglas actuales. Se conserva la sesión original.',
    same: 'Las reglas de este objeto no han cambiado. Continúa en una sesión nueva para conservar el registro original.',
    changed:
      'Objeto validado con las reglas actuales. Las próximas acciones usarán estas reglas; se conserva la sesión original.',
    continue: 'Continuar en una sesión nueva',
    retry: 'Reintentar',
    excluded: 'No disponible',
    reasons: {
      ITEM_LEVEL: 'Nivel de objeto insuficiente',
      FAMILY_OCCUPIED: 'Ya existe un modificador de esta familia',
      AFFIX_FULL: 'No quedan espacios de afijos',
      REVEAL_POOL: 'Fuera del grupo de revelación o del límite de nivel actual',
      STATE_RESTRICTION: 'No disponible en el estado actual del objeto',
    },
  },
}
