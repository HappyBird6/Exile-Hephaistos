import fs from 'node:fs'
import assert from 'node:assert/strict'
const read=p=>fs.readFileSync(p,'utf8'),write=(p,s)=>fs.writeFileSync(p,s)
const manifest=JSON.parse(read('backend/src/main/resources/catalog/top-bases.json'))
const keys=Object.keys(manifest).filter(k=>manifest[k].family==='rings')
assert.equal(keys.length,8)
const edit=(p,f)=>{const s=read(p),n=f(s);assert.notEqual(n,s,p);write(p,n)}
write('backend/src/main/java/com/poe2craft/item/ReviewedRings.java',`package com.poe2craft.item;
import java.util.Map;
/** Distinct source-reviewed Ring implicits; this is not the complete Ring catalog. */
public final class ReviewedRings {
 public static final Map<String,String> BASES=Map.of(${keys.map(k=>`"${k}","${manifest[k].id}"`).join(',')});
 private ReviewedRings() {}
 public static boolean supports(String id) { return BASES.containsValue(id); }
 public static boolean reviewedImplicit(ModifierDefinition d) {
  return java.util.Set.of(${keys.map(k=>`"${manifest[k].implicitModifierId}"`).join(',')}).contains(d.id()) && d.layer()==ModifierDefinition.Layer.IMPLICIT;
 }
}
`)
edit('backend/src/main/java/com/poe2craft/item/infrastructure/ItemCatalogLoader.java',s=>s.replace('&& !com.poe2craft.item.ReviewedBows.BASES.containsKey(key))','&& !com.poe2craft.item.ReviewedBows.BASES.containsKey(key)\n && !com.poe2craft.item.ReviewedRings.BASES.containsKey(key))').replace('com.poe2craft.item.ReviewedBows.BASES.get(key)','com.poe2craft.item.ReviewedBows.BASES.getOrDefault(key,com.poe2craft.item.ReviewedRings.BASES.get(key))'))
edit('backend/src/main/java/com/poe2craft/bootstrap/CraftingConfiguration.java',s=>s.replace('com.poe2craft.item.ReviewedBows.BASES.keySet().stream()','java.util.stream.Stream.concat(com.poe2craft.item.ReviewedBows.BASES.keySet().stream(),com.poe2craft.item.ReviewedRings.BASES.keySet().stream())'))
edit('backend/src/main/java/com/poe2craft/crafting/application/WorkbenchService.java',s=>s.replace('ReviewedBows.BASES.get(key)','ReviewedBows.BASES.getOrDefault(key,ReviewedRings.BASES.get(key))').replace('ReviewedBows.supports(catalog.base().id()) && catalog.base().hasImplicit()','(ReviewedBows.supports(catalog.base().id()) || ReviewedRings.supports(catalog.base().id())) && catalog.base().hasImplicit()'))
edit('backend/src/main/java/com/poe2craft/item/CatalystQuality.java',s=>s.replace('|| BasicJewel.supported(base);','|| ReviewedRings.supports(base)\n || BasicJewel.supported(base);'))
edit('backend/src/main/java/com/poe2craft/item/CatalystQualityDisplay.java',s=>s.replace('        ironImplicit\n','        ironImplicit\n || ReviewedRings.reviewedImplicit(d)\n'))
edit('backend/src/main/java/com/poe2craft/crafting/domain/QualityLimitRules.java',s=>s.replace('&& !ReviewedBows.supports(state.baseItemId())','&& !ReviewedBows.supports(state.baseItemId())\n && !ReviewedRings.supports(state.baseItemId())').replace('if (!state.baseItemId().equals(SolarAmulet.BASE_ID)\n          || !instance.modifierId()', 'if ((!state.baseItemId().equals(SolarAmulet.BASE_ID) && !ReviewedRings.supports(state.baseItemId()))\n          || !instance.modifierId()'))
edit('backend/src/main/java/com/poe2craft/crafting/domain/WorkbenchSimulator.java',s=>s.replace('private String baseRuleVersion() {','private String baseRuleVersion() {\n if (ReviewedRings.supports(catalog.base().id())) return "distinct-rings-workbench-v1";').replace('public String ledgerVersion() {','public String ledgerVersion() {\n if (ReviewedRings.supports(catalog.base().id())) return "ring-unverified-numeric-assumptions-v1";').replace('&& !state.baseItemId().equals(RingEssenceTargets.BASE_ID))','&& !state.baseItemId().equals(RingEssenceTargets.BASE_ID)\n && !ReviewedRings.supports(state.baseItemId()))'))
for(const p of ['frontend/src/features/crafting/CraftingPage.tsx','frontend/src/features/crafting/craftingApi.ts','frontend/src/features/crafting/draft.ts']){
 edit(p,s=>{
  s=s.replace(/^(\s*)\| 'obliterator'/gm,(_,i)=>`${i}| 'obliterator'\n${keys.map(k=>`${i}| '${k}'`).join('\n')}`)
  if(p.endsWith('draft.ts'))s=s.replace(/(\s*obliterator: 'Item[^\n]+,)/,`$1\n${keys.map(k=>`  '${k}': 'Item Class: Rings\\nRarity: Normal\\n${manifest[k].name}',`).join('\n')}`)
  if(p.endsWith('CraftingPage.tsx'))s=s.replace("  obliterator: 'Obliterator_Bow',",`  obliterator: 'Obliterator_Bow',\n${keys.map(k=>`  '${k}': '${manifest[k].slug}',`).join('\n')}`)
  return s
 })
}
edit('frontend/src/features/crafting/topBases.ts',s=>s.replace("'boots', 'bows'","'boots', 'bows', 'rings'"))
edit('frontend/src/features/crafting/workbenchApi.ts',s=>s.replaceAll("'boots', 'bows'","'boots', 'bows', 'rings'").replace("state.baseItemId !== 'Metadata/Items/Rings/FourRing1')","state.baseItemId !== 'Metadata/Items/Rings/FourRing1' && topBase(reviewedKey ?? '')?.family !== 'rings')"))
edit('frontend/src/features/crafting/catalystQuality.ts',s=>"import { topBase, topBaseKey } from './topBases'\n"+s.replace('  ].includes(base)','  ].includes(base) || topBase(topBaseKey(base) ?? \'\')?.family === \'rings\'').replace('    ironImplicit ||','    ironImplicit ||\n    (d.layer === \'IMPLICIT\' && Object.values(ringImplicitIds).includes(d.id)) ||'))
// Bind reviewed implicit IDs to the same source manifest used by initialization, without a prefix wildcard.
edit('frontend/src/features/crafting/catalystQuality.ts',s=>"import ringManifest from './topBases.json'\nconst ringImplicitIds = Object.fromEntries(Object.entries(ringManifest).filter(([,b]) => b.family === 'rings').map(([k,b]) => [k, 'implicitModifierId' in b ? b.implicitModifierId : '']))\n"+s)
edit('frontend/src/features/crafting/qualityLimit.ts',s=>s.replace("import { topBaseKey }", "import { topBase, topBaseKey }").replace('state.baseItemId !== solar ||',"(state.baseItemId !== solar && topBase(topBaseKey(state.baseItemId) ?? '')?.family !== 'rings') ||"))
edit('frontend/src/features/crafting/catalystQuality.ts',s=>s.replace("(state.baseItemId === 'Metadata/Items/Amulets/FourAmulet9' ? 40 : cap)","(state.baseItemId === 'Metadata/Items/Amulets/FourAmulet9' || topBase(topBaseKey(state.baseItemId) ?? '')?.family === 'rings' ? 40 : cap)"))
console.log('Wired eight distinct Ring bases, reviewed Catalyst projections and sourced Breach cap policy')
