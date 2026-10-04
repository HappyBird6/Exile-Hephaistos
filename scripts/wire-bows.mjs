import fs from 'node:fs'
import assert from 'node:assert/strict'
const read = p => fs.readFileSync(p, 'utf8')
const write = (p, s) => fs.writeFileSync(p, s)
const keys = ['warmonger', 'guardian', 'gemini', 'fanatic', 'obliterator']
const manifest = JSON.parse(read('backend/src/main/resources/catalog/top-bases.json'))
for (const p of ['frontend/src/features/crafting/CraftingPage.tsx', 'frontend/src/features/crafting/craftingApi.ts', 'frontend/src/features/crafting/draft.ts']) {
  let s = read(p)
  if (!s.includes("| 'warmonger'")) s = s.replace(/^(\s*)\| 'daggerfoot'/gm, (_, i) => `${i}| 'daggerfoot'\n${keys.map(k => `${i}| '${k}'`).join('\n')}`)
  if (p.endsWith('draft.ts') && !s.includes("'warmonger':")) s = s.replace(/(\s*'?daggerfoot'?: 'Item[^\n]+,)/, `$1\n${keys.map(k => `  '${k}': 'Item Class: Bows\\nRarity: Normal\\n${manifest[k].name}',`).join('\n')}`)
  if (p.endsWith('CraftingPage.tsx')) {
    if (!s.includes("'warmonger':")) s = s.replace(/(\s*'?daggerfoot'?: 'Daggerfoot_Shoes',)/, `$1\n${keys.map(k => `  '${k}': '${manifest[k].slug}',`).join('\n')}`)
    s = s.replaceAll("catalogBase === 'bow'", "(catalogBase === 'bow' || topBase(catalogBase)?.family === 'bows')")
  }
  write(p,s)
}
let p = 'frontend/src/features/crafting/topBases.ts'
write(p, read(p).replace('  armour: number', '  implicitModifierId?: string\n  implicitStats?: { id: string; min: number; max: number }[]\n  armour: number').replace("['gloves', 'helmets', 'body', 'boots'].includes(data[key].family)", "['gloves', 'helmets', 'body', 'boots', 'bows'].includes(data[key].family)"))
p = 'frontend/src/features/crafting/workbenchApi.ts'
write(p, read(p).replaceAll("['gloves', 'helmets', 'body', 'boots']", "['gloves', 'helmets', 'body', 'boots', 'bows']").replace("'Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow1') ||", "'Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow1' &&\n      topBase(reviewedKey ?? '')?.family !== 'bows') ||"))
p = 'frontend/src/features/crafting/craftingApi.ts'
write(p, read(p).replace(': v.state.implicits.length !== 0)', `: topBase(base)?.implicitModifierId
              ? v.state.implicits.length !== 1 ||
                v.state.implicits[0]?.modifierId !== topBase(base)!.implicitModifierId ||
                v.state.implicits[0]?.fractured === true ||
                Object.keys(v.state.implicits[0]?.values ?? {}).length !== topBase(base)!.implicitStats!.length ||
                !topBase(base)!.implicitStats!.every(s => v.state.implicits[0]?.values[s.id] === s.max)
              : v.state.implicits.length !== 0)`))
write('backend/src/main/java/com/poe2craft/item/ReviewedBows.java', `package com.poe2craft.item;\nimport java.util.Map;\n/** Source-reviewed ordinary endgame Bow categories; local properties are display data. */\npublic final class ReviewedBows {\n public static final Map<String,String> BASES=Map.of(\n${keys.map(k=>`"${k}", "${manifest[k].id}"`).join(',\n')});\n private ReviewedBows() {}\n public static boolean supports(String id) { return BASES.containsValue(id); }\n}\n`)
p = 'backend/src/main/java/com/poe2craft/item/infrastructure/ItemCatalogLoader.java'
write(p, read(p).replace('&& !com.poe2craft.item.ReviewedBoots.BASES.containsKey(key))', '&& !com.poe2craft.item.ReviewedBoots.BASES.containsKey(key)\n && !com.poe2craft.item.ReviewedBows.BASES.containsKey(key))').replace('key, com.poe2craft.item.ReviewedBoots.BASES.get(key))', 'key, com.poe2craft.item.ReviewedBoots.BASES.getOrDefault(key, com.poe2craft.item.ReviewedBows.BASES.get(key)))').replace('id, name, "https://poe2db.tw/us/" + name.replace(\' \', \'_\'), "", 1, 1, 3, 3)', 'id, name, "https://poe2db.tw/us/" + name.replace(\' \', \'_\'), pool.base().implicitModifierId(), 1, 1, 3, 3)'))
p = 'backend/src/main/java/com/poe2craft/crafting/application/WorkbenchService.java'
let s = read(p).replace('ReviewedBoots.BASES.get(key)', 'ReviewedBoots.BASES.getOrDefault(key, ReviewedBows.BASES.get(key))')
s = s.replace(': List.of(),\n                List.of(),\n                Set.of());', `: ReviewedBows.supports(catalog.base().id()) && catalog.base().hasImplicit()
                            ? List.of(new ModifierInstance(catalog.base().implicitModifierId(),
                                catalog.find(catalog.base().implicitModifierId()).orElseThrow().stats().stream()
                                  .collect(java.util.stream.Collectors.toMap(ModifierDefinition.StatRange::id, stat -> stat.max()))))
                            : List.of(),
                List.of(),
                Set.of());`)
// Generic reviewed registration preserves all existing call sites.
write(p,s)
p = 'backend/src/main/java/com/poe2craft/bootstrap/CraftingConfiguration.java'
write(p, read(p).replace('com.poe2craft.item.ReviewedBoots.BASES.keySet().stream()', 'java.util.stream.Stream.concat(com.poe2craft.item.ReviewedBoots.BASES.keySet().stream(), com.poe2craft.item.ReviewedBows.BASES.keySet().stream())'))
p = 'backend/src/main/java/com/poe2craft/crafting/domain/QualityLimitRules.java'
write(p,read(p).replace('&& !ReviewedBoots.supports(state.baseItemId())', '&& !ReviewedBoots.supports(state.baseItemId())\n && !ReviewedBows.supports(state.baseItemId())'))
p = 'backend/src/main/java/com/poe2craft/crafting/domain/WorkbenchSimulator.java'
write(p,read(p).replace('private String baseRuleVersion() {', 'private String baseRuleVersion() {\n if (ReviewedBows.supports(catalog.base().id())) return "endgame-bows-workbench-v1";').replace('public String ledgerVersion() {','public String ledgerVersion() {\n if (ReviewedBows.supports(catalog.base().id())) return "bow-unverified-numeric-assumptions-v1";'))
console.log('Wired five Bow variants without replacing legacy IDs')
