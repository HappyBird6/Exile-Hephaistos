import type { Locale } from '../../shared/i18n/i18n'
type Messages = {
  apply: string
  reload: string
  note: string
  stale: string
  blocked: string
}
export const textEditMessages: Record<Locale, Messages> = {
  en: {
    apply: 'Apply text edits',
    reload: 'Load current item text',
    note: 'English raw rolls. Sockets, installed augments, catalyst quality, observations and hidden state are retained separately when applying edits. Analyze item imports a new item instead. Original imported text remains in its session.',
    stale:
      'The item changed while this draft was being edited. Load current item text before applying.',
    blocked:
      'Could not safely apply these edits. The item and draft are preserved.',
  },
  ko: {
    apply: '텍스트 수정 적용',
    reload: '현재 아이템 텍스트 불러오기',
    note: '영문 원래 roll입니다. 수정 적용 시 소켓·장착물·기폭제 품질·관측값·숨겨진 상태는 별도로 보존합니다. 아이템 분석은 새 아이템을 가져옵니다. 가져온 원문은 기존 세션에 보존됩니다.',
    stale:
      '초안 편집 중 아이템이 변경됐습니다. 현재 아이템 텍스트를 불러온 뒤 적용하세요.',
    blocked: '수정을 안전하게 적용할 수 없습니다. 아이템과 초안은 보존됩니다.',
  },
  'zh-CN': {
    apply: '应用文本修改',
    reload: '载入当前物品文本',
    note: '英文原始数值。应用修改时单独保留插槽、已安装物、催化品质、观测值和隐藏状态。分析物品会导入新物品。原始文本保留在原会话中。',
    stale: '编辑草稿时物品已更改。请先载入当前文本。',
    blocked: '无法安全应用修改。物品和草稿均已保留。',
  },
  'zh-TW': {
    apply: '套用文字修改',
    reload: '載入目前物品文字',
    note: '英文原始數值。套用修改時另行保留插槽、已安裝物、催化品質、觀測值和隱藏狀態。分析物品會匯入新物品。原始文字保留於原工作階段。',
    stale: '編輯草稿時物品已變更。請先載入目前文字。',
    blocked: '無法安全套用修改。物品和草稿均已保留。',
  },
  ja: {
    apply: 'テキストの変更を適用',
    reload: '現在のアイテムを読み込む',
    note: '英語の元の値です。変更の適用ではソケット、装着物、触媒品質、観測値と非表示の状態を別途保持します。アイテムの解析は新しいアイテムを取り込みます。原文は元のセッションに保持します。',
    stale:
      '編集中にアイテムが変わりました。現在のテキストを読み込んでください。',
    blocked: '安全に適用できません。アイテムと下書きは保持されています。',
  },
  es: {
    apply: 'Aplicar cambios de texto',
    reload: 'Cargar texto actual',
    note: 'Valores originales en inglés. Al aplicar se conservan por separado engarces, aumentos instalados, calidad de catalizador, observaciones y estados ocultos. Analizar importa un objeto nuevo. El texto original permanece en su sesión.',
    stale:
      'El objeto cambió durante la edición. Carga el texto actual antes de aplicar.',
    blocked:
      'No se pueden aplicar estos cambios con seguridad. Se conservan el objeto y el borrador.',
  },
}
