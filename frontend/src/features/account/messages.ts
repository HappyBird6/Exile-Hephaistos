import type { Locale } from '../../shared/i18n/i18n'
type Messages = {
  login: string
  register: string
  logout: string
  username: string
  password: string
  close: string
  hint: string
  error: string
  unavailable: string
  merge: string
  add: string
  skip: string
  sync: string
  retry: string
  conflict: string
}
export const accountMessages: Record<Locale, Messages> = {
  en: {
    login: 'Log in',
    register: 'Sign up',
    logout: 'Log out',
    username: 'Username',
    password: 'Password',
    close: 'Close',
    hint: 'Username: 3–32 letters, numbers or _. Password: at least 8 characters, up to 72 UTF-8 bytes.',
    error:
      'Could not complete the request. Check your details or try again shortly.',
    unavailable: 'Account connection unavailable',
    merge:
      'Add this browser’s sessions and favorites to your account? These records may belong to someone else.',
    add: 'Add to my account',
    skip: 'Skip',
    sync: 'New saved records are available. Reload to use them.',
    retry: 'Retry',
    conflict:
      'Records changed elsewhere. Your local changes are preserved. Resolve the conflict before syncing.',
  },
  ko: {
    login: '로그인',
    register: '회원가입',
    logout: '로그아웃',
    username: '아이디',
    password: '비밀번호',
    close: '닫기',
    hint: '아이디: 영문·숫자·밑줄 3~32자. 비밀번호: 8자 이상, UTF-8 기준 72바이트 이하.',
    error:
      '요청을 완료하지 못했습니다. 입력 내용을 확인하거나 잠시 후 다시 시도해 주세요.',
    unavailable: '계정 연결 불가',
    merge:
      '이 브라우저의 세션과 즐겨찾기를 계정에 추가할까요? 다른 사람의 기록일 수 있습니다.',
    add: '계정에 추가',
    skip: '건너뛰기',
    sync: '새로운 저장 기록을 받았습니다. 새로고침하면 반영됩니다.',
    retry: '다시 시도',
    conflict:
      '다른 곳에서 기록이 변경됐습니다. 현재 변경은 보존되며 충돌 해결 후 동기화할 수 있습니다.',
  },
  'zh-CN': {
    login: '登录',
    register: '注册',
    logout: '退出登录',
    username: '用户名',
    password: '密码',
    close: '关闭',
    hint: '用户名：3–32 个字母、数字或下划线。密码：至少 8 个字符，UTF-8 不超过 72 字节。',
    error: '无法完成请求。请检查输入或稍后重试。',
    unavailable: '无法连接账户',
    merge: '将此浏览器的会话和收藏添加到账户？这些记录可能属于他人。',
    add: '添加到账户',
    skip: '跳过',
    sync: '已收到新记录，请刷新页面。',
    retry: '重试',
    conflict: '记录已在其他位置更改。本地更改已保留，请解决冲突后同步。',
  },
  'zh-TW': {
    login: '登入',
    register: '註冊',
    logout: '登出',
    username: '使用者名稱',
    password: '密碼',
    close: '關閉',
    hint: '名稱：3–32 個英文字母、數字或底線。密碼：至少 8 個字元，UTF-8 不超過 72 位元組。',
    error: '無法完成要求。請檢查輸入或稍後再試。',
    unavailable: '無法連線帳戶',
    merge: '將此瀏覽器的工作階段與收藏加入帳戶？這些紀錄可能屬於他人。',
    add: '加入帳戶',
    skip: '略過',
    sync: '已收到新紀錄，請重新整理頁面。',
    retry: '重試',
    conflict: '紀錄已在其他位置變更。本機變更已保留，請解決衝突後同步。',
  },
  ja: {
    login: 'ログイン',
    register: '登録',
    logout: 'ログアウト',
    username: 'ユーザー名',
    password: 'パスワード',
    close: '閉じる',
    hint: '名前：英数字・下線3～32文字。パスワード：8文字以上、UTF-8で72バイト以内。',
    error:
      '処理できませんでした。入力を確認するか、時間をおいて再試行してください。',
    unavailable: 'アカウントに接続できません',
    merge:
      'このブラウザーのセッションとお気に入りをアカウントに追加しますか？他の人の記録かもしれません。',
    add: 'アカウントに追加',
    skip: 'スキップ',
    sync: '新しい記録を受信しました。再読み込みしてください。',
    retry: '再試行',
    conflict:
      '別の場所で記録が変更されました。ローカルの変更は保持されています。競合を解決してください。',
  },
  es: {
    login: 'Iniciar sesión',
    register: 'Registrarse',
    logout: 'Cerrar sesión',
    username: 'Usuario',
    password: 'Contraseña',
    close: 'Cerrar',
    hint: 'Usuario: 3–32 letras, números o _. Contraseña: mínimo 8 caracteres, máximo 72 bytes UTF-8.',
    error: 'No se pudo completar. Revisa los datos o inténtalo más tarde.',
    unavailable: 'Cuenta sin conexión',
    merge:
      '¿Añadir las sesiones y favoritos de este navegador a tu cuenta? Podrían pertenecer a otra persona.',
    add: 'Añadir a mi cuenta',
    skip: 'Omitir',
    sync: 'Hay nuevos registros. Recarga para verlos.',
    retry: 'Reintentar',
    conflict:
      'Los registros cambiaron en otro lugar. Se conservan tus cambios locales. Resuelve el conflicto para sincronizar.',
  },
  ru: {
    login: 'Войти',
    register: 'Регистрация',
    logout: 'Выйти',
    username: 'Имя пользователя',
    password: 'Пароль',
    close: 'Закрыть',
    hint: 'Имя: 3–32 латинские буквы, цифры или _. Пароль: от 8 символов, до 72 байт UTF-8.',
    error: 'Не удалось выполнить запрос. Проверьте данные или повторите позже.',
    unavailable: 'Нет связи с аккаунтом',
    merge:
      'Добавить сессии и избранное браузера в аккаунт? Эти записи могут принадлежать другому человеку.',
    add: 'Добавить в аккаунт',
    skip: 'Пропустить',
    sync: 'Получены новые записи. Перезагрузите страницу.',
    retry: 'Повторить',
    conflict:
      'Записи изменены в другом месте. Локальные изменения сохранены. Разрешите конфликт перед синхронизацией.',
  },
  th: {
    login: 'เข้าสู่ระบบ',
    register: 'สมัครสมาชิก',
    logout: 'ออกจากระบบ',
    username: 'ชื่อผู้ใช้',
    password: 'รหัสผ่าน',
    close: 'ปิด',
    hint: 'ชื่อ: อักษรอังกฤษ ตัวเลข หรือ _ 3–32 ตัว รหัสผ่านอย่างน้อย 8 ตัว ไม่เกิน 72 ไบต์ UTF-8',
    error: 'ดำเนินการไม่สำเร็จ โปรดตรวจสอบข้อมูลหรือลองใหม่ภายหลัง',
    unavailable: 'เชื่อมต่อบัญชีไม่ได้',
    merge:
      'เพิ่มเซสชันและรายการโปรดในเบราว์เซอร์นี้ไปยังบัญชีหรือไม่? ข้อมูลอาจเป็นของผู้อื่น',
    add: 'เพิ่มในบัญชี',
    skip: 'ข้าม',
    sync: 'ได้รับบันทึกใหม่แล้ว โปรดโหลดหน้าใหม่',
    retry: 'ลองใหม่',
    conflict:
      'ข้อมูลถูกเปลี่ยนที่อื่น การเปลี่ยนในเครื่องยังคงอยู่ โปรดแก้ข้อขัดแย้งก่อนซิงค์',
  },
}
