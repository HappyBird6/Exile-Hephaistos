import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/maces-qa-20261005',previous='E:/WORK/Exile-Hephaistos/codex/quivers-qa-20261005'
assert(!fs.existsSync(q),'Preserve previous QA outputs')
assert.equal(JSON.parse(fs.readFileSync(`${previous}/heavy-qa-owner.json`)).active,false)
fs.mkdirSync(q)
fs.mkdirSync(`${q}/scripts`)
fs.copyFileSync(`${previous}/api-attempt-1/api-initials.json`,`${q}/baseline-api-initials.json`)
fs.cpSync(`${previous}/gradle-cache`,`${q}/gradle-cache`,{recursive:true})
fs.writeFileSync(`${q}/compose.yaml`,fs.readFileSync(`${previous}/compose.yaml`,'utf8').replaceAll('exile-quivers-20261005','exile-maces-20261005').replaceAll('20680','20780').replaceAll('20681','20781'),{flag:'wx'})
fs.copyFileSync(`${previous}/backend-check-1.sh`,`${q}/backend-check-1.sh`)
fs.copyFileSync(`${previous}/frontend-check-4.sh`,`${q}/frontend-check-1.sh`)
fs.writeFileSync(`${q}/record-live.mjs`,fs.readFileSync(`${previous}/record-live.mjs`,'utf8').replaceAll('quivers-qa-20261005','maces-qa-20261005'),{flag:'wx'})
fs.copyFileSync('scripts/check-display-coverage.mjs',`${q}/scripts/check-display-coverage.mjs`)
fs.writeFileSync(`${q}/heavy-qa-owner.json`,JSON.stringify({active:true,project:'exile-maces-20261005',ports:[20780,20781],baseHead:'3c40950098146be6b2b80fda3ec54c380f833927',authorization:'Sole writer; isolated sequential QA'},null,2)+'\n',{flag:'wx'})
console.log(q)
