import fs from 'node:fs'
const keys=['stellar','amber','bloodstone','lunar','azure','crimson','pearlescent']
let s=fs.readFileSync('backend/src/test/java/com/poe2craft/crafting/ReviewedRingsTest.java','utf8')
s=s.replaceAll('ReviewedRings','ReviewedAmulets').replace('ItemCatalogLoader.loadRing()','ItemCatalogLoader.loadDefault()').replace('hasSize(203)','hasSize(209)').replace('iron-ring:implicit:added-physical-damage-to-attacks','solar-amulet:implicit:spirit')
s=s.replace(/strings = \{[^]*?\}/,`strings = {${keys.map(k=>`"${k}"`).join(',')}}`)
s=s.replace(/Map.of\(\s*"attack_minimum_added_physical_damage",\s*1L,\s*"attack_maximum_added_physical_damage",\s*4L\)/,'Map.of("base_spirit",15L)')
s=s.replace('if (d.layer() == ModifierDefinition.Layer.EXPLICIT) assertThat(c.find(d.id())).contains(d);', `if (d.layer() == ModifierDefinition.Layer.EXPLICIT && d.weight()>0) assertThat(c.find(d.id())).contains(d);
    assertThat(c.modifiers().values().stream().filter(d -> d.layer() == ModifierDefinition.Layer.EXPLICIT && d.weight() == 0).map(ModifierDefinition::id)).containsExactlyInAnyOrder(
      "amulet:suffix:essence-percent-strength", "amulet:suffix:essence-percent-dexterity", "amulet:suffix:essence-percent-intelligence", "amulet:prefix:essence-global-defences", "amulet:prefix:essence-maximum-quality", "amulet:prefix:essence-abyssal-mark", "amulet:suffix:essence-abyssal-mark", "amulet:suffix:essence-hysteria-life-recoup");
    assertThat(c.find("amulet:prefix:alloy-maximum-runic-ward")).isEmpty();`)
fs.writeFileSync('backend/src/test/java/com/poe2craft/crafting/ReviewedAmuletsTest.java',s)
const p='backend/src/test/java/com/poe2craft/crafting/CatalystRegistryTest.java'
s=fs.readFileSync(p,'utf8').replace('    for (var base :','    ReviewedAmulets.BASES.keySet().forEach(key -> catalogs.put(key, ItemCatalogLoader.loadTopBase(key)));\n    for (var base :').replace('"two-stone-fire-cold");',`"two-stone-fire-cold",${keys.map(k=>`"${k}"`).join(',')});`)
fs.writeFileSync(p,s)
s=fs.readFileSync('frontend/src/features/crafting/ReviewedRings.test.ts','utf8')
s=s.replace(/const keys = \[[^]*?\] as const/,`const keys = [${keys.map(k=>`'${k}'`).join(',')}] as const`).replace(/const tags = \{[^]*?\n\}/,`const tags = {stellar:['attribute'],amber:['attribute'],bloodstone:['life'],lunar:['defences','energyshield'],azure:['mana'],crimson:['life'],pearlescent:['fire','cold','lightning','resistance']}`)
fs.writeFileSync('frontend/src/features/crafting/ReviewedAmulets.test.ts',s)
for(const [p,from,to] of [['frontend/src/features/crafting/localizedModifiers.test.ts','toHaveLength(1790)','toHaveLength(1798)'],['frontend/src/features/crafting/remainingDisplay.test.tsx','toHaveLength(218)','toHaveLength(225)']]){
 let t=fs.readFileSync(p,'utf8').replace(from,to)
 const anchor=p.includes('localized')?'    const jewelIds':'    // Preserve all 211'
 const assertion=`    expect(Object.keys(catalog.definitions).filter(id => /^(stellar|amber|bloodstone|lunar|azure|crimson|pearlescent):implicit:/.test(id)).sort()).toEqual(${JSON.stringify(keys.map(k=>JSON.parse(fs.readFileSync('frontend/src/features/crafting/topBases.json'))[k].implicitModifierId).sort())})\n`
 t=t.replace(anchor,assertion+anchor);fs.writeFileSync(p,t)
}
