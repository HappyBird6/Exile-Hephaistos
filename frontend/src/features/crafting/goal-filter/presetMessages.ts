import type { Locale } from '../../../shared/i18n/i18n'
type Copy = {
  title: string
  name: string
  save: string
  load: string
  choose: string
  guest: string
  account: string
  saved: string
  loaded: string
  duplicate: string
  incompatible: string
  error: string
}
export const presetMessages: Record<Locale, Copy> = {
  en: {
    title: 'Favorite setups',
    name: 'Favorite name',
    save: 'Save favorite',
    load: 'Load favorite',
    choose: 'Select a favorite',
    guest: 'Guest favorites',
    account: 'My favorites',
    saved: 'Favorite saved.',
    loaded: 'Favorite loaded.',
    duplicate: 'This name is already in use.',
    incompatible: 'These saved goal rows are unavailable.',
    error: 'The favorite could not be saved or loaded.',
  },
  ko: {
    title: '제작 설정 즐겨찾기',
    name: '즐겨찾기 이름',
    save: '즐겨찾기 저장',
    load: '즐겨찾기 불러오기',
    choose: '즐겨찾기 선택',
    guest: '비로그인 즐겨찾기',
    account: '내 즐겨찾기',
    saved: '즐겨찾기를 저장했습니다.',
    loaded: '즐겨찾기를 불러왔습니다.',
    duplicate: '이미 사용 중인 이름입니다.',
    incompatible: '다음 목표 행을 현재 아이템에 불러올 수 없습니다.',
    error: '즐겨찾기를 저장하거나 불러올 수 없습니다.',
  },
  'zh-CN': {
    title: '收藏的制作设置',
    name: '收藏名称',
    save: '保存收藏',
    load: '加载收藏',
    choose: '选择收藏',
    guest: '游客收藏',
    account: '我的收藏',
    saved: '已保存收藏。',
    loaded: '已加载收藏。',
    duplicate: '此名称已被使用。',
    incompatible: '以下目标行无法用于此物品。',
    error: '无法保存或加载收藏。',
  },
  'zh-TW': {
    title: '收藏的製作設定',
    name: '收藏名稱',
    save: '儲存收藏',
    load: '載入收藏',
    choose: '選擇收藏',
    guest: '訪客收藏',
    account: '我的收藏',
    saved: '已儲存收藏。',
    loaded: '已載入收藏。',
    duplicate: '此名稱已被使用。',
    incompatible: '以下目標列無法用於此物品。',
    error: '無法儲存或載入收藏。',
  },
  ja: {
    title: 'お気に入りのクラフト設定',
    name: 'お気に入りの名前',
    save: 'お気に入りに保存',
    load: 'お気に入りを読み込む',
    choose: 'お気に入りを選択',
    guest: 'ゲストのお気に入り',
    account: '自分のお気に入り',
    saved: 'お気に入りに保存しました。',
    loaded: 'お気に入りを読み込みました。',
    duplicate: 'この名前は既に使われています。',
    incompatible: 'このアイテムに読み込めない目標があります。',
    error: 'お気に入りを保存または読み込みできませんでした。',
  },
  es: {
    title: 'Configuraciones favoritas',
    name: 'Nombre del favorito',
    save: 'Guardar favorito',
    load: 'Cargar favorito',
    choose: 'Seleccionar un favorito',
    guest: 'Favoritos de invitado',
    account: 'Mis favoritos',
    saved: 'Favorito guardado.',
    loaded: 'Favorito cargado.',
    duplicate: 'Este nombre ya está en uso.',
    incompatible:
      'Estas filas de objetivo no están disponibles para este objeto.',
    error: 'No se pudo guardar o cargar el favorito.',
  },
  ru: {
    title: 'Избранные настройки крафта',
    name: 'Название набора',
    save: 'Сохранить в избранное',
    load: 'Загрузить из избранного',
    choose: 'Выберите набор',
    guest: 'Избранное гостя',
    account: 'Моё избранное',
    saved: 'Набор сохранён.',
    loaded: 'Набор загружен.',
    duplicate: 'Это название уже используется.',
    incompatible: 'Следующие условия цели недоступны для этого предмета.',
    error: 'Не удалось сохранить или загрузить набор.',
  },
  th: {
    title: 'ชุดตั้งค่าคราฟต์ที่ชื่นชอบ',
    name: 'ชื่อชุดโปรด',
    save: 'บันทึกชุดโปรด',
    load: 'โหลดชุดโปรด',
    choose: 'เลือกชุดโปรด',
    guest: 'ชุดโปรดของผู้เยี่ยมชม',
    account: 'ชุดโปรดของฉัน',
    saved: 'บันทึกชุดโปรดแล้ว',
    loaded: 'โหลดชุดโปรดแล้ว',
    duplicate: 'มีการใช้ชื่อนี้แล้ว',
    incompatible: 'ไม่สามารถโหลดเป้าหมายต่อไปนี้สำหรับไอเทมนี้ได้',
    error: 'ไม่สามารถบันทึกหรือโหลดชุดโปรดได้',
  },
}
