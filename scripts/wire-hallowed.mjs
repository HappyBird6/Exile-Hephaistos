import fs from 'node:fs'
import assert from 'node:assert/strict'
const edit = (p, f) => { const s = fs.readFileSync(p, 'utf8'), n = f(s); assert.notEqual(n, s, p); fs.writeFileSync(p, n) }
fs.writeFileSync('backend/src/main/java/com/poe2craft/item/ReviewedSceptres.java', `package com.poe2craft.item;
import java.util.Map;
/** Reviewed representative of the Skeletal Warrior family; innate skill is display-only. */
public final class ReviewedSceptres {
 public static final Map<String,String> BASES=Map.of("hallowed","Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre13");
 private ReviewedSceptres() {}
 public static boolean supports(String id) { return BASES.containsValue(id); }
}
`)
edit('backend/src/main/java/com/poe2craft/item/infrastructure/ItemCatalogLoader.java', s => s.replace('&& !com.poe2craft.item.ReviewedAmulets.BASES.containsKey(key))', '&& !com.poe2craft.item.ReviewedAmulets.BASES.containsKey(key)\n && !com.poe2craft.item.ReviewedSceptres.BASES.containsKey(key))').replace('com.poe2craft.item.ReviewedAmulets.BASES.get(key)', 'com.poe2craft.item.ReviewedAmulets.BASES.getOrDefault(key,com.poe2craft.item.ReviewedSceptres.BASES.get(key))'))
edit('backend/src/main/java/com/poe2craft/bootstrap/CraftingConfiguration.java', s => s.replace('com.poe2craft.item.ReviewedAmulets.BASES\n                                            .keySet()\n                                            .stream()', 'java.util.stream.Stream.concat(com.poe2craft.item.ReviewedAmulets.BASES.keySet().stream(),com.poe2craft.item.ReviewedSceptres.BASES.keySet().stream())'))
edit('backend/src/main/java/com/poe2craft/crafting/application/WorkbenchService.java', s => s.replace('ReviewedAmulets.BASES.get(key)', 'ReviewedAmulets.BASES.getOrDefault(key,ReviewedSceptres.BASES.get(key))'))
edit('backend/src/main/java/com/poe2craft/crafting/domain/WorkbenchSimulator.java', s => s.replaceAll('catalog.base().id().equals(SceptreEssenceTargets.BASE_ID)', '(catalog.base().id().equals(SceptreEssenceTargets.BASE_ID) || ReviewedSceptres.supports(catalog.base().id()))'))
edit('backend/src/main/java/com/poe2craft/crafting/domain/QualityLimitRules.java', s => s.replace('&& !ReviewedAmulets.supports(state.baseItemId())', '&& !ReviewedAmulets.supports(state.baseItemId())\n && !ReviewedSceptres.supports(state.baseItemId())'))
for (const p of ['frontend/src/features/crafting/CraftingPage.tsx', 'frontend/src/features/crafting/craftingApi.ts', 'frontend/src/features/crafting/draft.ts']) {
 edit(p, s => {
  s = s.replace(/^(\s*)\| 'obliterator'/gm, (_, i) => `${i}| 'obliterator'\n${i}| 'hallowed'`)
  if (p.endsWith('draft.ts')) s = s.replace(/(\s*obliterator: 'Item[^\n]+,)/, "$1\n  hallowed: 'Item Class: Sceptres\\nRarity: Normal\\nHallowed Sceptre',")
  if (p.endsWith('CraftingPage.tsx')) s = s.replace("  obliterator: 'Obliterator_Bow',", "  obliterator: 'Obliterator_Bow',\n  hallowed: 'Hallowed_Sceptre',").replaceAll("catalogBase === 'sceptre'", "(catalogBase === 'sceptre' || topBase(catalogBase)?.family === 'sceptres')")
  return s
 })
}
// Hallowed properties come from its six-language source, avoiding duplicated legacy properties.
edit('frontend/src/features/crafting/CraftingPage.tsx', s => s.replace("...((catalogBase === 'sceptre' || topBase(catalogBase)?.family === 'sceptres')\n                ? [", "...(catalogBase === 'sceptre'\n                ? ["))
for (const p of ['frontend/src/features/crafting/topBases.ts', 'frontend/src/features/crafting/workbenchApi.ts']) edit(p, s => s.replaceAll("'boots', 'bows', 'rings', 'amulets'", "'boots', 'bows', 'rings', 'amulets', 'sceptres'"))
edit('frontend/src/features/crafting/workbenchApi.ts', s => s.replace("'Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre1') ||", "'Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre1' && topBase(reviewedKey ?? '')?.family !== 'sceptres') ||"))
console.log('Wired Hallowed runtime and six-language card properties')
