import fs from 'node:fs'
import assert from 'node:assert/strict'
const read = p => fs.readFileSync(p, 'utf8')
const write = (p,s) => fs.writeFileSync(p,s)
const json = p => JSON.parse(read(p))
const save = (p,v) => write(p,JSON.stringify(v,null,2)+'\n')
const manifest = json('backend/src/main/resources/catalog/top-bases.json')
const bodyKeys = ['slipstrike','death-mail','sleek','vile','wolfskin']
const helmetKeys = ['ancestral','cryptic']
const keys = [...bodyKeys,...helmetKeys]
for (const k of keys) assert(manifest[k])
for (const p of ['frontend/src/features/crafting/CraftingPage.tsx','frontend/src/features/crafting/craftingApi.ts','frontend/src/features/crafting/draft.ts']) {
  let s = read(p)
  if (!s.includes("| 'slipstrike'")) s=s.replace(/^(\s*)\| 'adherent'/gm,(_,i)=>`${i}| 'adherent'\n${keys.map(k=>`${i}| '${k}'`).join('\n')}`)
  if (p.endsWith('draft.ts') && !s.includes("'slipstrike': 'Item")) s=s.replace("  adherent: 'Item Class: Gloves\\nRarity: Normal\\nAdherent Cuffs',", "  adherent: 'Item Class: Gloves\\nRarity: Normal\\nAdherent Cuffs',\n"+keys.map(k=>`  '${k}': 'Item Class: ${bodyKeys.includes(k)?'Body Armours':'Helmets'}\\nRarity: Normal\\n${manifest[k].name}',`).join('\n'))
  if (p.endsWith('CraftingPage.tsx')) {
    if (!s.includes("'slipstrike': 'Slipstrike_Vest'")) s=s.replace("  adherent: 'Adherent_Cuffs',", "  adherent: 'Adherent_Cuffs',\n"+keys.map(k=>`  '${k}': '${manifest[k].slug}',`).join('\n'))
    s=s.replaceAll("['gloves', 'helmets'].includes(","['gloves', 'helmets', 'body'].includes(")
    s=s.replace("topBase(catalogBase)?.family ?? '',\n              )", "topBase(catalogBase)?.family ?? '',\n              ) && catalogBase !== 'soldier'")
    s=s.replace('properties: [\n', "properties: [\n                ...(topBase(catalogBase)?.sourceProperties?.[locale] ?? []).map((text, i) => ({ id: 'source-property-' + i, text })),\n")
    s=s.replaceAll("catalogBase === 'body' || catalogBase === 'soldier'", "catalogBase === 'body' || catalogBase === 'soldier' || topBase(catalogBase)?.family === 'body'")
    s=s.replace(/catalogBase === 'body' \|\|\s+catalogBase === 'soldier'(\s*\? 'Body Armours')/g, "catalogBase === 'body' || catalogBase === 'soldier' || topBase(catalogBase)?.family === 'body'$1")
    // The source-only generic properties path already renders reviewed Body armour.
    s=s.replace("...(catalogBase === 'body' || catalogBase === 'soldier' || topBase(catalogBase)?.family === 'body'", "...(catalogBase === 'body' || catalogBase === 'soldier'")
  }
  write(p,s)
}
let p='frontend/src/features/crafting/topBases.ts'
write(p,read(p).replace('  energyShield?: number','  sourceProperties?: Record<\'en\' | \'ko\' | \'ja\' | \'zh-CN\' | \'zh-TW\' | \'es\', string[]>\n  baseMovementSpeed?: number\n  energyShield?: number').replace("(key) => ['gloves', 'helmets'].includes(","(key) => key !== 'soldier' && ['gloves', 'helmets', 'body'].includes("))
p='frontend/src/features/crafting/workbenchApi.ts'
write(p,read(p).replace("['gloves', 'helmets'].includes(","['gloves', 'helmets', 'body'].includes(").replace("state.baseItemId !== 'Metadata/Items/Armours/BodyArmours/FourBodyStr1')", "state.baseItemId !== 'Metadata/Items/Armours/BodyArmours/FourBodyStr1' &&\n      topBase(reviewedKey ?? '')?.family !== 'body')"))
const registryPath='backend/src/main/resources/crafting/registry-v2.json'
const registry=json(registryPath), overrides=json('backend/src/main/resources/catalog/top-base-essences.json')
for(const k of keys) {
  const b=manifest[k], body=bodyKeys.includes(k), donor=body?'soldier':'imperial'
  registry.workbenchBases[k]={...registry.workbenchBases[donor],ruleVersion:body?'body-workbench-uniform-v1':'helmets-workbench-uniform-v1',ledgerVersion:body?'body-uniform-candidates-unverified-rolls-v1':'helmets-uniform-candidates-unverified-rolls-v1',weightPolicy:'UNVERIFIED_GAME_WEIGHTS_EXPLICIT_UNIFORM_ELIGIBLE_CANDIDATES',baseItemId:b.id,source:b.sourceUrl,sourceSha256:b.sourceSha256,baseArmour:b.armour,baseEvasion:b.evasion,baseEnergyShield:b.energyShield,requiredCharacterLevel:b.requiredLevel,requiredStrength:b.strength,requiredDexterity:b.dexterity,requiredIntelligence:b.intelligence}
  const actions=new Set([...Object.keys(overrides[k].fixed),...Object.keys(overrides[k].replacements)])
  for(const e of registry.entries) {
    if(e.serviceScope==='DEFERRED') continue
    if(!e.supportedBases?.includes(donor) && !(e.category==='ESSENCE' && actions.has(e.action??e.workbenchAction))) continue
    if(e.category==='ESSENCE'&&!actions.has(e.action??e.workbenchAction))continue
    if(!e.supportedBases.includes(k))e.supportedBases.push(k)
  }
}
save(registryPath,registry)
p='backend/src/main/java/com/poe2craft/item/ReviewedHelmets.java'
write(p,read(p).replace('evasion Helmet archetypes','ordinary Helmet defence archetypes').replace('"grinning", "Metadata/Items/Armours/Helmets/FourHelmetDexInt6Endgame");','"grinning", "Metadata/Items/Armours/Helmets/FourHelmetDexInt6Endgame",\n'+helmetKeys.map(k=>`"${k}", "${manifest[k].id}"`).join(',\n')+');'))
write('backend/src/main/java/com/poe2craft/item/ReviewedBodies.java',`package com.poe2craft.item;\nimport java.util.Map;\n/** Source-reviewed highest-tier ordinary Body defence archetypes. */\npublic final class ReviewedBodies {\n public static final Map<String,String> BASES=Map.of(\n${bodyKeys.map(k=>`"${k}", "${manifest[k].id}"`).join(',\n')});\n private ReviewedBodies() {}\n public static boolean supports(String id) {return BASES.containsValue(id);}\n}\n`)
p='backend/src/main/java/com/poe2craft/item/infrastructure/ItemCatalogLoader.java'
write(p,read(p).replace('&& !com.poe2craft.item.ReviewedHelmets.BASES.containsKey(key))','&& !com.poe2craft.item.ReviewedHelmets.BASES.containsKey(key)\n && !com.poe2craft.item.ReviewedBodies.BASES.containsKey(key))').replace('key, com.poe2craft.item.ReviewedHelmets.BASES.get(key));','key, com.poe2craft.item.ReviewedHelmets.BASES.getOrDefault(key, com.poe2craft.item.ReviewedBodies.BASES.get(key)));'))
p='backend/src/main/java/com/poe2craft/crafting/application/WorkbenchService.java'
write(p,read(p).replace('ReviewedGloves.BASES.getOrDefault(key, ReviewedHelmets.BASES.get(key))','ReviewedGloves.BASES.getOrDefault(key, ReviewedHelmets.BASES.getOrDefault(key, ReviewedBodies.BASES.get(key)))'))
p='backend/src/main/java/com/poe2craft/bootstrap/CraftingConfiguration.java'
write(p,read(p).replace('com.poe2craft.item.ReviewedHelmets.BASES.keySet().stream())','java.util.stream.Stream.concat(com.poe2craft.item.ReviewedHelmets.BASES.keySet().stream(), com.poe2craft.item.ReviewedBodies.BASES.keySet().stream()))'))
p='backend/src/main/java/com/poe2craft/crafting/domain/BodyEssenceTargets.java'
write(p,read(p).replace('return id.equals(BASE_ID)','return com.poe2craft.item.ReviewedBodies.supports(id) || id.equals(BASE_ID)'))
p='backend/src/main/java/com/poe2craft/crafting/domain/WorkbenchSimulator.java'
let s=read(p).replace('private String baseRuleVersion() {','private String baseRuleVersion() {\n if (ReviewedBodies.supports(catalog.base().id())) return "body-workbench-uniform-v1";').replace('public String ledgerVersion() {','public String ledgerVersion() {\n if (ReviewedBodies.supports(catalog.base().id())) return "body-uniform-candidates-unverified-rolls-v1";')
s=s.replace('List<ModifierDefinition> candidates, List<Assumption> assumptions) {','List<ModifierDefinition> candidates, List<Assumption> assumptions) {\n if (ReviewedBodies.supports(catalog.base().id())) assumptions.add(new Assumption("body-uniform-candidates-v1", "eligible per-base Body ordinary modifier", candidates.size(), candidates.stream().map(ModifierDefinition::id).sorted().toList(), null, null, catalog.metadata().sourceUrl(), "USER-APPROVED SIMULATOR MODEL: equal 1/N after family, side and level restrictions. Actual game spawn weights and numeric roll probabilities remain unverified."));')
write(p,s)
