import type { Locale } from '../../../shared/i18n/i18n'

export const preparationConditions: Record<Locale, string> = {
  en: 'Prepare a higher modifier level so Whittling can protect existing goals. If this removal prerequisite fails, stop before using the follower; recovery and restart costs are separate.',
  ko: '먼저 높은 modifier level 옵션을 준비해 절사의 징조가 기존 목표를 보존하도록 계획합니다. 제거 우선순위 준비에 실패하면 다음 화폐 사용 전에 중단하며, 복구·재시작 비용은 별도입니다.',
  'zh-CN':
    '先准备更高词缀等级以保护现有目标。未满足移除优先级时，在使用后续货币前停止；恢复和重启成本另计。',
  'zh-TW':
    '先準備更高詞綴等級以保護現有目標。未滿足移除優先級時，在使用後續通貨前停止；恢復與重啟成本另計。',
  ja: '高い修飾子レベルを先に準備して既存目標を保護します。除去の前提を満たさない場合は次の通貨を使わず終了します。復旧と再開始費用は別です。',
  es: 'Prepara un nivel de afijo mas alto para proteger los objetivos existentes. Si falla la prioridad de eliminacion, se detiene antes de la siguiente moneda; recuperar o reiniciar cuesta aparte.',
}

export const acquisitionHints: Record<Locale, string> = {
  en: 'Planned goal acquisition or a temporary modifier needed for later removal only.',
  ko: '목표 옵션 획득 또는 이후 제거에 필요한 임시 옵션 준비에만 사용합니다.',
  'zh-CN': '仅用于计划中的目标词缀获取或后续移除所需的临时词缀。',
  'zh-TW': '僅用於計畫中的目標詞綴取得或後續移除所需的暫時詞綴。',
  ja: '目標修飾子の獲得、または後で除去する仮の修飾子の準備にのみ使用します。',
  es: 'Solo para obtener un afijo objetivo o preparar un afijo temporal para eliminarlo.',
}

export const acquisitionLabels: Record<Locale, string> = {
  en: 'goal acquisition attempt',
  ko: '목표 옵션 획득 시도',
  'zh-CN': '目标词缀获取尝试',
  'zh-TW': '目標詞綴取得嘗試',
  ja: '目標修飾子の獲得試行',
  es: 'intento de obtener el objetivo',
}
