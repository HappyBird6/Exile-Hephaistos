import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/quarterstaves-spears-qa-20261005',previous='E:/WORK/Exile-Hephaistos/codex/maces-qa-20261005'
assert(!fs.existsSync(q),'Preserve prior outputs')
assert.equal(JSON.parse(fs.readFileSync(`${previous}/heavy-qa-owner.json`)).active,false)
fs.mkdirSync(q)
fs.mkdirSync(`${q}/scripts`)
fs.cpSync(`${previous}/gradle-cache`,`${q}/gradle-cache`,{recursive:true})
fs.writeFileSync(`${q}/compose.yaml`,fs.readFileSync(`${previous}/compose.yaml`,'utf8').replaceAll('exile-maces-20261005','exile-quarterstaves-spears-20261005').replaceAll('20780','20880').replaceAll('20781','20881'),{flag:'wx'})
fs.copyFileSync(`${previous}/backend-check-1.sh`,`${q}/backend-check-1.sh`)
fs.writeFileSync(`${q}/frontend-check-1.sh`,fs.readFileSync(`${previous}/frontend-check-3.sh`,'utf8').replaceAll('frontend-check-3.log','frontend-check-1.log'),{flag:'wx'})
fs.writeFileSync(`${q}/record-live.mjs`,fs.readFileSync(`${previous}/record-live.mjs`,'utf8').replaceAll('maces-qa-20261005','quarterstaves-spears-qa-20261005'),{flag:'wx'})
fs.copyFileSync('scripts/check-display-coverage.mjs',`${q}/scripts/check-display-coverage.mjs`)
fs.copyFileSync(`${previous}/baseline-api-initials.json`,`${q}/baseline-api-initials.json`)
fs.writeFileSync(`${q}/heavy-qa-owner.json`,JSON.stringify({active:true,project:'exile-quarterstaves-spears-20261005',ports:[20880,20881],baseHead:'2fe3f86d101ef8f3f35636470a9e4789ae7c7edf',authorization:'Sole writer; isolated sequential QA'},null,2)+'\n',{flag:'wx'})
console.log(q)
