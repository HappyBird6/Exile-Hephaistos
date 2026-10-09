import fs from 'node:fs'
import assert from 'node:assert/strict'
const bases=JSON.parse(fs.readFileSync('frontend/src/features/crafting/topBases.json','utf8'))
const additions=Object.values(bases).filter(b=>b.family==='quivers')
assert.equal(additions.length,11)
const edit=(p,f)=>{const s=fs.readFileSync(p,'utf8'),n=f(s);assert.notEqual(s,n,p);fs.writeFileSync(p,n)}
fs.writeFileSync('backend/src/main/java/com/poe2craft/item/ReviewedQuivers.java',`package com.poe2craft.item;
import java.util.Map;
/** Ordinary distinct-implicit Quiver representatives; effects are display-only. */
public final class ReviewedQuivers {
 public static final Map<String,String> BASES=Map.ofEntries(${additions.map(b=>`Map.entry("${b.key}","${b.id}")`).join(',\n')});
 private ReviewedQuivers() {}
 public static boolean supports(String id) { return BASES.containsValue(id); }
}
`,{flag:'wx'})
edit('backend/src/main/java/com/poe2craft/item/infrastructure/ItemCatalogLoader.java',s=>s.replace('&& !com.poe2craft.item.ReviewedOffhands.BASES.containsKey(key))','&& !com.poe2craft.item.ReviewedOffhands.BASES.containsKey(key)\n && !com.poe2craft.item.ReviewedQuivers.BASES.containsKey(key))').replace(/\.ReviewedOffhands\.BASES\s*\.get\(key\)/,'.ReviewedOffhands.BASES.getOrDefault(key, com.poe2craft.item.ReviewedQuivers.BASES.get(key))'))
edit('backend/src/main/java/com/poe2craft/bootstrap/CraftingConfiguration.java',s=>s.replace(/com\s*\.poe2craft\s*\.item\s*\.ReviewedOffhands\s*\.BASES\s*\.keySet\(\)\s*\.stream\(\)/,'java.util.stream.Stream.concat(com.poe2craft.item.ReviewedOffhands.BASES.keySet().stream(), com.poe2craft.item.ReviewedQuivers.BASES.keySet().stream())'))
edit('backend/src/main/java/com/poe2craft/crafting/application/WorkbenchService.java',s=>s.replace(/ReviewedOffhands\.BASES\.get\(\s*key\)/,'ReviewedOffhands.BASES.getOrDefault(key, ReviewedQuivers.BASES.get(key))').replace('|| ReviewedOffhands.supports(catalog.base().id()))','|| ReviewedOffhands.supports(catalog.base().id())\n || ReviewedQuivers.supports(catalog.base().id()))'))
edit('backend/src/main/java/com/poe2craft/crafting/domain/WorkbenchSimulator.java',s=>s.replace('private String baseRuleVersion() {','private String baseRuleVersion() {\n if (ReviewedQuivers.supports(catalog.base().id())) return "quiver-workbench-v1";').replace('public String ledgerVersion() {','public String ledgerVersion() {\n if (ReviewedQuivers.supports(catalog.base().id())) return "quiver-unverified-numeric-assumptions-v1";'))
for(const p of ['frontend/src/features/crafting/CraftingPage.tsx','frontend/src/features/crafting/craftingApi.ts','frontend/src/features/crafting/draft.ts'])edit(p,s=>{
  s=s.replace(/^(\s*)\| 'tasalian-focus'/gm,(_,i)=>`${i}| 'tasalian-focus'\n${additions.map(b=>`${i}| '${b.key}'`).join('\n')}`)
  if(p.endsWith('draft.ts'))s=s.replace(/(\s*'tasalian-focus': 'Item[^\n]+,)/,`$1\n${additions.map(b=>`  '${b.key}': 'Item Class: Quivers\\nRarity: Normal\\n${b.name}',`).join('\n')}`)
  if(p.endsWith('CraftingPage.tsx')) {
    s=s.replace("  'tasalian-focus': 'Tasalian_Focus',",`  'tasalian-focus': 'Tasalian_Focus',\n${additions.map(b=>`  '${b.key}': '${b.slug}',`).join('\n')}`)
    s=s.replace(/(itemClass:\s*)/g,"$1topBase(catalogBase)?.family === 'quivers' ? 'Quivers' : ")
  }
  return s
})
for(const p of ['frontend/src/features/crafting/topBases.ts','frontend/src/features/crafting/workbenchApi.ts'])edit(p,s=>s.replace("      'foci',","      'foci',\n      'quivers',").replace('maximumQuality: number','maximumQuality: number | null'))
edit('frontend/src/features/crafting/qualityLimit.ts',s=>s.replace("topBase(topBaseKey(state.baseItemId) ?? '')?.family === 'belts'","['belts','quivers'].includes(topBase(topBaseKey(state.baseItemId) ?? '')?.family ?? '')"))
console.log('Wired independent Quiver class; quality and socket state remain unsupported')
