import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/quarterstaves-spears-qa-20261005'
for(const [from,to]of [['backend','backend-check'],['frontend','frontend-check']]) {
 assert(!fs.existsSync(`${q}/${to}`),'Preserve prior QA copy')
 fs.cpSync(from,`${q}/${to}`,{recursive:true,filter:p=>!p.split(/[\\/]/).some(s=>['node_modules','dist','build','.gradle'].includes(s))})
}
for(const [from,to]of [['audit-maces-source.mjs','audit-quarterstaves-spears-source.mjs'],['verify-maces-preservation.mjs','verify-quarterstaves-spears-preservation.mjs'],['record-maces-final-inputs.mjs','record-quarterstaves-spears-final-inputs.mjs'],['record-maces-backend.mjs','record-quarterstaves-spears-backend.mjs']]) {
 let s=fs.readFileSync(`scripts/${from}`,'utf8').replaceAll('maces-source-bundle','quarterstaves-spears-source-bundle').replaceAll('maces-qa-20261005','quarterstaves-spears-qa-20261005').replaceAll('3c40950098146be6b2b80fda3ec54c380f833927','2fe3f86d101ef8f3f35636470a9e4789ae7c7edf')
 s=s.replaceAll("['One_Hand_Maces','Two_Hand_Maces']","['Quarterstaves','Spears']").replaceAll("page==='One_Hand_Maces'?12:17","page==='Quarterstaves'?58:79").replaceAll('assert.equal(data.normal.length,150)',"assert.equal(data.normal.length,page==='Quarterstaves'?158:162)").replaceAll('ordinary:150','ordinary:data.normal.length')
 s=s.replaceAll("['fortified-hammer','strife-pick','akoyan-club','ruination-maul','fanatic-greathammer','tawhoan-greatclub']","['aegis-quarterstaff','bolting-quarterstaff','dreaming-quarterstaff','grand-spear','flying-spear','akoyan-spear']").replaceAll('results.totalBases=119','results.totalBases=125')
 fs.writeFileSync(`scripts/${to}`,s,{flag:'wx'})
}
