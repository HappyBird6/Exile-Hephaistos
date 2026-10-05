import fs from 'node:fs'
import assert from 'node:assert/strict'
let s=fs.readFileSync('scripts/import-maces-bundle.mjs','utf8')
s=s.replaceAll('maces-source-bundle','quarterstaves-spears-source-bundle').replaceAll("['One_Hand_Maces','Two_Hand_Maces']","['Quarterstaves','Spears']")
s=s.replace("page === 'One_Hand_Maces'","page === 'Spears'")
s=s.replace("oneHand ? 'one-hand-maces' : 'two-hand-maces'","oneHand ? 'spears' : 'quarterstaves'")
s=s.replace("['Fortified Hammer','Strife Pick','Akoyan Club'] : ['Ruination Maul','Fanatic Greathammer','Tawhoan Greatclub']","['Grand Spear','Flying Spear','Akoyan Spear'] : ['Aegis Quarterstaff','Bolting Quarterstaff','Dreaming Quarterstaff']")
s=s.replace("const tags=['mace',","const tags=[oneHand?'spear':'warstaff',")
s=s.replace("assert.equal(proof.length,1)","assert.equal(proof.length, key==='dreaming-quarterstaff'?0:key==='grand-spear'||key==='flying-spear'?2:1)")
s=s.replace("const template = `maces.${d.id}`","const template = `quarterstaves-spears.${d.id}`")
s=s.replace("const craft=implicitBlock.match", "const craft=implicitBlock.match")
s=s.replace("strength:+b.requirements.match(/(\\d+) Str/)[1],dexterity:0", "strength:+(b.requirements.match(/(\\d+) Str/)?.[1]??0),dexterity:+(b.requirements.match(/(\\d+) Dex/)?.[1]??0),intelligence:+(b.requirements.match(/(\\d+) Int/)?.[1]??0)")
s=s.replace("...registry.workbenchBases.bow,ruleVersion:'maces-workbench-v1',ledgerVersion:'maces-unverified-numeric-assumptions-v1'","numericModelStatus:'UNVERIFIED',ruleVersion:'quarterstaves-spears-workbench-v1',ledgerVersion:'quarterstaves-spears-unverified-numeric-assumptions-v1'")
s=s.replace("projection:'EXPLICIT_AFFIX_WITH_SOURCE_MACE_IMPLICIT'","projection:'EXPLICIT_AFFIX_WITH_SOURCE_QUARTERSTAFF_SPEAR_IMPLICIT'")
s=s.replace("requiredDexterity:0","requiredDexterity:bases[key].dexterity,requiredIntelligence:bases[key].intelligence")
// Generic rarity/affix actions have no weapon-class predicate. Essence targets are class-source-derived.
// Blessed/Omen of the Blessed require a variable source implicit; sockets/Catalysts remain restricted.
s=s.replace("if(e.category==='ESSENCE'?!fixed[a]&&!replacements[a]:!e.supportedBases?.includes('bow'))continue", "if(e.category==='ESSENCE'?!fixed[a]&&!replacements[a]:!['CURRENCY','OMEN'].includes(e.category)||!Array.isArray(e.supportedBases))continue\n    if(a==='BLESSED'&&!proof.some(p=>p.stats.some(s=>s.min!==s.max)))continue")
assert(!fs.existsSync('scripts/import-quarterstaves-spears-bundle.mjs'))
fs.writeFileSync('scripts/import-quarterstaves-spears-bundle.mjs',s,{flag:'wx'})
