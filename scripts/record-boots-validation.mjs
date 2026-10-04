import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
const qa='E:/WORK/Exile-Hephaistos/codex/boots-qa-20261004'
const root='docs/evidence/boots-runtime-bundle-2026-10-04'
assert(!fs.existsSync(root+'/validation.json'),'Preserve earlier validation output')
assert(fs.readFileSync(qa+'/backend-check-3.log','utf8').includes('BUILD SUCCESSFUL'))
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'))
const totals={}
for(const task of ['test','integrationTest']) {
 const dir=`${qa}/backend-check/build/test-results/${task}`
 const sum={tests:0,failures:0,errors:0,skipped:0,suites:0}
 for(const f of fs.readdirSync(dir).filter(f=>f.endsWith('.xml'))) {
  const s=fs.readFileSync(dir+'/'+f,'utf8').match(/<testsuite\b[^>]+>/)[0]
  for(const a of ['tests','failures','errors','skipped'])sum[a]+=Number(s.match(new RegExp(a+'="(\\d+)"'))[1])
  sum.suites++
 }
 assert.equal(sum.failures+sum.errors+sum.skipped,0)
 totals[task]=sum
}
const api=read(qa+'/api-results.json'),contract=read(qa+'/boots-contract-results.json'),browser=read(qa+'/browser-attempt-1/browser-results.json'),replay=read(qa+'/importer-replay-results.json')
for(const result of [api,contract,browser,replay])assert(result.passed)
const frontend=fs.readFileSync(qa+'/frontend-check-3.log','utf8').replace(/\x1b\[[0-9;]*m/g,'')
assert(frontend.includes('built in'))
const fe=Number(frontend.match(/Tests\s+(\d+) passed/)[1])
const shots=fs.readdirSync(qa+'/browser-attempt-1').filter(f=>f.endsWith('.png')&&!f.startsWith('contact-'))
assert.equal(shots.length,99)
const release=read(qa+'/qa-release.json');assert.equal(release.active,false)
const summary={passed:true,baseHead:'e8034894cce832d0a7397d0265cc1b1d68ee2628',branch:'workbench/top-bases-20261004',baseCount:Object.keys(read(qa+'/api-initials.json')).length,backend:totals,frontend:{tests:fe,checks:['npm ci','lint','typecheck','format:check','test -- --run','build'],bundleSizeWarning:'Existing Vite chunk warning retained'},api:{general:api.count,bootsContract:contract.count,total:api.count+contract.count},browser:{checks:browser.count,screenshots:shots.length,errors:browser.errors,viewports:[1440,390],locales:['en','ko','ja','zh-CN','zh-TW','es'],pixelReview:'All 72 locale/viewport captures inspected in contact sheets; representative original-size captures, 6 orange previews and 21 old films inspected'},importer:replay,scope:{supportExplorer:'Solar-only',deferred:50,review:'Self-review; no separate reviewer',remotePushMergeDeploy:false},qa:{project:'exile-boots-20261004',ports:[19680,19681],released:true}}
assert.equal(summary.baseCount,41)
summary.jarSha256=crypto.createHash('sha256').update(fs.readFileSync(qa+'/poe2craft.jar')).digest('hex')
summary.logs={backend:'backend-check-3.log',frontend:'frontend-check-3.log',api:'api-check-2.log',bootsContract:'boots-contract-4.log',browser:'browser-check-1.log'}
summary.displayCoverage=read(qa+'/display-coverage-final.json')
summary.cardCrops=fs.readdirSync(qa+'/browser-attempt-1/cards').filter(f=>f.endsWith('.png')).length
assert.equal(summary.cardCrops,72)
for(const file of ['api-results.json','boots-contract-results.json','importer-replay-results.json','qa-release.json'])fs.copyFileSync(qa+'/'+file,root+'/'+file)
fs.copyFileSync(qa+'/display-coverage-final.json',root+'/display-coverage.json')
fs.copyFileSync(qa+'/browser-attempt-1/browser-results.json',root+'/browser-results.json')
for(const file of ['tasalian-ko-390.png','drakeskin-en-1440.png','sekhema-ja-390.png','blacksteel-boots-zh-CN-390.png','faithful-es-1440.png','daggerfoot-zh-TW-390.png','tasalian-orange-preview.png','old-film-sleek.png'])fs.copyFileSync(qa+'/browser-attempt-1/'+file,root+'/'+file)
fs.writeFileSync(root+'/validation.json',JSON.stringify(summary,null,2)+'\n')
console.log(JSON.stringify(summary,null,2))
