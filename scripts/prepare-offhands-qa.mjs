import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/offhands-qa-20261005',previous='E:/WORK/Exile-Hephaistos/codex/crossbows-qa-20261005'
assert(!fs.existsSync(q),'Preserve prior QA outputs')
assert.equal(JSON.parse(fs.readFileSync(`${previous}/heavy-qa-owner.json`)).active,false,'Prior exclusive QA owner must be released')
fs.mkdirSync(q)
fs.mkdirSync(`${q}/scripts`)
fs.copyFileSync(`${previous}/api-attempt-1/api-initials.json`,`${q}/baseline-api-initials.json`)
fs.cpSync(`${previous}/gradle-cache`,`${q}/gradle-cache`,{recursive:true})
fs.writeFileSync(`${q}/compose.yaml`,fs.readFileSync(`${previous}/compose.yaml`,'utf8').replaceAll('exile-crossbows-20261005','exile-offhands-20261005').replaceAll('20480','20580').replaceAll('20481','20581'),{flag:'wx'})
for(const name of ['backend','frontend'])fs.copyFileSync(`${previous}/${name}-check-1.sh`,`${q}/${name}-check-1.sh`)
fs.writeFileSync(`${q}/record-live.mjs`,fs.readFileSync('scripts/record-wands-live.mjs','utf8').replaceAll('wands-qa-20261005','offhands-qa-20261005'),{flag:'wx'})
fs.copyFileSync('scripts/check-display-coverage.mjs',`${q}/scripts/check-display-coverage.mjs`)
fs.writeFileSync(`${q}/heavy-qa-owner.json`,JSON.stringify({active:true,project:'exile-offhands-20261005',ports:[20580,20581],baseHead:'ba9df63a0fddc6c15967103ebb4e2636b7488cd7',authorization:'Sole writer; sequential exclusive QA'},null,2)+'\n',{flag:'wx'})
console.log(q)
