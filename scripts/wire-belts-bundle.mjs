import fs from 'node:fs'
import assert from 'node:assert/strict'
const bases = JSON.parse(fs.readFileSync('frontend/src/features/crafting/topBases.json', 'utf8'))
const additions = Object.values(bases).filter(b => b.family === 'belts')
assert.equal(additions.length, 13)
const edit = (p, f) => { const s = fs.readFileSync(p, 'utf8'), n = f(s); assert.notEqual(s, n, p); fs.writeFileSync(p, n) }
fs.writeFileSync('backend/src/main/java/com/poe2craft/item/ReviewedBelts.java', `package com.poe2craft.item;
import java.util.Map;
/** Distinct source-reviewed Belt implicits; Charm slot range remains an unsupplied property. */
public final class ReviewedBelts {
 public static final Map<String,String> BASES = Map.ofEntries(${additions.map(b=>`Map.entry("${b.key}","${b.id}")`).join(',\n')});
 private ReviewedBelts() {}
 public static boolean supports(String id) { return BASES.containsValue(id); }
}
`)
edit('backend/src/main/java/com/poe2craft/item/infrastructure/ItemCatalogLoader.java', s => s.replace('&& !com.poe2craft.item.ReviewedWands.BASES.containsKey(key))', '&& !com.poe2craft.item.ReviewedWands.BASES.containsKey(key)\n && !com.poe2craft.item.ReviewedBelts.BASES.containsKey(key))').replace('com.poe2craft.item.ReviewedWands.BASES.get(\n                                                  key)', 'com.poe2craft.item.ReviewedWands.BASES.getOrDefault(\n                                                  key, com.poe2craft.item.ReviewedBelts.BASES.get(key))'))
edit('backend/src/main/java/com/poe2craft/bootstrap/CraftingConfiguration.java', s => s.replace('com.poe2craft.item.ReviewedWands.BASES\n                                                    .keySet()\n                                                    .stream()', 'java.util.stream.Stream.concat(com.poe2craft.item.ReviewedWands.BASES.keySet().stream(), com.poe2craft.item.ReviewedBelts.BASES.keySet().stream())'))
edit('backend/src/main/java/com/poe2craft/crafting/application/WorkbenchService.java', s => s.replace('ReviewedWands.BASES.get(key)', 'ReviewedWands.BASES.getOrDefault(key, ReviewedBelts.BASES.get(key))').replace('|| ReviewedAmulets.supports(catalog.base().id()))', '|| ReviewedAmulets.supports(catalog.base().id())\n || ReviewedBelts.supports(catalog.base().id()))'))
edit('backend/src/main/java/com/poe2craft/crafting/domain/WorkbenchSimulator.java', s => s.replace('private String baseRuleVersion() {', 'private String baseRuleVersion() {\n if (ReviewedBelts.supports(catalog.base().id())) return "distinct-belts-workbench-v1";').replace('public String ledgerVersion() {', 'public String ledgerVersion() {\n if (ReviewedBelts.supports(catalog.base().id())) return "belt-unverified-numeric-assumptions-v1";'))
for (const p of ['frontend/src/features/crafting/CraftingPage.tsx', 'frontend/src/features/crafting/craftingApi.ts', 'frontend/src/features/crafting/draft.ts']) edit(p, s => {
  s = s.replace(/^(\s*)\| 'wrath'/gm, (_, i) => `${i}| 'wrath'\n${additions.map(b => `${i}| '${b.key}'`).join('\n')}`)
  if (p.endsWith('draft.ts')) s = s.replace(/(\s*wrath: 'Item[^\n]+,)/, `$1\n${additions.map(b=>`  '${b.key}': 'Item Class: Belts\\nRarity: Normal\\n${b.name}',`).join('\n')}`)
  if (p.endsWith('CraftingPage.tsx')) s = s.replace("  wrath: 'Wrath_Sceptre',", `  wrath: 'Wrath_Sceptre',\n${additions.map(b=>`  '${b.key}': '${b.slug}',`).join('\n')}`)
  return s
})
edit('frontend/src/features/crafting/topBases.ts', s => s.replace("      'wands',", "      'wands',\n      'belts',"))
edit('frontend/src/features/crafting/workbenchApi.ts', s => s.replace("      'wands',", "      'wands',\n      'belts',").replace("action === 'PERFECT_ESSENCE_INSULATION' &&", "action === 'PERFECT_ESSENCE_INSULATION' &&\n topBase(reviewedKey ?? '')?.family !== 'belts' &&"))
edit('frontend/src/features/crafting/qualityLimit.ts', s => s.replace('  if (\n    !topBaseKey', "  if (topBase(topBaseKey(state.baseItemId) ?? '')?.family === 'belts') return null\n  if (\n    !topBaseKey"))
console.log('Wired thirteen Belts through reviewed catalog paths; Catalyst and quality cap remain unsupported')
