import fs from 'node:fs'
import assert from 'node:assert/strict'
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'))
const write=(p,v)=>fs.writeFileSync(p,JSON.stringify(v,null,2)+'\n',{flag:'wx'})
const bases=read('backend/src/main/resources/catalog/top-bases.json')
const registry=read('backend/src/main/resources/crafting/registry-v2.json')
const classes={body:'Body Armours',helmet:'Helmets',helmets:'Helmets',gloves:'Gloves',boots:'Boots',bows:'Bows',rings:'Rings',amulets:'Amulets',sceptres:'Sceptres',wands:'Wands',belts:'Belts',crossbows:'Crossbows',shields:'Shields',bucklers:'Bucklers',foci:'Foci',quivers:'Quivers','one-hand-maces':'One Hand Maces','two-hand-maces':'Two Hand Maces',quarterstaves:'Quarterstaves',spears:'Spears'}
const versions={gloves:['gloves-workbench-uniform-v1','gloves-uniform-candidates-unverified-rolls-v1'],helmets:['helmets-workbench-uniform-v1','helmets-uniform-candidates-unverified-rolls-v1'],body:['body-workbench-uniform-v1','body-uniform-candidates-unverified-rolls-v1'],boots:['boots-workbench-uniform-v1','boots-uniform-candidates-unverified-rolls-v1'],bows:['endgame-bows-workbench-v1','bow-unverified-numeric-assumptions-v1'],rings:['distinct-rings-workbench-v1','ring-unverified-numeric-assumptions-v1'],amulets:['distinct-amulets-workbench-v1','amulet-unverified-numeric-assumptions-v1'],sceptres:['sceptre-workbench-essence-v1','sceptre-unverified-numeric-assumptions-v1'],wands:['wand-workbench-essence-v1','wand-unverified-numeric-assumptions-v1'],belts:['distinct-belts-workbench-v1','belt-unverified-numeric-assumptions-v1'],crossbows:['crossbow-workbench-v1','crossbow-unverified-numeric-assumptions-v1'],quivers:['quiver-workbench-v1','quiver-unverified-numeric-assumptions-v1']}
for(const f of ['shields','bucklers','foci'])versions[f]=['offhand-workbench-v1','offhand-unverified-numeric-assumptions-v1']
for(const f of ['one-hand-maces','two-hand-maces'])versions[f]=['maces-workbench-v1','maces-unverified-numeric-assumptions-v1']
for(const f of ['quarterstaves','spears'])versions[f]=['quarterstaves-spears-workbench-v1','quarterstaves-spears-unverified-numeric-assumptions-v1']
versions.helmet=['helmet-workbench-perfect-essence-v1','helmet-unverified-numeric-assumptions-v1']
const families={}
for(const [family,itemClass]of Object.entries(classes)) {
 const [ruleVersion,ledgerVersion]=versions[family]
 families[family]={itemClass,ruleVersion,ledgerVersion,qualityLimit:!['belts','quivers'].includes(family),ordinaryCatalyst:['rings','amulets'].includes(family),refinedCatalyst:false,catalystQuality:['rings','amulets'].includes(family),maximumQualityBreach:['rings','amulets'].includes(family),initializeImplicit:!['body','helmet','helmets','gloves','boots','sceptres','wands'].includes(family),socketExecutionMaximum:null,divine:true,sourcePropertyUnscaled:['quarterstaves','spears','one-hand-maces','two-hand-maces','crossbows'].includes(family),snapshotDate:['shields','bucklers','foci','quivers','one-hand-maces','two-hand-maces'].includes(family)?'20261005':'20261004',snapshotRetrievedAt:['shields','bucklers','foci','quivers','one-hand-maces','two-hand-maces'].includes(family)?'SOURCE':'2026-10-04'}
}
const baseOverrides={soldier:{ruleVersion:'body-workbench-perfect-essence-v1',ledgerVersion:'body-unverified-numeric-assumptions-v1'}}
const legacy={}
const legacyKeys=['solar','stocky','bow','wand','body','sceptre','belt','helmet','ring','ruby','emerald','sapphire','diamond','time-lost-ruby','time-lost-emerald','time-lost-sapphire','time-lost-diamond']
const legacyIds={solar:'Metadata/Items/Amulets/FourAmulet9',stocky:'Metadata/Items/Armours/Gloves/FourGlovesStr1',bow:'Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow1',wand:'Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand3',body:'Metadata/Items/Armours/BodyArmours/FourBodyStr1',sceptre:'Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre1',belt:'Metadata/Items/Belts/FourBelt1',helmet:'Metadata/Items/Armours/Helmets/FourHelmetStr1',ring:'Metadata/Items/Rings/FourRing1',ruby:'Metadata/Items/Jewels/JewelStr',emerald:'Metadata/Items/Jewels/JewelDex',sapphire:'Metadata/Items/Jewels/JewelInt',diamond:'Metadata/Items/Jewels/JewelDiamond','time-lost-ruby':'Metadata/Items/Jewels/JewelRadiusStr','time-lost-emerald':'Metadata/Items/Jewels/JewelRadiusDex','time-lost-sapphire':'Metadata/Items/Jewels/JewelRadiusInt','time-lost-diamond':'Metadata/Items/Jewels/JewelRadiusDiamond'}
const draft=fs.readFileSync('frontend/src/features/crafting/draft.ts','utf8')
const draftObject=draft.slice(draft.indexOf('const baseTexts = {')+'const baseTexts = {'.length,draft.indexOf('\n}\ntype Draft'))
const textMap=Function('baseText',`return ({${draftObject}\n})`)('Item Class: Amulets\nRarity: Normal\nSolar Amulet')
const page=fs.readFileSync('frontend/src/features/crafting/CraftingPage.tsx','utf8')
const slugStart=page.indexOf('const baseSlugs = {')+'const baseSlugs = {'.length,slugEnd=page.indexOf('\n} as const',slugStart)
const slugs=Function(`return ({${page.slice(slugStart,slugEnd)}\n})`)()
for(const key of legacyKeys) {
 const jewel=['ruby','emerald','sapphire','diamond'].includes(key),timeLost=key.startsWith('time-lost-')
 legacy[key]={id:legacyIds[key],slug:slugs[key],initialText:textMap[key],qualityLimit:key!=='belt'&&!timeLost,ordinaryCatalyst:['solar','ring'].includes(key),refinedCatalyst:jewel,catalystQuality:['solar','ring'].includes(key)||jewel,maximumQualityBreach:key==='solar',socketExecutionMaximum:key==='stocky'?1:null,divine:key!=='belt'}
}
const policies={schemaVersion:1,families,baseOverrides,legacy}
write('backend/src/main/resources/catalog/base-policies.json',policies)
write('frontend/src/features/crafting/basePolicies.json',policies)
// Capture exact immutable data and duplicated registrations before changing code.
const proof={baseHead:'e80d63fca33b9702b192cf23970b03fb8891eed5',topBases:bases,essences:read('backend/src/main/resources/catalog/top-base-essences.json'),reviewed:{},slugs,textMap}
for(const name of fs.readdirSync('backend/src/main/java/com/poe2craft/item').filter(n=>/^Reviewed.*\.java$/.test(n))) {
 const source=fs.readFileSync(`backend/src/main/java/com/poe2craft/item/${name}`,'utf8')
 const pairs=[...source.matchAll(/"([a-z0-9-]+)"\s*,\s*"(Metadata\/[^"\n]+)"/g)]
 proof.reviewed[name]=Object.fromEntries(pairs.map(m=>[m[1],m[2]]))
 assert(pairs.length>0,name)
}
fs.mkdirSync('docs/evidence/base-registry-refactor-2026-10-05',{recursive:true})
write('docs/evidence/base-registry-refactor-2026-10-05/registration-baseline.json',proof)
console.log('policy families',Object.keys(families).length,'top bases',Object.keys(bases).length,'legacy',legacyKeys.length)
