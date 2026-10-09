import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'

const qa='E:/WORK/Exile-Hephaistos/codex/crossbows-qa-20261005'
const root='docs/evidence/crossbows-source-bundle-2026-10-05'
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'))
const hash=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex')
assert(fs.readFileSync(`${qa}/backend-check-3.log`,'utf8').includes('BUILD SUCCESSFUL'))
const fe=fs.readFileSync(`${qa}/frontend-check-2.log`,'utf8')
assert(/1865 passed/.test(fe))
assert(/built in/.test(fe))
for(const p of ['api-results.json','browser-results.json','old-filled-results.json','preservation-final.json'])assert.equal(read(`${root}/${p}`).passed,true)
assert.deepEqual(read(`${qa}/live-before.json`).services,read(`${qa}/live-after.json`).services)
const summary={
 passed:true,baseline:'9fbb83da948d515756b6cea9ffb7a24155abba8e',baseCount:97,
 backend:{unit:460,integration:6,command:'check generateJooq bootJar',log:'backend-check-3.log',sha256:hash(`${qa}/backend-check-3.log`)},
 frontend:{tests:1865,files:63,commands:['npm ci','lint','typecheck','format:check','test -- --run','build'],log:'frontend-check-2.log',sha256:hash(`${qa}/frontend-check-2.log`)},
 api:read(`${root}/api-results.json`).count,browser:read(`${root}/browser-results.json`).count,legacyFilled:read(`${root}/old-filled-results.json`).count,
 pixels:{bases:6,locales:['en','ko','zh-CN','zh-TW','ja','es'],widths:[1440,390],cards:72,fullPages:72,orange:6,solarOverflow:6,cardContactSheetsReviewed:12,fullContactSheetsReviewed:14,issues:[]},
 isolatedQa:qa,project:'exile-crossbows-20261005',ports:[20480,20481],servicesNormallyStopped:true,liveIdsStartedAtMountsPreserved:true,
 remainingBlockers:[],runtimeChangesAfterFinalAggregate:false,
}
fs.writeFileSync(`${root}/qa-summary.json`,JSON.stringify(summary,null,2)+'\n',{flag:'wx'})
const releasedAt=new Date().toISOString()
fs.writeFileSync(`${qa}/qa-release.json`,JSON.stringify({releasedAt,normalStop:true,services:['app','frontend','postgres','redis'],states:['exited','exited','exited','exited'],livePreserved:true,volumesDeleted:false,summary:`${root}/qa-summary.json`},null,2)+'\n',{flag:'wx'})
const owner=read(`${qa}/heavy-qa-owner.json`)
assert.equal(owner.active,true)
fs.writeFileSync(`${qa}/heavy-qa-owner.json`,JSON.stringify({...owner,active:false,releasedAt,result:'All checks passed; own QA normally stopped; local commit pending'},null,2)+'\n')
console.log(JSON.stringify(summary,null,2))
