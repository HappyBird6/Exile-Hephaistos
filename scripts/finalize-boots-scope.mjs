import fs from 'node:fs'
const path='backend/src/main/resources/crafting/registry-v2.json'
const r=JSON.parse(fs.readFileSync(path,'utf8'))
const keys=['tasalian','drakeskin','sekhema','blacksteel-boots','faithful','daggerfoot']
const blessed=r.entries.find(e=>e.id==='Omen_of_the_Blessed')
blessed.supportedBases=blessed.supportedBases.filter(k=>!keys.includes(k))
fs.writeFileSync(path,JSON.stringify(r,null,2)+'\n')
console.log('Boots have no implicit: preserve atomic Blessed refusal without claiming a new positive supported path')
