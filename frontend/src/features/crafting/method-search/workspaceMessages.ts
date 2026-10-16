import type { Locale } from '../../../shared/i18n/i18n'
const en = {
  route: 'Selected candidate route',
  hint: 'Choose arrows from state 0 to build a route. This is a calculated candidate, not currency you have spent.',
  incoming: 'Incoming method',
  choose: 'Choose a method',
  reset: 'Clear route',
  unknown:
    'The API provides initial actions and an ordered repeat cycle, not the realized number of repetitions or the exact action at which this outcome occurred. The final cycle may stop early. Omens are listed with their actions; quality work is a separate step.',
  disconnected:
    'This node has no selected route from state 0. Choose incoming methods or follow arrows from state 0.',
  implicit:
    'Existing implicit target conditions are preserved but cannot be used for a new search. Remove those rows explicitly before starting.',
  storage:
    'Local saving is unavailable or full. You can continue in this tab; changes may not survive a reload.',
  expired:
    'The saved search is unavailable or its rules changed. Your input is preserved; choose Recalculate to start a new search.',
}
type Copy = typeof en
export const workspaceMessages: Record<Locale, Copy> = {
  en,
  ko: {
    route: '선택한 후보 경로',
    hint: '상태 0부터 화살표를 선택해 경로를 만드세요. 계산된 후보이며 실제로 소비한 화폐 기록이 아닙니다.',
    incoming: '진입 제작 방법',
    choose: '방법 선택',
    reset: '경로 비우기',
    unknown:
      '응답에는 초기 작업과 반복 cycle의 순서가 있습니다. 실제 반복 횟수와 이 결과가 발생한 정확한 행동 시점은 없습니다. 마지막 cycle은 중간에 종료될 수 있습니다. 징조는 해당 행동에 함께 표시하며 품질 작업은 별도 단계입니다.',
    disconnected:
      '이 노드까지 상태 0에서 이어지는 경로를 선택하지 않았습니다. 진입 방법을 선택하거나 상태 0부터 화살표를 따라가세요.',
    implicit:
      '기존 고정 속성 목표는 보존했지만 새 탐색에는 사용할 수 없습니다. 해당 조건 행을 직접 삭제한 뒤 시작하세요.',
    storage:
      '로컬 저장 공간이 부족하거나 사용할 수 없습니다. 현재 탭에서는 계속할 수 있지만 새로고침 후 변경이 유지되지 않을 수 있습니다.',
    expired:
      '저장된 탐색을 불러올 수 없거나 규칙이 변경되었습니다. 입력은 유지했습니다. 다시 계산을 선택해 새 탐색을 시작하세요.',
  },
  'zh-CN': {
    route: '所选候选路径',
    hint: '从状态0选择箭头组成路径。这是计算候选，不是实际花费记录。',
    incoming: '进入方法',
    choose: '选择方法',
    reset: '清空路径',
    unknown:
      '仅提供初始动作和循环顺序；不提供实际重复次数或结果发生的具体动作。最后一轮可能提前结束。预兆随动作列出，品质作业单独列出。',
    disconnected:
      '尚未选择从状态0到此节点的路径。请选择进入方法或从状态0跟随箭头。',
    implicit:
      '保留了旧的固定词缀目标，但新搜索不能使用它们。请手动删除这些条件。',
    storage: '本地存储不可用或已满；刷新后可能无法恢复修改。',
    expired: '保存的搜索不可用或规则已变更。输入已保留，请重新计算。',
  },
  'zh-TW': {
    route: '所選候選路徑',
    hint: '從狀態0選擇箭頭組成路徑。這是計算候選，不是實際花費紀錄。',
    incoming: '進入方法',
    choose: '選擇方法',
    reset: '清空路徑',
    unknown:
      '僅提供初始動作和循環順序；不提供實際重複次數或結果發生的具體動作。最後一輪可能提前結束。預兆隨動作列出，品質作業單獨列出。',
    disconnected:
      '尚未選擇從狀態0到此節點的路徑。請選擇進入方法或從狀態0跟隨箭頭。',
    implicit:
      '保留了舊的固定詞綴目標，但新搜尋不能使用它們。請手動刪除這些條件。',
    storage: '本機儲存不可用或已滿；重新整理後可能無法還原修改。',
    expired: '儲存的搜尋不可用或規則已變更。輸入已保留，請重新計算。',
  },
  ja: {
    route: '選択した候補経路',
    hint: '状態0から矢印を選んで経路を作成します。計算上の候補で、実際の消費履歴ではありません。',
    incoming: '流入する方法',
    choose: '方法を選択',
    reset: '経路を消去',
    unknown:
      '初期操作と反復順序のみ提供されます。実際の反復回数や結果発生時の操作は不明です。最終周期は途中で終了する場合があります。予兆は操作と併記し、品質作業は別の段階です。',
    disconnected:
      '状態0からこのノードへの経路が未選択です。流入方法を選ぶか、状態0から矢印をたどってください。',
    implicit:
      '既存の固定特性の目標は保持しましたが、新しい探索には使えません。該当条件を明示的に削除してください。',
    storage:
      'ローカル保存が利用不可または満杯です。再読み込みで変更が失われる場合があります。',
    expired:
      '保存した探索が利用できないかルールが変更されました。入力は保持されています。再計算してください。',
  },
  es: {
    route: 'Ruta candidata seleccionada',
    hint: 'Elige flechas desde el estado 0. Es una ruta calculada, no un historial de moneda gastada.',
    incoming: 'Método de entrada',
    choose: 'Elegir método',
    reset: 'Borrar ruta',
    unknown:
      'Se muestran acciones iniciales y el orden del ciclo, no las repeticiones reales ni la acción exacta del resultado. El último ciclo puede terminar antes. Los presagios acompañan sus acciones y la calidad es un paso separado.',
    disconnected:
      'No has elegido una ruta desde el estado 0. Elige métodos de entrada o sigue las flechas desde el estado 0.',
    implicit:
      'Se conservaron los objetivos implícitos anteriores, pero no se pueden usar en una búsqueda nueva. Elimina esas condiciones explícitamente.',
    storage:
      'El almacenamiento local está lleno o no está disponible. Los cambios pueden perderse al recargar.',
    expired:
      'La búsqueda guardada no está disponible o cambiaron las reglas. Se conservó la entrada; vuelve a calcular.',
  },
}
