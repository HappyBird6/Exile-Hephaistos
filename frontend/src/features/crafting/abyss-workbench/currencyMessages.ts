const messages = {
  en: [
    'This currency cannot be used on the current item. The item and active omens are unchanged.',
    'Complete the reveal choice before using this currency.',
    'This item cannot be modified in its current condition.',
    'The unrevealed modifier level is unknown. Choose a currency or omen that does not require it.',
  ],
  ko: [
    '이 화폐는 현재 아이템에 적용할 수 없습니다. 아이템과 활성 징조는 유지됩니다.',
    '이 화폐를 사용하기 전에 드러내기 후보 선택을 완료하세요.',
    '현재 상태의 아이템은 더 변경할 수 없습니다.',
    '미공개 옵션의 레벨을 알 수 없습니다. 해당 레벨이 필요하지 않은 화폐나 징조를 선택하세요.',
  ],
  'zh-CN': [
    '此通货无法用于当前物品。物品和已启用的预兆保持不变。',
    '使用此通货前，请先完成揭示词缀的选择。',
    '当前状态的物品无法继续修改。',
    '未揭示词缀的等级未知。请选择不需要该等级的通货或预兆。',
  ],
  'zh-TW': [
    '此通貨無法用於目前物品。物品和已啟用的預兆保持不變。',
    '使用此通貨前，請先完成揭示詞綴的選擇。',
    '目前狀態的物品無法繼續修改。',
    '未揭示詞綴的等級未知。請選擇不需要該等級的通貨或預兆。',
  ],
  ja: [
    'この通貨は現在のアイテムには使用できません。アイテムと有効なオーメンは保持されます。',
    'この通貨を使用する前に開示するモッドを選択してください。',
    '現在の状態ではアイテムを変更できません。',
    '未公開モッドのレベルは不明です。そのレベルを必要としない通貨やオーメンを選択してください。',
  ],
  es: [
    'Esta moneda no se puede usar en el objeto actual. El objeto y los presagios activos no cambian.',
    'Completa la elección del modificador revelado antes de usar esta moneda.',
    'El objeto no se puede modificar en su estado actual.',
    'Se desconoce el nivel del modificador sin revelar. Elige una moneda o un presagio que no necesite ese nivel.',
  ],
} as const

export function abyssCurrencyReason(
  reason: string,
  locale: keyof typeof messages,
): string {
  const index =
    reason === 'ITEM_CONDITION'
      ? 2
      : reason.startsWith('RESOLVE_OFFER')
        ? 1
        : reason === 'UNREVEALED_MODIFIER_LEVEL_UNKNOWN'
          ? 3
          : 0
  return messages[locale][index]
}
