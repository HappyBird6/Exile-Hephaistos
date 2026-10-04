import fs from 'node:fs'
const root='E:/WORK/Exile-Hephaistos/codex/amulets-qa-20261004',previous='E:/WORK/Exile-Hephaistos/codex/rings-qa-20261004'
const keys=['stellar','amber','bloodstone','lunar','azure','crimson','pearlescent']
const write=(p,s)=>{if(fs.existsSync(p))throw Error('Preserve prior probe');fs.writeFileSync(p,s)}
let s=fs.readFileSync(`${previous}/qa-api-3.mjs`,'utf8').replaceAll('19880','19980').replace("['kinetic','vitalic','mnemonic','pearl','amethyst','prismatic','ruby-ring','two-stone-fire-cold']",JSON.stringify(keys)).replaceAll('iron-ring:prefix:hale','amulet:prefix:hale').replaceAll('iron-ring:prefix:healthy','amulet:prefix:healthy')
s=s.replace("'RUNIC_ALLOY','REFINED_CATALYST_FLESH'","'RUNIC_ALLOY','REFINED_CATALYST_FLESH'")
write(`${root}/qa-api.mjs`,s)
s=fs.readFileSync(`${previous}/qa-material-paths-2.mjs`,'utf8').replaceAll('19880','19980').replace("['kinetic','vitalic','mnemonic','pearl','amethyst','prismatic','ruby-ring','two-stone-fire-cold']",JSON.stringify(keys)).replaceAll('iron-ring:suffix:of-the-mongoose','amulet:suffix:of-the-mongoose').replaceAll('iron-ring:prefix:hale','amulet:prefix:hale')
write(`${root}/qa-material-paths.mjs`,s)
s=fs.readFileSync(`${previous}/qa-browser-3.cjs`,'utf8').replaceAll('19881','19981').replaceAll("['kinetic', 'vitalic', 'mnemonic', 'pearl', 'amethyst', 'prismatic', 'ruby-ring', 'two-stone-fire-cold']",JSON.stringify(keys)).replaceAll("place('kinetic')","place('stellar')").replaceAll('54 base selector choices','61 base selector choices').replaceAll('count() === 54','count() === 61').replaceAll('actual card class Rings','actual card class Amulets').replaceAll("innerText()==='Rings'","innerText()==='Amulets'")
s=s.replaceAll('Lesser_Essence_of_Flames','Lesser_Essence_of_Insulation').replaceAll('Perfect_Essence_of_the_Mind','Perfect_Essence_of_Enhancement').replaceAll('Ring Perfect Mind','Amulet Perfect Enhancement').replaceAll('exact Ring class target','exact Amulet class target')
// Use a compound, source-unit modifier in locale captures alongside the implicit.
s=s.replace("d.familyIds.includes('IncreasedLife') && d.stats[0].min !== d.stats[0].max","d.stats.length > 1 && d.stats.some(s=>s.min!==s.max)")
write(`${root}/qa-browser.cjs`,s)
console.log('Prepared API/browser positive paths and historical54 film checks')
