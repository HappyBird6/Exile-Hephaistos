import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/crossbows-qa-20261005',previous='E:/WORK/Exile-Hephaistos/codex/belts-qa-20261005'
assert(!fs.existsSync(q),'Preserve previous QA output')
assert.equal(JSON.parse(fs.readFileSync(`${previous}/heavy-qa-owner.json`)).active,false,'Previous owner must have released QA')
fs.mkdirSync(q)
for(const name of ['backend','frontend'])fs.cpSync(name,`${q}/${name}-check`,{recursive:true,filter:p=>!p.split(/[\\/]/).some(s=>['node_modules','build','dist','.gradle','coverage'].includes(s))})
fs.cpSync(`${previous}/gradle-cache`,`${q}/gradle-cache`,{recursive:true})
fs.copyFileSync(`${previous}/api-attempt-3/api-initials.json`,`${q}/baseline-api-initials.json`)
fs.mkdirSync(`${q}/scripts`)
fs.copyFileSync('scripts/check-display-coverage.mjs',`${q}/scripts/check-display-coverage.mjs`)
fs.writeFileSync(`${q}/compose.yaml`,fs.readFileSync(`${previous}/compose.yaml`,'utf8').replaceAll('exile-belts-20261005','exile-crossbows-20261005').replaceAll('20380','20480').replaceAll('20381','20481'))
for(const name of ['backend','frontend'])fs.copyFileSync(`${previous}/${name}-check-1.sh`,`${q}/${name}-check-1.sh`)
fs.writeFileSync(`${q}/record-live.mjs`,fs.readFileSync('scripts/record-wands-live.mjs','utf8').replaceAll('wands-qa-20261005','crossbows-qa-20261005'))
fs.writeFileSync(`${q}/heavy-qa-owner.json`,JSON.stringify({active:true,project:'exile-crossbows-20261005',ports:[20480,20481],baseHead:'9fbb83da948d515756b6cea9ffb7a24155abba8e',authorization:'Sole writer; sequential exclusive QA'},null,2)+'\n')
console.log(q)
