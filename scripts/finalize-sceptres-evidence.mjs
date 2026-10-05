import fs from 'node:fs'
import path from 'node:path'
import crypto from 'node:crypto'
import assert from 'node:assert/strict'
import './verify-sceptres-preservation.mjs'
import './verify-sceptres-identities.mjs'
const q='E:/WORK/Exile-Hephaistos/codex/sceptres-qa-20261005',out='docs/evidence/sceptres-runtime-bundle-2026-10-05'
assert(!fs.existsSync(out),'Preserve previous evidence')
const walk=root=>fs.readdirSync(root,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(root,e.name)):[path.join(root,e.name)])
const hash=file=>crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex')
let sourceFiles=0
for(const area of ['backend','frontend']) for(const file of walk(`${area}/src`)) {
 assert(fs.readFileSync(file).equals(fs.readFileSync(`${q}/${area}-check/${file.replace(/^[^/\\]+[/\\]/,'')}`)),file)
 sourceFiles++
}
const count=kind=>walk(`${q}/backend-check/build/test-results/${kind}`).filter(p=>p.endsWith('.xml')).reduce((n,p)=>{
 const xml=fs.readFileSync(p,'utf8'),root=xml.match(/<testsuite[^>]+>/)[0]
 for(const flag of ['failures','errors','skipped']) assert.equal(+root.match(new RegExp(`${flag}="(\\d+)"`))[1],0,p)
 return n + +root.match(/tests="(\d+)"/)[1]
},0)
assert(fs.readFileSync(`${q}/backend-check-1.log`,'utf8').includes('BUILD SUCCESSFUL'))
assert(fs.readFileSync(`${q}/frontend-check-2.log`,'utf8').replace(/\x1b\[[0-9;]*m/g,'').includes('1820 passed (1820)'))
const api=JSON.parse(fs.readFileSync(`${q}/api-attempt-2/api-results.json`)),browser=JSON.parse(fs.readFileSync(`${q}/browser-attempt-2/browser-results.json`)),legacy=JSON.parse(fs.readFileSync(`${q}/browser-attempt-2/old-filled-results.json`))
assert(api.passed && browser.passed && legacy.passed && browser.errors.length===0)
assert.equal(Object.keys(JSON.parse(fs.readFileSync(`${q}/api-attempt-2/api-initials.json`))).length,78)
const pngs=walk(`${q}/browser-attempt-2`).filter(p=>p.endsWith('.png')),contacts=pngs.filter(p=>path.basename(p).startsWith('contact-'))
assert.equal(contacts.length,30)
assert.equal(pngs.length-contacts.length,180)
const reusedCardSheets=contacts.filter(p=>path.basename(p).startsWith('contact-cards-'))
assert.equal(reusedCardSheets.length,14)
for(const file of reusedCardSheets) assert(fs.readFileSync(file).equals(fs.readFileSync(`${q}/browser-attempt-1/${path.basename(file)}`)),`Previously reviewed card pixels must match exactly: ${file}`)
fs.mkdirSync(out)
const failureHistory=[
 {stage:'source collection',error:'sandbox EACCES',resolution:'Approved source reads; empty/new-only output preserved'},
 {stage:'additional source weight review',error:'New assertion incorrectly equated detail Spawn Tags 1 with published class DropChance 250',resolution:'Separate strict applicability and published selection-weight checks; no catalog or existing expected-value change'},
 {stage:'API attempt1',error:'UND_ERR_SOCKET during app startup',preservedPath:`${q}/api-attempt-1`,resolution:'Same assertions plus high-tier positives passed against ready runtime in new api-attempt-2'},
 {stage:'pixels self-review',error:'New Sceptres inherited legacy Skeletal Warrior scope notice',preservedPath:`${q}/browser-attempt-1`,resolution:'Added generic source skill notice in all six locales; original legacy notices preserved; full Frontend checks and stronger browser assertions repeated into new outputs'},
]
fs.writeFileSync(`${out}/failure-history.json`,JSON.stringify({failures:failureHistory},null,2)+'\n',{flag:'wx'})
for(const [from,to] of [['api-attempt-2/api-results.json','api-results.json'],['browser-attempt-2/browser-results.json','browser-results.json'],['browser-attempt-2/old-filled-results.json','old-filled-results.json']]) fs.copyFileSync(`${q}/${from}`,`${out}/${to}`)
fs.writeFileSync(`${out}/screenshots.json`,JSON.stringify({visuallyReviewedContactSheets:30,reusedPixelIdenticalCardSheets:14,newlyReviewedFinalFullSheets:16,files:pngs.map(p=>({file:p.replaceAll('\\','/'),sha256:hash(p)}))},null,2)+'\n',{flag:'wx'})
fs.writeFileSync(`${out}/validation.json`,JSON.stringify({passed:true,totalBases:78,addedBases:['stoic','omen','shrine-fire','shrine-ice','shrine-lightning','clasped','wrath'],backendUnitArchitecture:count('test'),backendIntegration:count('integrationTest'),frontendTests:1820,apiChecks:api.count,browserChecks:browser.count,oldFilledLegacyChecks:legacy.count,browserErrors:browser.errors,matchingValidatedSourceFiles:sourceFiles,screenshots:180,visuallyReviewedContactSheets:30,jarSha256:hash(`${q}/poe2craft.jar`),qaRoot:q,qaProject:'exile-sceptres-20261005',qaPorts:[20280,20281],successfulLogs:['backend-check-1.log','frontend-check-2.log'],failureHistory,preservation:'Existing71 API initials/data/translations/registry220/deferred50, exact legacy and new films, Solar-only Support/Explorer, Shift/Alt, orange preview, Solar quality40 and HALF_UP',remainingScope:'Belt distinct implicits and Breach roster; Crossbow/melee/Shields/Foci/Quivers source/mechanics review'},null,2)+'\n',{flag:'wx'})
console.log({sourceFiles,tests:count('test'),integration:count('integrationTest'),api:api.count,browser:browser.count,screenshots:180,contacts:30})
