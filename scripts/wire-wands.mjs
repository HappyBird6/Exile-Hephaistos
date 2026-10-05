import fs from 'node:fs'
import assert from 'node:assert/strict'
const read = p => fs.readFileSync(p, 'utf8')
const edit = (p, f) => { const s = read(p), n = f(s); assert.notEqual(n,s,p); fs.writeFileSync(p,n) }
const bases = JSON.parse(read('backend/src/main/resources/catalog/top-bases.json'))
const wands = Object.values(bases).filter(b => b.family === 'wands')
const entries = wands.map(b => `Map.entry("${b.key}","${b.id}")`).join(',\n')
fs.writeFileSync('backend/src/main/java/com/poe2craft/item/ReviewedWands.java', `package com.poe2craft.item;
import java.util.Map;
/** Source-reviewed ordinary Wand skill families; granted skills are display-only. */
public final class ReviewedWands {
 public static final Map<String,String> BASES=Map.ofEntries(${entries});
 private ReviewedWands() {}
 public static boolean supports(String id) { return BASES.containsValue(id); }
}
`)
edit('backend/src/main/java/com/poe2craft/item/infrastructure/ItemCatalogLoader.java',s=>s.replace('&& !com.poe2craft.item.ReviewedSceptres.BASES.containsKey(key))','&& !com.poe2craft.item.ReviewedSceptres.BASES.containsKey(key)\n && !com.poe2craft.item.ReviewedWands.BASES.containsKey(key))').replace('com.poe2craft.item.ReviewedSceptres.BASES.get(key)','com.poe2craft.item.ReviewedSceptres.BASES.getOrDefault(key,com.poe2craft.item.ReviewedWands.BASES.get(key))'))
edit('backend/src/main/java/com/poe2craft/bootstrap/CraftingConfiguration.java',s=>s.replace('com.poe2craft.item.ReviewedSceptres.BASES\n                                                .keySet()\n                                                .stream()', 'java.util.stream.Stream.concat(com.poe2craft.item.ReviewedSceptres.BASES.keySet().stream(), com.poe2craft.item.ReviewedWands.BASES.keySet().stream())'))
edit('backend/src/main/java/com/poe2craft/crafting/application/WorkbenchService.java',s=>s.replace('ReviewedSceptres.BASES.get(key)','ReviewedSceptres.BASES.getOrDefault(key,ReviewedWands.BASES.get(key))'))
edit('backend/src/main/java/com/poe2craft/crafting/domain/WorkbenchSimulator.java',s=>s.replaceAll('catalog.base().id().equals(WandEssenceTargets.BASE_ID)', '(catalog.base().id().equals(WandEssenceTargets.BASE_ID) || ReviewedWands.supports(catalog.base().id()))'))
edit('backend/src/main/java/com/poe2craft/crafting/domain/QualityLimitRules.java',s=>s.replace('&& !ReviewedSceptres.supports(state.baseItemId())','&& !ReviewedSceptres.supports(state.baseItemId())\n && !ReviewedWands.supports(state.baseItemId())'))
for (const p of ['frontend/src/features/crafting/CraftingPage.tsx', 'frontend/src/features/crafting/craftingApi.ts', 'frontend/src/features/crafting/draft.ts']) edit(p,s=>{
 s=s.replace(/^(\s*)\| 'hallowed'/gm,(_,i)=>`${i}| 'hallowed'\n${wands.map(b=>`${i}| '${b.key}'`).join('\n')}`)
 if(p.endsWith('draft.ts')) s=s.replace(/(\s*hallowed: 'Item[^\n]+,)/,`$1\n${wands.map(b=>`  ${b.key}: 'Item Class: Wands\\nRarity: Normal\\n${b.name}',`).join('\n')}`)
 if(p.endsWith('CraftingPage.tsx')) {
  s=s.replace("  hallowed: 'Hallowed_Sceptre',",`  hallowed: 'Hallowed_Sceptre',\n${wands.map(b=>`  ${b.key}: '${b.slug}',`).join('\n')}`)
  s=s.replaceAll("catalogBase === 'wand'", "(catalogBase === 'wand' || topBase(catalogBase)?.family === 'wands')")
  s=s.replace("...((catalogBase === 'wand' || topBase(catalogBase)?.family === 'wands')\n                ? [", "...(catalogBase === 'wand'\n                ? [")
 }
 return s
})
for(const p of ['frontend/src/features/crafting/topBases.ts','frontend/src/features/crafting/workbenchApi.ts']) edit(p,s=>s.replace("      'sceptres',","      'sceptres',\n      'wands',"))
console.log('Wired nine Wand families through reviewed catalog infrastructure')
