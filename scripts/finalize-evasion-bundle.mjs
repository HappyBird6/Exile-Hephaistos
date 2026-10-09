import fs from 'node:fs'
import assert from 'node:assert/strict'
const read = p => JSON.parse(fs.readFileSync(p, 'utf8'))
const path = 'frontend/src/shared/i18n/messages.json'
const messages = read(path)
const reasons = {
  en: 'USER-APPROVED SIMULATOR MODEL: equal 1/N among eligible Helmet modifiers after family, side and level restrictions. Actual game spawn weights are unavailable; published DropChance is not a verified game weight.',
  ko: '사용자가 승인한 시뮬레이터 모델: 계열·접두/접미·아이템 레벨 제한 후 적격 투구 옵션을 1/N로 선택합니다. 실제 게임 생성 가중치는 알려지지 않았으며 공개 DropChance는 검증된 게임 가중치가 아닙니다.',
  ja: 'ユーザー承認のシミュレーターモデル：系統・接頭辞/接尾辞・アイテムレベル制限後の適格な兜モッドを1/Nで選択します。実際のゲーム生成重みは不明で、公開DropChanceは検証済みのゲーム重みではありません。',
  'zh-CN': '用户批准的模拟器模型：应用词缀组、前缀/后缀和物品等级限制后，以1/N选择符合条件的头盔词缀。实际游戏生成权重未知；公开DropChance不是已验证的游戏权重。',
  'zh-TW': '使用者批准的模擬器模型：套用詞綴組、前綴/後綴和物品等級限制後，以1/N選擇符合條件的頭盔詞綴。實際遊戲生成權重未知；公開DropChance不是已驗證的遊戲權重。',
  es: 'MODELO DE SIMULACIÓN APROBADO POR EL USUARIO: selección uniforme 1/N entre los modificadores de casco válidos tras aplicar las restricciones de familia, prefijo/sufijo y nivel de objeto. Los pesos reales del juego son desconocidos; el DropChance publicado no es un peso de juego verificado.',
}
const units = { en: 'eligible per-base Helmet ordinary modifier', ko: '베이스별 적격 투구 일반 옵션', ja: 'ベースごとの適格な兜通常モッド', 'zh-CN': '每种基底符合条件的头盔普通词缀', 'zh-TW': '各基底符合條件的頭盔普通詞綴', es: 'modificador normal de casco válido para esta base' }
for (const locale of Object.keys(reasons)) { messages[locale]['server.helmets_uniform_candidates'] = reasons[locale]; messages[locale]['server.helmets_candidate_unit'] = units[locale] }
fs.writeFileSync(path, JSON.stringify(messages, null, 2) + '\n')
const manifest = read('backend/src/main/resources/catalog/top-bases.json')
const counts = { polished: 174, 'blacksteel-gloves': 184, 'war-wraps': 180, freebooter: 137, gladiatorial: 147, grinning: 135 }
const registryPath = 'backend/src/main/resources/crafting/registry-v2.json'
const registry = read(registryPath), essences = read('backend/src/main/resources/catalog/top-base-essences.json')
for (const key of Object.keys(counts)) {
  const sourced = new Set([...Object.keys(essences[key].fixed), ...Object.keys(essences[key].replacements)])
  if (manifest[key].family === 'gloves') for (const action of ['PERFECT_ESSENCE_GROUNDING', 'PERFECT_ESSENCE_OPULENCE']) sourced.add(action)
  for (const entry of registry.entries) if (entry.serviceScope !== 'DEFERRED' && sourced.has(entry.action ?? entry.workbenchAction)) {
    assert(entry.supportedBases, `Existing explicit supported-base field required: ${entry.id}`)
    if (!entry.supportedBases.includes(key)) entry.supportedBases.push(key)
  }
}
fs.writeFileSync(registryPath, JSON.stringify(registry, null, 2) + '\n')
for (const [key, count] of Object.entries(counts)) {
  const base = manifest[key], root = `backend/src/main/resources/catalog/${base.pool}`
  const c = read(`${root}/catalog.json`), raw = read(`${root}/base.raw.json`), details = read(`${root}/details.raw.json`)
  assert.equal(raw.length, count); assert.equal(details.proofs.length, count)
  assert.equal(c.modifiers.filter(d => d.weight > 0).length, count)
  assert.equal(c.modifiers.filter(d => !d.weight).length, base.family === 'helmets' ? 1 : 4)
  for (const proof of details.proofs) assert(c.modifiers.some(d => d.id === proof.id))
  console.log(key, count, base.id)
}
