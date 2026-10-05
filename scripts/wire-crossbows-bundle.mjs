import fs from 'node:fs'
import assert from 'node:assert/strict'
const bases=JSON.parse(fs.readFileSync('frontend/src/features/crafting/topBases.json','utf8'))
const additions=Object.values(bases).filter(b=>b.family==='crossbows')
assert.equal(additions.length,6)
const edit=(p,f)=>{const s=fs.readFileSync(p,'utf8'),n=f(s);assert.notEqual(s,n,p);fs.writeFileSync(p,n)}
fs.writeFileSync('backend/src/main/java/com/poe2craft/item/ReviewedCrossbows.java',`package com.poe2craft.item;
import java.util.Map;
/** Ordinary highest-tier Crossbow representatives; weapon properties and mechanics are display-only. */
public final class ReviewedCrossbows {
 public static final Map<String,String> BASES=Map.ofEntries(${additions.map(b=>`Map.entry("${b.key}","${b.id}")`).join(',\n')});
 private ReviewedCrossbows() {}
 public static boolean supports(String id) { return BASES.containsValue(id); }
}
`,{flag:'wx'})
edit('backend/src/main/java/com/poe2craft/item/infrastructure/ItemCatalogLoader.java',s=>s.replace('&& !com.poe2craft.item.ReviewedBelts.BASES.containsKey(key))','&& !com.poe2craft.item.ReviewedBelts.BASES.containsKey(key)\n && !com.poe2craft.item.ReviewedCrossbows.BASES.containsKey(key))').replace(/com\.poe2craft\.item\.ReviewedBelts\.BASES\.get\(\s*key\)/,'com.poe2craft.item.ReviewedBelts.BASES.getOrDefault(key, com.poe2craft.item.ReviewedCrossbows.BASES.get(key))'))
edit('backend/src/main/java/com/poe2craft/bootstrap/CraftingConfiguration.java',s=>s.replace(/com\.poe2craft\.item\.ReviewedBelts\.BASES\s*\.keySet\(\)\s*\.stream\(\)/,'java.util.stream.Stream.concat(com.poe2craft.item.ReviewedBelts.BASES.keySet().stream(), com.poe2craft.item.ReviewedCrossbows.BASES.keySet().stream())'))
edit('backend/src/main/java/com/poe2craft/crafting/application/WorkbenchService.java',s=>s.replace('ReviewedBelts.BASES.get(key)','ReviewedBelts.BASES.getOrDefault(key, ReviewedCrossbows.BASES.get(key))').replace('|| ReviewedBelts.supports(catalog.base().id()))','|| ReviewedBelts.supports(catalog.base().id())\n || ReviewedCrossbows.supports(catalog.base().id()))'))
edit('backend/src/main/java/com/poe2craft/crafting/domain/QualityLimitRules.java',s=>s.replace('&& !ReviewedBows.supports(state.baseItemId())','&& !ReviewedBows.supports(state.baseItemId())\n && !ReviewedCrossbows.supports(state.baseItemId())'))
edit('backend/src/main/java/com/poe2craft/crafting/domain/WorkbenchSimulator.java',s=>s.replace('private String baseRuleVersion() {','private String baseRuleVersion() {\n if (ReviewedCrossbows.supports(catalog.base().id())) return "crossbow-workbench-v1";').replace('public String ledgerVersion() {','public String ledgerVersion() {\n if (ReviewedCrossbows.supports(catalog.base().id())) return "crossbow-unverified-numeric-assumptions-v1";'))
for(const p of ['frontend/src/features/crafting/CraftingPage.tsx','frontend/src/features/crafting/craftingApi.ts','frontend/src/features/crafting/draft.ts'])edit(p,s=>{
  s=s.replace(/^(\s*)\| 'forking-belt'/gm,(_,i)=>`${i}| 'forking-belt'\n${additions.map(b=>`${i}| '${b.key}'`).join('\n')}`)
  if(p.endsWith('draft.ts'))s=s.replace(/(\s*'forking-belt': 'Item[^\n]+,)/,`$1\n${additions.map(b=>`  '${b.key}': 'Item Class: Crossbows\\nRarity: Normal\\n${b.name}',`).join('\n')}`)
  if(p.endsWith('CraftingPage.tsx')) {
    s=s.replace("  'forking-belt': 'Forking_Belt',",`  'forking-belt': 'Forking_Belt',\n${additions.map(b=>`  '${b.key}': '${b.slug}',`).join('\n')}`)
    s=s.replace(/(itemClass:\s*(?:Object.hasOwn\(workbenchJewelBases, catalogBase\)\s*\? 'Jewels'\s*:\s*)?)/g,'$1topBase(catalogBase)?.family === \'crossbows\' ? \'Crossbows\' : ')
  }
  return s
})
for(const p of ['frontend/src/features/crafting/topBases.ts','frontend/src/features/crafting/workbenchApi.ts'])edit(p,s=>{
  s=s.replace("      'belts',","      'belts',\n      'crossbows',")
  if(p.endsWith('workbenchApi.ts'))s=s.replace("topBase(reviewedKey ?? '')?.family !== 'bows')", "!['bows','crossbows'].includes(topBase(reviewedKey ?? '')?.family ?? ''))")
  return s
})
console.log('Wired separate Crossbow class through reviewed catalogs; existing socket model stays restricted to Stocky')
