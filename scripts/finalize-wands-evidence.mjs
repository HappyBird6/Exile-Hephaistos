import fs from 'node:fs'
import path from 'node:path'
import crypto from 'node:crypto'
import assert from 'node:assert/strict'
import './verify-wands-preservation.mjs'
const q = 'E:/WORK/Exile-Hephaistos/codex/wands-qa-20261005'
const out = 'docs/evidence/wands-runtime-bundle-2026-10-05'
assert(!fs.existsSync(out),'Preserve prior evidence')
const walk = root => fs.readdirSync(root,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(root,e.name)):[path.join(root,e.name)])
const hash = file => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex')
let sourceFiles = 0
for(const area of ['backend','frontend'])for(const file of walk(`${area}/src`)){
 assert(fs.readFileSync(file).equals(fs.readFileSync(`${q}/${area}-check/${file.replace(/^[^/\\]+[/\\]/,'')}`)),file)
 sourceFiles++
}
const count = kind => walk(`${q}/backend-check/build/test-results/${kind}`).filter(p=>p.endsWith('.xml')).reduce((n,p)=>{
 const xml=fs.readFileSync(p,'utf8'),root=xml.match(/<testsuite[^>]+>/)[0]
 for(const flag of ['failures','errors','skipped'])assert.equal(+root.match(new RegExp(`${flag}="(\\d+)"`))[1],0,p)
 return n + +root.match(/tests="(\d+)"/)[1]
},0)
const frontendLog=fs.readFileSync(`${q}/frontend-check-2.log`,'utf8').replace(/\x1b\[[0-9;]*m/g,'')
assert(frontendLog.includes('1820 passed (1820)'))
assert(fs.readFileSync(`${q}/backend-check-2.log`,'utf8').includes('BUILD SUCCESSFUL'))
const api=JSON.parse(fs.readFileSync(`${q}/api-attempt-1/api-results.json`)),browser=JSON.parse(fs.readFileSync(`${q}/browser-attempt-1/browser-results.json`))
assert(api.passed && browser.passed && browser.errors.length === 0)
const pngs=walk(`${q}/browser-attempt-1`).filter(p=>p.endsWith('.png'))
const contacts=pngs.filter(p=>path.basename(p).startsWith('contact-'))
assert.equal(contacts.length,38)
assert.equal(pngs.length-contacts.length,228)
fs.mkdirSync(out)
for(const [from,to] of [['api-attempt-1/api-results.json','api-results.json'],['browser-attempt-1/browser-results.json','browser-results.json']])fs.copyFileSync(`${q}/${from}`,`${out}/${to}`)
fs.writeFileSync(`${out}/screenshots.json`,JSON.stringify({visuallyReviewedContactSheets:38,files:pngs.map(p=>({file:p.replaceAll('\\','/'),sha256:hash(p)}))},null,2)+'\n')
fs.writeFileSync(`${out}/validation.json`,JSON.stringify({passed:true,totalBases:71,addedBases:['Bone','Siphoning','Volatile','Galvanic','Acrid','Offering','Critical','Primordial','Dueling'],backendUnitArchitecture:count('test'),backendIntegration:count('integrationTest'),frontendTests:1820,apiChecks:api.count,browserChecks:browser.count,browserErrors:browser.errors,matchingValidatedSourceFiles:sourceFiles,screenshots:228,visuallyReviewedContactSheets:38,jarSha256:hash(`${q}/poe2craft.jar`),qaRoot:q,qaProject:'exile-wands-20261005',qaPorts:[20180,20181],commands:['check generateJooq bootJar','npm ci; lint; typecheck; format:check; test -- --run; build','docker compose config --quiet'],preservedFailureLogs:['backend-check-1.log: corrected new test accessor','frontend-check-1.log: old Node22 image rejected by current engine requirement'],successfulLogs:['backend-check-2.log','frontend-check-2.log'],preservation:'Existing62 initial API responses, data/translations/registry220/deferred50, old films, Solar-only Support/Explorer, Shift/Alt, orange preview, Solar quality40 and HALF_UP all verified',remainingScope:'Sceptre7, Belt and broader class gaps; unique/runeforged availability remains separate. See scope document.'},null,2)+'\n')
console.log({sourceFiles,tests:count('test'),integration:count('integrationTest'),screenshots:228,contacts:38})
