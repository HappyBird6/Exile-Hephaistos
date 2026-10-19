import type { Locale } from '../../../shared/i18n/i18n'
type Copy = {
  complete: string
  partial: string
  states: string
  methods: string
  repeats: string
  current: string
  pasted: string
  title: string
  export: string
  import: string
  preview: string
  open: string
  close: string
  edit: string
  warning: string
  readonly: string
  manual: string
  copied: string
  invalid: string
  busy: string
  download: string
  file: string
  start: string
  goal: string
  details: string
  editWarning: string
}
export const scenarioMessages: Record<Locale, Copy> = {
  en: {
    complete: 'Result ready',
    partial: 'Partial result ready',
    states: 'Item states',
    methods: 'Crafting methods',
    repeats: 'Attempt limit for repeatable methods only',
    current: 'Use current result code',
    pasted: 'Pasted code — automatic updates are paused',
    title: 'Scenario code',
    export: 'Copy scenario code',
    import: 'Load scenario code',
    preview: 'Validate and preview',
    open: 'Open read-only result',
    close: 'Return to my work',
    edit: 'Copy into my scenario',
    warning:
      'The code contains inputs, results and the selected route. Large results produce long codes. It is data, not executable code. A checksum detects damage, not authorship. Anyone with the code can read it; copies cannot be revoked.',
    readonly:
      'Saved result — read only. No recalculation or catalog substitution.',
    manual:
      'Select and copy the code manually if clipboard access is unavailable.',
    copied: 'Code copied.',
    invalid:
      'Cannot load this code. Check its format, version, checksum and size (2 MiB compressed / 16 MiB expanded).',
    busy: 'Processing…',
    download: 'Download code file',
    file: 'Load code file',
    start: 'Starting item',
    goal: 'Goal conditions',
    details: 'Saved versions and conditions',
    editWarning:
      'Copying replaces your current inputs. Recalculation is a separate action using the current supported catalog.',
  },
  ko: {
    complete: '결과 준비 완료',
    partial: '일부 결과 준비 완료',
    states: '아이템 상태',
    methods: '제작 방법',
    repeats: '반복 가능한 작업의 최대 시도 횟수',
    current: '현재 결과 코드로 바꾸기',
    pasted: '붙여넣은 코드 · 자동 덮어쓰기 중지',
    title: '시나리오 코드',
    export: '시나리오 코드 복사',
    import: '시나리오 코드 불러오기',
    preview: '검증 후 미리보기',
    open: '읽기 전용 결과 열기',
    close: '내 작업으로 돌아가기',
    edit: '내 시나리오로 복사',
    warning:
      '시작·목표·결과·선택 경로를 코드에 담습니다. 결과가 크면 코드도 길어집니다. 실행 코드가 아닌 데이터이며 체크섬은 손상 감지용이지 작성자 인증이 아닙니다. 코드를 가진 사람은 내용을 볼 수 있고 전달한 사본은 회수할 수 없습니다.',
    readonly:
      '저장된 결과 · 읽기 전용. 재계산하거나 최신 카탈로그로 바꾸지 않습니다.',
    manual: '클립보드를 사용할 수 없으면 아래 코드를 선택해 직접 복사하세요.',
    copied: '코드를 복사했습니다.',
    invalid:
      '코드를 불러올 수 없습니다. 형식·버전·체크섬·크기를 확인하세요. 압축 2MiB / 해제 16MiB까지 지원합니다.',
    busy: '처리 중…',
    download: '코드 파일 저장',
    file: '코드 파일 불러오기',
    start: '시작 아이템',
    goal: '목표 조건',
    details: '저장된 버전과 조건',
    editWarning:
      '복사하면 현재 입력을 바꿉니다. 재계산은 별도 동작이며 현재 지원 카탈로그를 사용합니다.',
  },
  'zh-CN': {
    complete: '结果已就绪',
    partial: '部分结果已就绪',
    states: '物品状态',
    methods: '制作方法',
    repeats: '仅可重复操作的最大尝试次数',
    current: '使用当前结果代码',
    pasted: '已粘贴代码，不自动覆盖',
    title: '场景代码',
    export: '复制场景代码',
    import: '导入场景代码',
    preview: '验证并预览',
    open: '打开只读结果',
    close: '返回我的工作',
    edit: '复制为我的场景',
    warning:
      '代码包含输入、结果和路径。结果越大代码越长。这是数据而非程序。校验和检测损坏，不验证作者。持有代码者可读取，副本无法撤回。',
    readonly: '已保存结果，只读。不重新计算或替换目录。',
    manual: '剪贴板不可用时，请手动选择并复制代码。',
    copied: '已复制代码。',
    invalid: '无法加载。请检查格式、版本、校验和及大小：压缩2MiB，解压16MiB。',
    busy: '处理中…',
    download: '保存代码文件',
    file: '导入代码文件',
    start: '起始物品',
    goal: '目标条件',
    details: '保存的版本和条件',
    editWarning: '复制会替换当前输入。重新计算是独立操作，使用当前支持的目录。',
  },
  'zh-TW': {
    complete: '結果已就緒',
    partial: '部分結果已就緒',
    states: '物品狀態',
    methods: '製作方法',
    repeats: '僅可重複操作的最大嘗試次數',
    current: '使用目前結果代碼',
    pasted: '已貼上代碼，不自動覆寫',
    title: '情境代碼',
    export: '複製情境代碼',
    import: '匯入情境代碼',
    preview: '驗證並預覽',
    open: '開啟唯讀結果',
    close: '返回我的工作',
    edit: '複製為我的情境',
    warning:
      '代碼包含輸入、結果和路徑。結果越大代碼越長。這是資料而非程式。校驗和偵測損壞，不驗證作者。持有代碼者可讀取，副本無法撤回。',
    readonly: '已儲存結果，唯讀。不重新計算或替換目錄。',
    manual: '剪貼簿無法使用時，請手動選取並複製代碼。',
    copied: '已複製代碼。',
    invalid: '無法載入。請檢查格式、版本、校驗和及大小：壓縮2MiB，解壓16MiB。',
    busy: '處理中…',
    download: '儲存代碼檔案',
    file: '匯入代碼檔案',
    start: '起始物品',
    goal: '目標條件',
    details: '儲存的版本和條件',
    editWarning: '複製會取代目前輸入。重新計算是獨立操作，使用目前支援的目錄。',
  },
  ja: {
    complete: '結果の準備完了',
    partial: '一部結果の準備完了',
    states: 'アイテム状態',
    methods: 'クラフト方法',
    repeats: '繰り返せる操作のみの最大試行回数',
    current: '現在の結果コードに切り替え',
    pasted: '貼り付けたコード・自動上書き停止',
    title: 'シナリオコード',
    export: 'シナリオコードをコピー',
    import: 'シナリオコードを読み込む',
    preview: '検証してプレビュー',
    open: '読み取り専用の結果を開く',
    close: '自分の作業に戻る',
    edit: '自分のシナリオにコピー',
    warning:
      '入力・結果・経路を含むデータです。結果が大きいほどコードも長くなります。実行コードではありません。チェックサムは破損検出用で作者認証ではありません。コードを持つ人は閲覧でき、コピーは回収できません。',
    readonly: '保存済み結果・読み取り専用。再計算やカタログ置換は行いません。',
    manual: 'クリップボードが使えない場合は手動で選択してコピーしてください。',
    copied: 'コピーしました。',
    invalid:
      '読み込めません。形式・バージョン・チェックサム・サイズを確認してください。圧縮2MiB、展開16MiBまで。',
    busy: '処理中…',
    download: 'コードファイルを保存',
    file: 'コードファイルを読む',
    start: '開始アイテム',
    goal: '目標条件',
    details: '保存されたバージョンと条件',
    editWarning:
      'コピーすると現在の入力を置き換えます。再計算は別操作で、現在対応するカタログを使用します。',
  },
  es: {
    complete: 'Resultado listo',
    partial: 'Resultado parcial listo',
    states: 'Estados del objeto',
    methods: 'Métodos de fabricación',
    repeats: 'Intentos máximos solo para métodos repetibles',
    current: 'Usar código del resultado actual',
    pasted: 'Código pegado: no se sobrescribe automáticamente',
    title: 'Código de escenario',
    export: 'Copiar código de escenario',
    import: 'Cargar código de escenario',
    preview: 'Validar y previsualizar',
    open: 'Abrir resultado de solo lectura',
    close: 'Volver a mi trabajo',
    edit: 'Copiar a mi escenario',
    warning:
      'El código contiene entradas, resultados y ruta. Los resultados grandes producen códigos largos. Son datos, no un programa. La suma detecta daños, no autentica al autor. Quien tenga el código podrá leerlo y las copias no se pueden revocar.',
    readonly:
      'Resultado guardado de solo lectura. No se recalcula ni sustituye el catálogo.',
    manual:
      'Si no funciona el portapapeles, selecciona y copia el código manualmente.',
    copied: 'Código copiado.',
    invalid:
      'No se puede cargar. Comprueba formato, versión, suma y tamaño: 2MiB comprimidos, 16MiB expandidos.',
    busy: 'Procesando…',
    download: 'Guardar archivo de código',
    file: 'Cargar archivo de código',
    start: 'Objeto inicial',
    goal: 'Condiciones del objetivo',
    details: 'Versiones y condiciones guardadas',
    editWarning:
      'Copiar sustituye tus entradas actuales. Recalcular es una acción aparte que usa el catálogo disponible actualmente.',
  },
}
