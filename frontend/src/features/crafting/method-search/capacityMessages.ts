import type { Locale } from '../../../shared/i18n/i18n'
export const capacityMessages: Record<
  Locale,
  { full: string; storage: string; limited: string; after: string }
> = {
  en: {
    full: 'All calculation slots are in use. Your input is preserved. Try again when another calculation finishes or stops. Saved results do not occupy calculation slots.',
    storage:
      'Saved search storage is full. Your input and existing results are preserved. Try again after saved searches expire.',
    limited:
      'The server is limiting new requests. Your input is preserved. No automatic retry was sent.',
    after: 'Try again after',
  },
  ko: {
    full: '계산 슬롯을 모두 사용 중입니다. 입력은 보존했습니다. 다른 계산이 완료되거나 중단되면 다시 시도하세요. 저장된 결과는 계산 슬롯을 차지하지 않습니다.',
    storage:
      '검색 결과 저장 공간이 가득 찼습니다. 입력과 기존 결과는 보존했습니다. 저장 기간이 만료된 뒤 다시 시도하세요.',
    limited:
      '서버가 새 요청을 제한하고 있습니다. 입력은 보존했으며 자동으로 재요청하지 않았습니다.',
    after: '다시 시도할 수 있는 시간',
  },
  'zh-CN': {
    full: '计算名额已满。输入已保留。请在其他计算完成或停止后重试。保存的结果不占用计算名额。',
    storage:
      '搜索结果存储空间已满。输入和已有结果均已保留。请在保存期限到期后重试。',
    limited: '服务器正在限制新请求。输入已保留，未自动重试。',
    after: '请在此时间后重试',
  },
  'zh-TW': {
    full: '計算名額已滿。輸入已保留。請在其他計算完成或停止後重試。儲存的結果不占用計算名額。',
    storage:
      '搜尋結果儲存空間已滿。輸入和既有結果均已保留。請在儲存期限到期後重試。',
    limited: '伺服器正在限制新請求。輸入已保留，未自動重試。',
    after: '請在此時間後重試',
  },
  ja: {
    full: '計算枠をすべて使用中です。入力は保持されています。他の計算が完了または停止したら再試行してください。保存済みの結果は計算枠を使用しません。',
    storage:
      '検索結果の保存領域がいっぱいです。入力と既存の結果は保持されています。保存期間が切れた後に再試行してください。',
    limited:
      'サーバーが新しいリクエストを制限しています。入力は保持され、自動再試行は行っていません。',
    after: '再試行できる時刻',
  },
  es: {
    full: 'Todas las plazas de cálculo están ocupadas. Se conserva la entrada. Reintenta cuando otro cálculo termine o se detenga. Los resultados guardados no ocupan plazas de cálculo.',
    storage:
      'El almacenamiento de resultados está lleno. Se conservan la entrada y los resultados existentes. Reintenta cuando caduquen las búsquedas guardadas.',
    limited:
      'El servidor limita las solicitudes nuevas. Se conserva la entrada y no se ha reintentado automáticamente.',
    after: 'Vuelve a intentarlo después de',
  },
}
