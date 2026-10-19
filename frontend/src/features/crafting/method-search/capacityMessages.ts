import type { Locale } from '../../../shared/i18n/i18n'
export const capacityMessages: Record<
  Locale,
  { full: string; limited: string; after: string }
> = {
  en: {
    full: 'Saved searches are full. Your input is preserved. New searches can start after an existing search expires; stopping a calculation does not free a saved slot.',
    limited:
      'The server is limiting new requests. Your input is preserved. No automatic retry was sent.',
    after: 'Try again after',
  },
  ko: {
    full: '저장된 탐색이 가득 찼습니다. 입력은 보존했습니다. 기존 탐색의 보관 기간이 지나야 새 탐색을 시작할 수 있으며, 계산 중단만으로 저장 공간이 비워지지는 않습니다.',
    limited:
      '서버가 새 요청을 제한하고 있습니다. 입력은 보존했으며 자동으로 재요청하지 않았습니다.',
    after: '다시 시도할 수 있는 시각',
  },
  'zh-CN': {
    full: '已保存的搜索已满。输入已保留。现有搜索过期后才能开始新搜索；停止计算不会释放保存位置。',
    limited: '服务器正在限制新请求。输入已保留，未自动重试。',
    after: '可重试时间',
  },
  'zh-TW': {
    full: '已儲存的搜尋已滿。輸入已保留。現有搜尋到期後才能開始新搜尋；停止計算不會釋放儲存位置。',
    limited: '伺服器正在限制新請求。輸入已保留，未自動重試。',
    after: '可重試時間',
  },
  ja: {
    full: '保存済み探索が上限に達しました。入力は保持されています。既存の探索の保存期限後に新しい探索を開始できます。計算の停止では保存枠は空きません。',
    limited:
      'サーバーが新しいリクエストを制限しています。入力は保持され、自動再試行は行いません。',
    after: '再試行可能な時刻',
  },
  es: {
    full: 'El almacenamiento de busquedas esta lleno. Se conserva la entrada. Puedes iniciar otra cuando caduque una busqueda guardada; detener el calculo no libera espacio.',
    limited:
      'El servidor limita las solicitudes nuevas. Se conserva la entrada y no se ha reintentado automaticamente.',
    after: 'Vuelve a intentarlo despues de',
  },
}
