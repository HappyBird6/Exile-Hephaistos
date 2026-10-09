import fs from 'node:fs'
import assert from 'node:assert/strict'
const read = p => fs.readFileSync(p, 'utf8')
const write = (p, s) => fs.writeFileSync(p, s)
const json = p => JSON.parse(read(p))
const keys = ['tasalian', 'drakeskin', 'sekhema', 'blacksteel-boots', 'faithful', 'daggerfoot']
const manifest = json('backend/src/main/resources/catalog/top-bases.json')
for (const k of keys) assert(manifest[k]?.family === 'boots')
for (const p of ['frontend/src/features/crafting/CraftingPage.tsx', 'frontend/src/features/crafting/craftingApi.ts', 'frontend/src/features/crafting/draft.ts']) {
  let s = read(p)
  if (!s.includes("| 'tasalian'")) s = s.replace(/^(\s*)\| 'cryptic'/gm, (_, i) => `${i}| 'cryptic'\n${keys.map(k => `${i}| '${k}'`).join('\n')}`)
  if (p.endsWith('draft.ts') && !s.includes("'tasalian':")) s = s.replace(/(\s*'?cryptic'?: 'Item[^\n]+,)/, `$1\n${keys.map(k => `  '${k}': 'Item Class: Boots\\nRarity: Normal\\n${manifest[k].name}',`).join('\n')}`)
  if (p.endsWith('CraftingPage.tsx')) {
    if (!s.includes("'tasalian':")) s = s.replace(/(\s*'?cryptic'?: 'Cryptic_Crown',)/, `$1\n${keys.map(k => `  '${k}': '${manifest[k].slug}',`).join('\n')}`)
    s = s.replaceAll("['gloves', 'helmets', 'body']", "['gloves', 'helmets', 'body', 'boots']")
    s = s.replaceAll(": 'Amulet',", ": topBase(catalogBase)?.family === 'boots' ? 'Boots' : 'Amulet',")
  }
  write(p, s)
}
for (const p of ['frontend/src/features/crafting/topBases.ts', 'frontend/src/features/crafting/workbenchApi.ts']) write(p, read(p).replaceAll("['gloves', 'helmets', 'body']", "['gloves', 'helmets', 'body', 'boots']"))
write('backend/src/main/java/com/poe2craft/item/ReviewedBoots.java', `package com.poe2craft.item;\nimport java.util.Map;\n/** Source-reviewed highest-tier ordinary Boots defence archetypes. */\npublic final class ReviewedBoots {\n public static final Map<String,String> BASES=Map.of(\n${keys.map(k => `"${k}", "${manifest[k].id}"`).join(',\n')});\n private ReviewedBoots() {}\n public static boolean supports(String id) {return BASES.containsValue(id);}\n}\n`)
let p = 'backend/src/main/java/com/poe2craft/item/infrastructure/ItemCatalogLoader.java'
write(p, read(p).replace('&& !com.poe2craft.item.ReviewedBodies.BASES.containsKey(key))', '&& !com.poe2craft.item.ReviewedBodies.BASES.containsKey(key)\n && !com.poe2craft.item.ReviewedBoots.BASES.containsKey(key))').replace('key, com.poe2craft.item.ReviewedBodies.BASES.get(key))', 'key, com.poe2craft.item.ReviewedBodies.BASES.getOrDefault(key, com.poe2craft.item.ReviewedBoots.BASES.get(key)))'))
p = 'backend/src/main/java/com/poe2craft/crafting/application/WorkbenchService.java'
write(p, read(p).replace('ReviewedBodies.BASES.get(key)', 'ReviewedBodies.BASES.getOrDefault(key, ReviewedBoots.BASES.get(key))'))
p = 'backend/src/main/java/com/poe2craft/bootstrap/CraftingConfiguration.java'
write(p, read(p).replace('com.poe2craft.item.ReviewedBodies.BASES.keySet().stream()', 'java.util.stream.Stream.concat(com.poe2craft.item.ReviewedBodies.BASES.keySet().stream(), com.poe2craft.item.ReviewedBoots.BASES.keySet().stream())'))
p = 'backend/src/main/java/com/poe2craft/crafting/domain/WorkbenchSimulator.java'
write(p, read(p).replace('private String baseRuleVersion() {', 'private String baseRuleVersion() {\n if (ReviewedBoots.supports(catalog.base().id())) return "boots-workbench-uniform-v1";').replace('public String ledgerVersion() {', 'public String ledgerVersion() {\n if (ReviewedBoots.supports(catalog.base().id())) return "boots-uniform-candidates-unverified-rolls-v1";').replace('List<ModifierDefinition> candidates, List<Assumption> assumptions) {', 'List<ModifierDefinition> candidates, List<Assumption> assumptions) {\n if (ReviewedBoots.supports(catalog.base().id())) assumptions.add(new Assumption("boots-uniform-candidates-v1", "eligible per-base Boots ordinary modifier", candidates.size(), candidates.stream().map(ModifierDefinition::id).sorted().toList(), null, null, catalog.metadata().sourceUrl(), "USER-APPROVED SIMULATOR MODEL: equal 1/N after family, side and level restrictions. Actual game spawn weights and numeric roll probabilities remain unverified."));'))
const registryPath = 'backend/src/main/resources/crafting/registry-v2.json'
p = 'backend/src/main/java/com/poe2craft/crafting/domain/QualityLimitRules.java'
write(p, read(p).replace('&& !ReviewedGloves.supports(state.baseItemId())', '&& !ReviewedGloves.supports(state.baseItemId())\n && !ReviewedBoots.supports(state.baseItemId())'))
const registry = json(registryPath), overrides = json('backend/src/main/resources/catalog/top-base-essences.json')
for (const k of keys) {
  const b = manifest[k]
  registry.workbenchBases[k] = { ...registry.workbenchBases.massive, ruleVersion: 'boots-workbench-uniform-v1', ledgerVersion: 'boots-uniform-candidates-unverified-rolls-v1', baseItemId: b.id, source: b.sourceUrl, sourceSha256: b.sourceSha256, baseArmour: b.armour, baseEvasion: b.evasion, baseEnergyShield: b.energyShield, requiredCharacterLevel: b.requiredLevel, requiredStrength: b.strength, requiredDexterity: b.dexterity, requiredIntelligence: b.intelligence }
  const actions = new Set([...Object.keys(overrides[k].fixed), ...Object.keys(overrides[k].replacements)])
  for (const e of registry.entries) {
    if (e.serviceScope === 'DEFERRED') continue
    if (e.category === 'ESSENCE') {
      if (!actions.has(e.action ?? e.workbenchAction)) continue
    } else if (!e.supportedBases?.includes('massive')) continue
    // Socket, alloy and catalyst behaviour require separate Boots evidence.
    if (e.id === 'Omen_of_the_Blessed' || ['ARTIFICER', 'RUNIC_ALLOY', 'ESSENCE_HORROR'].includes(e.action ?? e.workbenchAction) || ['ALLOY', 'CATALYST'].includes(e.category)) continue
    if (!e.supportedBases.includes(k)) e.supportedBases.push(k)
  }
}
write(registryPath, JSON.stringify(registry, null, 2) + '\n')
