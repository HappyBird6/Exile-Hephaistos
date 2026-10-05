import fs from 'node:fs'
import assert from 'node:assert/strict'
const names=['Aegis Quarterstaff','Bolting Quarterstaff','Dreaming Quarterstaff','Grand Spear','Flying Spear','Akoyan Spear']
const keys=names.map(n=>n.toLowerCase().replaceAll(' ','-'))
const bases=JSON.parse(fs.readFileSync('frontend/src/features/crafting/topBases.json','utf8'))
const edit=(p,f)=>{const before=fs.readFileSync(p,'utf8'),after=f(before);assert.notEqual(before,after,p);fs.writeFileSync(p,after)}
fs.writeFileSync('backend/src/main/java/com/poe2craft/item/ReviewedQuarterstavesSpears.java',`package com.poe2craft.item;
import java.util.Map;
/** Ordinary endgame Quarterstaves and Spears with source-specific pools; combat effects are display-only. */
public final class ReviewedQuarterstavesSpears {
 public static final Map<String,String> BASES=Map.ofEntries(${keys.map(k=>`Map.entry("${k}","${bases[k].id}")`).join(',\n')});
 private ReviewedQuarterstavesSpears() {}
 public static boolean supports(String id) { return BASES.containsValue(id); }
}
`,{flag:'wx'})
edit('backend/src/main/java/com/poe2craft/item/infrastructure/ItemCatalogLoader.java',s=>s.replace('&& !com.poe2craft.item.ReviewedMaces.BASES.containsKey(key))','&& !com.poe2craft.item.ReviewedMaces.BASES.containsKey(key)\n && !com.poe2craft.item.ReviewedQuarterstavesSpears.BASES.containsKey(key))').replace('name = source.get("name").asText();','if (com.poe2craft.item.ReviewedQuarterstavesSpears.BASES.containsKey(key)) id=com.poe2craft.item.ReviewedQuarterstavesSpears.BASES.get(key);\n name = source.get("name").asText();'))
edit('backend/src/main/java/com/poe2craft/bootstrap/CraftingConfiguration.java',s=>s.replace('com.poe2craft.item.ReviewedMaces.BASES.keySet()', 'java.util.stream.Stream.concat(com.poe2craft.item.ReviewedMaces.BASES.keySet().stream(),com.poe2craft.item.ReviewedQuarterstavesSpears.BASES.keySet().stream()).toList()'))
edit('backend/src/main/java/com/poe2craft/crafting/application/WorkbenchService.java',s=>s.replace('|| ReviewedMaces.supports(catalog.base().id()))','|| ReviewedMaces.supports(catalog.base().id())\n || ReviewedQuarterstavesSpears.supports(catalog.base().id()))').replace('ReviewedMaces.BASES.getOrDefault(key, "")','ReviewedMaces.BASES.getOrDefault(key, ReviewedQuarterstavesSpears.BASES.getOrDefault(key, ""))'))
edit('backend/src/main/java/com/poe2craft/crafting/domain/QualityLimitRules.java',s=>s.replace('&& !ReviewedMaces.supports(state.baseItemId())','&& !ReviewedMaces.supports(state.baseItemId())\n && !ReviewedQuarterstavesSpears.supports(state.baseItemId())'))
edit('backend/src/main/java/com/poe2craft/crafting/domain/WorkbenchSimulator.java',s=>s.replace('private String baseRuleVersion() {','private String baseRuleVersion() {\n if (ReviewedQuarterstavesSpears.supports(catalog.base().id())) return "quarterstaves-spears-workbench-v1";').replace('public String ledgerVersion() {','public String ledgerVersion() {\n if (ReviewedQuarterstavesSpears.supports(catalog.base().id())) return "quarterstaves-spears-unverified-numeric-assumptions-v1";'))
for(const p of ['frontend/src/features/crafting/draft.ts','frontend/src/features/crafting/craftingApi.ts','frontend/src/features/crafting/CraftingPage.tsx'])edit(p,s=>{
 s=s.replace(/^(\s*)\| 'tawhoan-greatclub'/gm,(_,i)=>`${i}| 'tawhoan-greatclub'\n`+keys.map(k=>`${i}| '${k}'`).join('\n'))
 if(p.endsWith('draft.ts'))s=s.replace('const baseTexts = {','const baseTexts = {\n'+names.map((n,i)=>` '${keys[i]}': 'Item Class: ${i<3?'Quarterstaves':'Spears'}\\nRarity: Normal\\n${n}',`).join('\n'))
 if(p.endsWith('CraftingPage.tsx')) {
 s=s.replace('const baseSlugs = {','const baseSlugs = {\n'+names.map((n,i)=>` '${keys[i]}': '${n.replaceAll(' ','_')}',`).join('\n'))
 s=s.replace("topBase(catalogBase)?.family === 'quivers'","topBase(catalogBase)?.family === 'quarterstaves' ? 'Quarterstaves' : topBase(catalogBase)?.family === 'spears' ? 'Spears' : topBase(catalogBase)?.family === 'quivers'")
 s=s.replace(/(id: 'source-property-' \+ i,\s*text:\s*\[\s*)'one-hand-maces'/,"$1'quarterstaves',\n 'spears',\n 'one-hand-maces'")
 }
 return s
})
for(const p of ['frontend/src/features/crafting/topBases.ts','frontend/src/features/crafting/workbenchApi.ts'])edit(p,s=>s.replace("      'one-hand-maces',","      'quarterstaves',\n      'spears',\n      'one-hand-maces',"))
