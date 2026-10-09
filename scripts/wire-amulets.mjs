import fs from 'node:fs'
import assert from 'node:assert/strict'
const read=p=>fs.readFileSync(p,'utf8'),write=(p,s)=>fs.writeFileSync(p,s)
const manifest=JSON.parse(read('backend/src/main/resources/catalog/top-bases.json'))
const keys=Object.keys(manifest).filter(k=>manifest[k].family==='amulets')
assert.equal(keys.length,7)
const edit=(p,f)=>{const s=read(p),n=f(s);assert.notEqual(n,s,p);write(p,n)}
write('backend/src/main/java/com/poe2craft/item/ReviewedAmulets.java',`package com.poe2craft.item;
import java.util.Map;
/** Distinct source-reviewed Amulet implicits; not the complete Amulet catalog. */
public final class ReviewedAmulets {
 public static final Map<String,String> BASES=Map.of(${keys.map(k=>`"${k}","${manifest[k].id}"`).join(',')});
 private ReviewedAmulets() {}
 public static boolean supports(String id) { return BASES.containsValue(id); }
 public static boolean reviewedImplicit(ModifierDefinition d) {
  return java.util.Set.of(${keys.map(k=>`"${manifest[k].implicitModifierId}"`).join(',')}).contains(d.id()) && d.layer()==ModifierDefinition.Layer.IMPLICIT;
 }
}
`)
edit('backend/src/main/java/com/poe2craft/item/infrastructure/ItemCatalogLoader.java',s=>s.replace('&& !com.poe2craft.item.ReviewedRings.BASES.containsKey(key))','&& !com.poe2craft.item.ReviewedRings.BASES.containsKey(key)\n && !com.poe2craft.item.ReviewedAmulets.BASES.containsKey(key))').replace('com.poe2craft.item.ReviewedRings.BASES.get(key)','com.poe2craft.item.ReviewedRings.BASES.getOrDefault(key,com.poe2craft.item.ReviewedAmulets.BASES.get(key))'))
edit('backend/src/main/java/com/poe2craft/bootstrap/CraftingConfiguration.java',s=>s.replace('com.poe2craft.item.ReviewedRings.BASES.keySet().stream()','java.util.stream.Stream.concat(com.poe2craft.item.ReviewedRings.BASES.keySet().stream(),com.poe2craft.item.ReviewedAmulets.BASES.keySet().stream())'))
edit('backend/src/main/java/com/poe2craft/crafting/application/WorkbenchService.java',s=>s.replace('ReviewedRings.BASES.get(key)','ReviewedRings.BASES.getOrDefault(key,ReviewedAmulets.BASES.get(key))').replace('|| ReviewedRings.supports(catalog.base().id()))','|| ReviewedRings.supports(catalog.base().id()) || ReviewedAmulets.supports(catalog.base().id()))'))
edit('backend/src/main/java/com/poe2craft/item/CatalystQuality.java',s=>s.replace('|| ReviewedRings.supports(base)','|| ReviewedRings.supports(base)\n || ReviewedAmulets.supports(base)'))
edit('backend/src/main/java/com/poe2craft/item/CatalystQualityDisplay.java',s=>s.replace('|| ReviewedRings.reviewedImplicit(d)','|| ReviewedRings.reviewedImplicit(d)\n || ReviewedAmulets.reviewedImplicit(d)'))
edit('backend/src/main/java/com/poe2craft/crafting/domain/QualityLimitRules.java',s=>s.replaceAll('&& !ReviewedRings.supports(state.baseItemId())','&& !ReviewedRings.supports(state.baseItemId())\n && !ReviewedAmulets.supports(state.baseItemId())'))
edit('backend/src/main/java/com/poe2craft/crafting/domain/WorkbenchSimulator.java',s=>s.replace('private String baseRuleVersion() {','private String baseRuleVersion() {\n if (ReviewedAmulets.supports(catalog.base().id())) return "distinct-amulets-workbench-v1";').replace('public String ledgerVersion() {','public String ledgerVersion() {\n if (ReviewedAmulets.supports(catalog.base().id())) return "amulet-unverified-numeric-assumptions-v1";').replace('&& !ReviewedRings.supports(state.baseItemId()))','&& !ReviewedRings.supports(state.baseItemId())\n && !ReviewedAmulets.supports(state.baseItemId()))'))
for(const p of ['frontend/src/features/crafting/CraftingPage.tsx','frontend/src/features/crafting/craftingApi.ts','frontend/src/features/crafting/draft.ts']){
 edit(p,s=>{
  s=s.replace(/^(\s*)\| 'obliterator'/gm,(_,i)=>`${i}| 'obliterator'\n${keys.map(k=>`${i}| '${k}'`).join('\n')}`)
  if(p.endsWith('draft.ts'))s=s.replace(/(\s*obliterator: 'Item[^\n]+,)/,`$1\n${keys.map(k=>`  '${k}': 'Item Class: Amulets\\nRarity: Normal\\n${manifest[k].name}',`).join('\n')}`)
  if(p.endsWith('CraftingPage.tsx'))s=s.replace("  obliterator: 'Obliterator_Bow',",`  obliterator: 'Obliterator_Bow',\n${keys.map(k=>`  '${k}': '${manifest[k].slug}',`).join('\n')}`)
  if(p.endsWith('CraftingPage.tsx'))s=s.replaceAll(": 'Amulet',",": topBase(catalogBase)?.family === 'amulets' ? 'Amulets' : 'Amulet',")
  return s
 })
}
for(const p of ['frontend/src/features/crafting/topBases.ts','frontend/src/features/crafting/workbenchApi.ts'])edit(p,s=>s.replaceAll("'boots', 'bows', 'rings'","'boots', 'bows', 'rings', 'amulets'").replace("topBase(reviewedKey ?? '')?.family !== 'rings'","!['rings','amulets'].includes(topBase(reviewedKey ?? '')?.family ?? '')"))
edit('frontend/src/features/crafting/catalystQuality.ts',s=>s.replace("b.family === 'rings'","['rings','amulets'].includes(b.family)").replaceAll("topBase(topBaseKey(base) ?? '')?.family === 'rings'","['rings','amulets'].includes(topBase(topBaseKey(base) ?? '')?.family ?? '')").replaceAll("topBase(topBaseKey(state.baseItemId) ?? '')?.family === 'rings'","['rings','amulets'].includes(topBase(topBaseKey(state.baseItemId) ?? '')?.family ?? '')"))
edit('frontend/src/features/crafting/qualityLimit.ts',s=>s.replace("topBase(topBaseKey(state.baseItemId) ?? '')?.family !== 'rings'","!['rings','amulets'].includes(topBase(topBaseKey(state.baseItemId) ?? '')?.family ?? '')"))
console.log('Wired seven verified Amulet identities')
