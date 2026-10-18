export const recoveryMessages = {
  en: {
    pending: 'Checking the saved goal against the current catalog…',
    blocked: 'Your goal is preserved. Review these entries before searching:',
    RECOVERY_CONTEXT: 'The item base does not match.',
    RECOVERY_OPERATION: 'This group operation is no longer supported.',
    RECOVERY_MISSING: 'This option is absent from the current catalog.',
    RECOVERY_MEANING:
      'The unit or option definition changed or is unsupported.',
    RECOVERY_UNVERIFIED:
      'The previous option definition is unavailable; its meaning cannot be verified.',
    invalid:
      'The current catalog rejected this condition. Review its values and operation.',
  },
  ko: {
    pending: '저장된 목표를 현재 Catalog와 대조하고 있습니다…',
    blocked: '목표는 보존되었습니다. 탐색 전에 다음 항목을 확인해 주세요:',
    RECOVERY_CONTEXT: '아이템 베이스가 일치하지 않습니다.',
    RECOVERY_OPERATION: '현재 지원하지 않는 그룹 연산입니다.',
    RECOVERY_MISSING: '현재 Catalog에 없는 옵션입니다.',
    RECOVERY_MEANING: '단위나 옵션 정의가 변경되었거나 지원하지 않습니다.',
    RECOVERY_UNVERIFIED:
      '이전 옵션 정의가 없어 의미가 동일한지 확인할 수 없습니다.',
    invalid:
      '현재 Catalog에서 조건을 검증하지 못했습니다. 수치와 연산을 확인해 주세요.',
  },
  'zh-CN': {
    pending: '正在对照当前目录检查保存的目标…',
    blocked: '目标已保留。搜索前请检查以下项目：',
    RECOVERY_CONTEXT: '物品基底不匹配。',
    RECOVERY_OPERATION: '不再支持此分组运算。',
    RECOVERY_MISSING: '当前目录中没有此词缀。',
    RECOVERY_MEANING: '单位或词缀定义已改变或不受支持。',
    RECOVERY_UNVERIFIED: '缺少旧词缀定义，无法验证含义。',
    invalid: '当前目录无法验证此条件。请检查数值和运算。',
  },
  'zh-TW': {
    pending: '正在對照目前目錄檢查儲存的目標…',
    blocked: '目標已保留。搜尋前請檢查以下項目：',
    RECOVERY_CONTEXT: '物品基底不符。',
    RECOVERY_OPERATION: '不再支援此群組運算。',
    RECOVERY_MISSING: '目前目錄中沒有此詞綴。',
    RECOVERY_MEANING: '單位或詞綴定義已變更或不受支援。',
    RECOVERY_UNVERIFIED: '缺少舊詞綴定義，無法驗證含義。',
    invalid: '目前目錄無法驗證此條件。請檢查數值和運算。',
  },
  ja: {
    pending: '保存した目標を現在のカタログと照合しています…',
    blocked: '目標は保持されています。探索前に次の項目を確認してください：',
    RECOVERY_CONTEXT: 'アイテムベースが一致しません。',
    RECOVERY_OPERATION: 'このグループ演算は対応していません。',
    RECOVERY_MISSING: '現在のカタログにないオプションです。',
    RECOVERY_MEANING: '単位や定義が変更されたか、未対応です。',
    RECOVERY_UNVERIFIED: '以前の定義がないため意味を確認できません。',
    invalid: '条件を検証できません。数値と演算を確認してください。',
  },
  es: {
    pending: 'Comprobando el objetivo guardado con el catálogo actual…',
    blocked: 'El objetivo se conserva. Revisa estas entradas antes de buscar:',
    RECOVERY_CONTEXT: 'La base del objeto no coincide.',
    RECOVERY_OPERATION: 'Esta operación de grupo ya no es compatible.',
    RECOVERY_MISSING: 'Esta opción no existe en el catálogo actual.',
    RECOVERY_MEANING: 'La unidad o definición cambió o no es compatible.',
    RECOVERY_UNVERIFIED:
      'Falta la definición anterior; no se puede verificar su significado.',
    invalid:
      'No se pudo validar la condición. Revisa los valores y la operación.',
  },
} as const
