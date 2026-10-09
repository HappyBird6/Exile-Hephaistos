import fs from 'node:fs'
import assert from 'node:assert/strict'
const read = p => fs.readFileSync(p, 'utf8')
const write = (p, value) => fs.writeFileSync(p, value)
const keys = ['massive', 'sirenscale', 'adherent']
for (const path of ['frontend/src/features/crafting/CraftingPage.tsx', 'frontend/src/features/crafting/craftingApi.ts', 'frontend/src/features/crafting/draft.ts']) {
  let text = read(path)
  if (!text.includes("| 'massive'")) text = text.replace(/^(\s*)\| 'imperial'/gm, (_, indent) => `${indent}| 'imperial'\n${keys.map(k => `${indent}| '${k}'`).join('\n')}`)
  if (path.endsWith('draft.ts') && !text.includes("massive: 'Item")) text = text.replace("  imperial: 'Item Class: Helmets\\nRarity: Normal\\nImperial Greathelm',", "  imperial: 'Item Class: Helmets\\nRarity: Normal\\nImperial Greathelm',\n  massive: 'Item Class: Gloves\\nRarity: Normal\\nMassive Mitts',\n  sirenscale: 'Item Class: Gloves\\nRarity: Normal\\nSirenscale Gloves',\n  adherent: 'Item Class: Gloves\\nRarity: Normal\\nAdherent Cuffs',")
  if (path.endsWith('CraftingPage.tsx')) {
    text = text.replace("import { topBase, topBaseKey }", "import { topBase, topBaseKey, reviewedGloveKeys }")
    if (!text.includes("massive: 'Massive_Mitts'")) text = text.replace("  imperial: 'Imperial_Greathelm',", "  imperial: 'Imperial_Greathelm',\n  massive: 'Massive_Mitts',\n  sirenscale: 'Sirenscale_Gloves',\n  adherent: 'Adherent_Cuffs',")
    text = text.replace(/catalogBase === 'stocky'(\s*\? 'Gloves')/g, "(catalogBase === 'stocky' || topBase(catalogBase)?.family === 'gloves')$1")
    if (!text.includes("id: 'base-energy-shield'")) text = text.replace('            properties: [', `            properties: [
              ...(topBase(catalogBase)?.family === 'gloves'
                ? [
                    ...(topBase(catalogBase)!.armour > 0 ? [{ id: 'base-armour', text: t('base.armour', { value: topBase(catalogBase)!.armour }) }] : []),
                    ...((topBase(catalogBase)!.energyShield ?? 0) > 0 ? [{ id: 'base-energy-shield', text: t('base.energy_shield', { value: topBase(catalogBase)!.energyShield! }) }] : []),
                  ]
                : []),`)
    if (!text.includes('reviewedGloveKeys.map')) text = text.replace('                    <option value="soldier">', `                    {reviewedGloveKeys.map((key) => (
                      <option key={key} value={key}>{name(topBase(key)!.slug, topBase(key)!.name)}</option>
                    ))}
                    <option value="soldier">`)
  }
  write(path, text)
}
const messagesPath = 'frontend/src/shared/i18n/messages.json'
const messages = JSON.parse(read(messagesPath))
const energyShield = { en: 'Base Energy Shield: {value} (not computed)', ko: '기본 에너지 보호막: {value} (미계산)', 'zh-CN': '基础能量护盾：{value}（未计算）', 'zh-TW': '基礎能量護盾：{value}（未計算）', ja: '基本エナジーシールド: {value} (未計算)', es: 'Escudo de energía base: {value} (sin calcular)' }
for (const [locale, text] of Object.entries(energyShield)) messages[locale]['base.energy_shield'] = text
const uniform = {
  en: 'USER-APPROVED SIMULATOR MODEL: equal 1/N among eligible Gloves modifiers after family, side and level restrictions. Actual game spawn weights are unavailable; published DropChance is not a verified game weight.',
  ko: '사용자가 승인한 시뮬레이터 모델: 계열·접두/접미·아이템 레벨 제한을 적용한 적격 장갑 옵션 중 1/N로 선택합니다. 실제 게임 생성 가중치는 알려지지 않았으며 공개 DropChance는 검증된 게임 가중치가 아닙니다.',
  ja: 'ユーザー承認のシミュレーターモデル: ファミリー、接頭辞・接尾辞、アイテムレベルの制限を満たすグローブモッドを等確率1/Nで選択します。実際の生成重みは不明で、公開DropChanceは検証されたゲーム内の重みではありません。',
  'zh-CN': '用户批准的模拟器模型：在满足词缀族、前后缀和物品等级限制的手套词缀中以1/N等概率选择。实际游戏生成权重未知，公开DropChance并非经过验证的游戏权重。',
  'zh-TW': '使用者核准的模擬器模型：在符合詞綴族、前後綴和物品等級限制的手套詞綴中以1/N等機率選擇。實際遊戲生成權重未知，公開DropChance並非經過驗證的遊戲權重。',
  es: 'MODELO DE SIMULACIÓN APROBADO POR EL USUARIO: selección uniforme 1/N entre los modificadores de guantes válidos tras aplicar las restricciones de familia, prefijo/sufijo y nivel de objeto. Los pesos reales del juego son desconocidos; el DropChance publicado no es un peso de juego verificado.',
}
const units = { en: 'eligible per-base Gloves ordinary modifier', ko: '베이스별 적격 장갑 일반 옵션', ja: 'ベースごとの適格なグローブ通常モッド', 'zh-CN': '该基底的可用手套普通词缀', 'zh-TW': '該基底的適格手套普通詞綴', es: 'modificador normal de guantes válido para esta base' }
for (const locale of Object.keys(uniform)) {
  messages[locale]['server.gloves_uniform_candidates'] = uniform[locale]
  messages[locale]['server.gloves_candidate_unit'] = units[locale]
}
write(messagesPath, JSON.stringify(messages, null, 2) + '\n')
const manifest = JSON.parse(read('backend/src/main/resources/catalog/top-bases.json'))
const essence = JSON.parse(read('backend/src/main/resources/catalog/top-base-essences.json'))
const registryPath = 'backend/src/main/resources/crafting/registry-v2.json'
const registry = JSON.parse(read(registryPath))
for (const key of keys) {
  const base = manifest[key]; assert(base)
  registry.workbenchBases[key] = { ...registry.workbenchBases.stocky, ruleVersion: 'gloves-workbench-uniform-v1', ledgerVersion: 'gloves-uniform-candidates-unverified-rolls-v1', numericModelStatus: 'USER_APPROVED_UNIFORM_CANDIDATES_UNVERIFIED_NUMERIC_ROLLS', baseItemId: base.id, source: base.sourceUrl, sourceSha256: base.sourceSha256, baseArmour: base.armour, baseEnergyShield: base.energyShield, requiredCharacterLevel: base.requiredLevel, requiredStrength: base.strength, requiredIntelligence: base.intelligence, qualityMaximum: 20, implicit: 'NONE_IN_REVIEWED_BASE', augmentSockets: null, projection: 'EXPLICIT_AFFIX_ONLY', weightPolicy: 'UNVERIFIED_GAME_WEIGHTS_EXPLICIT_UNIFORM_ELIGIBLE_CANDIDATES' }
  const supportedActions = new Set([...Object.keys(essence[key].fixed), ...Object.keys(essence[key].replacements), 'PERFECT_ESSENCE_GROUNDING', 'PERFECT_ESSENCE_OPULENCE', 'ESSENCE_ABYSS'])
  for (const entry of registry.entries) {
    if (!entry.supportedBases?.includes('stocky') || entry.serviceScope === 'DEFERRED') continue
    const action = entry.action ?? entry.workbenchAction
    if (/Artificer|Alloy|Horror/.test(entry.id)) continue
    if (entry.category === 'ESSENCE' && action && !supportedActions.has(action)) continue
    if (!entry.supportedBases.includes(key)) entry.supportedBases.push(key)
  }
}
write(registryPath, JSON.stringify(registry, null, 2) + '\n')
