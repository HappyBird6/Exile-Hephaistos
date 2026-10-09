import fs from 'node:fs'
import assert from 'node:assert/strict'
const read = p => fs.readFileSync(p, 'utf8')
const write = (p, s) => fs.writeFileSync(p, s)
const manifest = JSON.parse(read('backend/src/main/resources/catalog/top-bases.json'))
const helmets = process.argv.includes('--helmets')
const keys = helmets ? ['freebooter', 'gladiatorial', 'grinning'] : ['polished', 'blacksteel-gloves', 'war-wraps']
for (const key of keys) assert(manifest[key])
for (const path of ['frontend/src/features/crafting/CraftingPage.tsx', 'frontend/src/features/crafting/craftingApi.ts', 'frontend/src/features/crafting/draft.ts']) {
  let s = read(path)
  if (!s.includes(`| '${keys[0]}'`)) s = s.replace(/^(\s*)\| 'adherent'/gm, (_, indent) => `${indent}| 'adherent'\n${keys.map(k => `${indent}| '${k}'`).join('\n')}`)
  if (path.endsWith('draft.ts') && !s.includes(`'${keys[0]}': 'Item`)) s = s.replace("  adherent: 'Item Class: Gloves\\nRarity: Normal\\nAdherent Cuffs',", "  adherent: 'Item Class: Gloves\\nRarity: Normal\\nAdherent Cuffs',\n" + keys.map(k => `  '${k}': 'Item Class: ${helmets ? 'Helmets' : 'Gloves'}\\nRarity: Normal\\n${manifest[k].name}',`).join('\n'))
  if (path.endsWith('CraftingPage.tsx')) {
    if (!s.includes(`'${keys[0]}': '${manifest[keys[0]].slug}'`)) s = s.replace("  adherent: 'Adherent_Cuffs',", "  adherent: 'Adherent_Cuffs',\n" + keys.map(k => `  '${k}': '${manifest[k].slug}',`).join('\n'))
    if (!s.includes("id: 'base-evasion'")) s = s.replace("...((topBase(catalogBase)!.energyShield ?? 0) > 0", "...((topBase(catalogBase)!.evasion ?? 0) > 0 ? [{ id: 'base-evasion', text: t('base.evasion', { value: topBase(catalogBase)!.evasion! }) }] : []),\n                    ...((topBase(catalogBase)!.energyShield ?? 0) > 0")
  }
  if (helmets && path.endsWith('CraftingPage.tsx')) {
    s = s.replaceAll("topBase(catalogBase)?.family === 'gloves'", "['gloves', 'helmets'].includes(topBase(catalogBase)?.family ?? '')")
    // Class display remains exact even though source properties share the same rendering path.
    s = s.replace("(catalogBase === 'stocky' || ['gloves', 'helmets'].includes(topBase(catalogBase)?.family ?? ''))", "(catalogBase === 'stocky' || topBase(catalogBase)?.family === 'gloves')")
    s = s.replace(/catalogBase === 'helmet' \|\|\s+catalogBase === 'imperial'(\s*\? 'Helmets')/g, "catalogBase === 'helmet' || catalogBase === 'imperial' || topBase(catalogBase)?.family === 'helmets'$1")
    s = s.replaceAll('reviewedGloveKeys', 'reviewedArmourKeys')
  }
  write(path, s)
}
const typePath = 'frontend/src/features/crafting/topBases.ts'
if (!helmets) write(typePath, read(typePath).replace('  energyShield?: number', '  evasion?: number\n  dexterity?: number\n  energyShield?: number'))
else write(typePath, read(typePath) + "\nexport const reviewedArmourKeys = (Object.keys(data) as TopBaseKey[]).filter(key => ['gloves', 'helmets'].includes(data[key].family))\n")
const messagePath = 'frontend/src/shared/i18n/messages.json'
const messages = JSON.parse(read(messagePath))
const evasion = { en: 'Base Evasion Rating: {value} (not computed)', ko: '기본 회피: {value} (미계산)', ja: '基礎回避力: {value} (未計算)', 'zh-CN': '基础闪避值：{value}（未计算）', 'zh-TW': '基礎閃避值：{value}（未計算）', es: 'Evasión base: {value} (sin calcular)' }
for (const [locale, text] of Object.entries(evasion)) messages[locale]['base.evasion'] = text
write(messagePath, JSON.stringify(messages, null, 2) + '\n')
const registryPath = 'backend/src/main/resources/crafting/registry-v2.json'
const registry = JSON.parse(read(registryPath))
const overrides = JSON.parse(read('backend/src/main/resources/catalog/top-base-essences.json'))
for (const key of keys) {
  const base = manifest[key]
  registry.workbenchBases[key] = { ...registry.workbenchBases[helmets ? 'imperial' : 'massive'], ruleVersion: helmets ? 'helmets-workbench-uniform-v1' : 'gloves-workbench-uniform-v1', ledgerVersion: helmets ? 'helmets-uniform-candidates-unverified-rolls-v1' : 'gloves-uniform-candidates-unverified-rolls-v1', weightPolicy: 'UNVERIFIED_GAME_WEIGHTS_EXPLICIT_UNIFORM_ELIGIBLE_CANDIDATES', baseItemId: base.id, source: base.sourceUrl, sourceSha256: base.sourceSha256, baseArmour: base.armour, baseEvasion: base.evasion, baseEnergyShield: base.energyShield, requiredCharacterLevel: base.requiredLevel, requiredStrength: base.strength, requiredDexterity: base.dexterity, requiredIntelligence: base.intelligence }
  const actions = new Set([...Object.keys(overrides[key].fixed), ...Object.keys(overrides[key].replacements), 'PERFECT_ESSENCE_GROUNDING', 'PERFECT_ESSENCE_OPULENCE', 'ESSENCE_ABYSS'])
  for (const entry of registry.entries) {
    if (!entry.supportedBases?.includes(helmets ? 'imperial' : 'massive') || entry.serviceScope === 'DEFERRED') continue
    if (entry.category === 'ESSENCE' && !actions.has(entry.action ?? entry.workbenchAction)) continue
    if (!entry.supportedBases.includes(key)) entry.supportedBases.push(key)
  }
}
write(registryPath, JSON.stringify(registry, null, 2) + '\n')
if (helmets) {
  write('backend/src/main/java/com/poe2craft/item/ReviewedHelmets.java', `package com.poe2craft.item;\nimport java.util.Map;\n/** Source-reviewed highest-tier evasion Helmet archetypes. */\npublic final class ReviewedHelmets {\n  public static final Map<String, String> BASES = Map.of(\n${keys.map(k => `    "${k}", "${manifest[k].id}"`).join(',\n')});\n  private ReviewedHelmets() {}\n  public static boolean supports(String id) { return BASES.containsValue(id); }\n}\n`)
  const loader = 'backend/src/main/java/com/poe2craft/item/infrastructure/ItemCatalogLoader.java'
  write(loader, read(loader).replace('if (!com.poe2craft.item.ReviewedGloves.BASES.containsKey(key))', 'if (!com.poe2craft.item.ReviewedGloves.BASES.containsKey(key) && !com.poe2craft.item.ReviewedHelmets.BASES.containsKey(key))').replace('id = com.poe2craft.item.ReviewedGloves.BASES.get(key);', 'id = com.poe2craft.item.ReviewedGloves.BASES.getOrDefault(key, com.poe2craft.item.ReviewedHelmets.BASES.get(key));').replaceAll('Reviewed Gloves catalog', 'Reviewed armour catalog').replaceAll('reviewed Gloves catalog', 'reviewed armour catalog'))
  const service = 'backend/src/main/java/com/poe2craft/crafting/application/WorkbenchService.java'
  write(service, read(service).replace('!catalog.base().id().equals(ReviewedGloves.BASES.get(key))', '!catalog.base().id().equals(ReviewedGloves.BASES.getOrDefault(key, ReviewedHelmets.BASES.get(key)))').replaceAll('registerReviewedGloves', 'registerReviewedArmour').replace('Unreviewed or duplicate Gloves registration', 'Unreviewed or duplicate armour registration'))
  const config = 'backend/src/main/java/com/poe2craft/bootstrap/CraftingConfiguration.java'
  write(config, read(config).replace('for (var key : com.poe2craft.item.ReviewedGloves.BASES.keySet())', 'for (var key : java.util.stream.Stream.concat(com.poe2craft.item.ReviewedGloves.BASES.keySet().stream(), com.poe2craft.item.ReviewedHelmets.BASES.keySet().stream()).toList())').replaceAll('registerReviewedGloves', 'registerReviewedArmour'))
  const targets = 'backend/src/main/java/com/poe2craft/crafting/domain/HelmetEssenceTargets.java'
  write(targets, read(targets).replace('return id.equals(BASE_ID)', 'return com.poe2craft.item.ReviewedHelmets.supports(id) || id.equals(BASE_ID)'))
  const simulator = 'backend/src/main/java/com/poe2craft/crafting/domain/WorkbenchSimulator.java'
  let s = read(simulator).replace('private String baseRuleVersion() {', 'private String baseRuleVersion() {\n    if (ReviewedHelmets.supports(catalog.base().id())) return "helmets-workbench-uniform-v1";').replace('public String ledgerVersion() {', 'public String ledgerVersion() {\n    if (ReviewedHelmets.supports(catalog.base().id())) return "helmets-uniform-candidates-unverified-rolls-v1";')
  s = s.replace('List<ModifierDefinition> candidates, List<Assumption> assumptions) {', 'List<ModifierDefinition> candidates, List<Assumption> assumptions) {\n    if (ReviewedHelmets.supports(catalog.base().id())) assumptions.add(new Assumption("helmets-uniform-candidates-v1", "eligible per-base Helmet ordinary modifier", candidates.size(), candidates.stream().map(ModifierDefinition::id).sorted().toList(), null, null, catalog.metadata().sourceUrl(), "USER-APPROVED SIMULATOR MODEL: equal 1/N among eligible Helmet modifiers after family, side and level restrictions. Actual game spawn weights are unavailable; published DropChance is not a verified game weight."));')
  write(simulator, s)
  const api = 'frontend/src/features/crafting/workbenchApi.ts'
  write(api, read(api).replace("reviewedKey && topBase(reviewedKey)?.family === 'gloves'", "reviewedKey && ['gloves', 'helmets'].includes(topBase(reviewedKey)?.family ?? '')"))
} else {
const javaPath = 'backend/src/main/java/com/poe2craft/item/ReviewedGloves.java'
let java = read(javaPath)
java = java.replace('"adherent", "Metadata/Items/Armours/Gloves/FourGlovesStrInt4Endgame");', '"adherent", "Metadata/Items/Armours/Gloves/FourGlovesStrInt4Endgame",\n' + keys.map(k => `          "${k}", "${manifest[k].id}"`).join(',\n') + ');')
write(javaPath, java)
const testPath = 'backend/src/test/java/com/poe2craft/bootstrap/ReviewedGlovesWorkbenchTest.java'
write(testPath, read(testPath).replaceAll('{"massive", "sirenscale", "adherent"}', '{"massive", "sirenscale", "adherent", "polished", "blacksteel-gloves", "war-wraps"}').replace('          default -> 188;', '          case "adherent" -> 188;\n          case "polished" -> 174;\n          case "blacksteel-gloves" -> 184;\n          case "war-wraps" -> 180;\n          default -> throw new IllegalArgumentException(key);'))
}
