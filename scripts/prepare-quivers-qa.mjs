import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/quivers-qa-20261005',previous='E:/WORK/Exile-Hephaistos/codex/offhands-qa-20261005'
assert(!fs.existsSync(q),'Preserve prior QA outputs')
assert.equal(JSON.parse(fs.readFileSync(`${previous}/heavy-qa-owner.json`)).active,false,'Previous exclusive QA must be released')
fs.mkdirSync(q)
fs.mkdirSync(`${q}/scripts`)
fs.copyFileSync(`${previous}/api-attempt-1/api-initials.json`,`${q}/baseline-api-initials.json`)
fs.cpSync(`${previous}/gradle-cache`,`${q}/gradle-cache`,{recursive:true})
fs.writeFileSync(`${q}/compose.yaml`,fs.readFileSync(`${previous}/compose.yaml`,'utf8').replaceAll('exile-offhands-20261005','exile-quivers-20261005').replaceAll('20580','20680').replaceAll('20581','20681'),{flag:'wx'})
fs.writeFileSync(`${q}/backend-check-1.sh`,fs.readFileSync(`${previous}/backend-check-2.sh`,'utf8').replaceAll('backend-check-2','backend-check-1'),{flag:'wx'})
fs.writeFileSync(`${q}/frontend-check-1.sh`,fs.readFileSync(`${previous}/frontend-check-4.sh`,'utf8').replaceAll('frontend-check-4','frontend-check-1'),{flag:'wx'})
fs.writeFileSync(`${q}/record-live.mjs`,fs.readFileSync(`${previous}/record-live.mjs`,'utf8').replaceAll('offhands-qa-20261005','quivers-qa-20261005'),{flag:'wx'})
fs.copyFileSync('scripts/check-display-coverage.mjs',`${q}/scripts/check-display-coverage.mjs`)
fs.writeFileSync(`${q}/heavy-qa-owner.json`,JSON.stringify({active:true,project:'exile-quivers-20261005',ports:[20680,20681],baseHead:'aa741d572e23067e74bb17aaf6daf3a3949fb87b',authorization:'Sole writer; isolated sequential QA'},null,2)+'\n',{flag:'wx'})
console.log(q)
