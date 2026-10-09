import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'))
const proof='/source/docs/evidence/body-helmets-runtime-bundle-2026-10-04'
const save=(p,v)=>{assert(!fs.existsSync(p),'Preserve prior validation output');fs.writeFileSync(p,JSON.stringify(v,null,2)+'\n')}
function tests(kind) {
  let count=0
  for(const file of fs.readdirSync(`/qa/backend-check/build/test-results/${kind}`).filter(f=>f.endsWith('.xml'))) {
    const xml=fs.readFileSync(`/qa/backend-check/build/test-results/${kind}/${file}`,'utf8')
    for(const field of ['failures','errors','skipped'])assert.equal(+xml.match(new RegExp(`${field}="(\\d+)"`))[1],0,file)
    count+=+xml.match(/tests="(\d+)"/)[1]
  }
  return count
}
assert(fs.readFileSync('/qa/backend-check-3.log','utf8').includes('BUILD SUCCESSFUL'))
assert(fs.readFileSync('/qa/backend-final-package.log','utf8').includes('BUILD SUCCESSFUL'))
const frontendLog=fs.readFileSync('/qa/frontend-check-4.log','utf8').replace(/\x1b\[[0-9;]*m/g,'')
const frontend=+frontendLog.match(/Tests\s+(\d+) passed/)[1]
assert(frontendLog.includes('vite build')&&frontendLog.includes('built in'))
const coveragePaths=fs.readdirSync('/qa').filter(f=>f==='display-coverage-final.json')
assert.equal(coveragePaths.length,1)
const coverage=read('/qa/'+coveragePaths[0])
const api=read('/qa/api-attempt-2/api-results.json'), browser=read('/qa/browser-attempt-2/browser-results.json'), replay=read('/qa/importer-replay-results-2.json')
assert(api.passed&&browser.passed&&replay.passed)
assert.equal(browser.errors.length,0)
assert.equal(browser.count,598)
const screenshotCount=fs.readdirSync('/qa/browser-attempt-2').filter(f=>f.endsWith('.png')).length
assert.equal(screenshotCount,105)
const manifest=read('/source/backend/src/main/resources/catalog/top-bases.json')
const keys=['slipstrike','death-mail','sleek','vile','wolfskin','ancestral','cryptic']
const bases=keys.map(k=>({key:k,...manifest[k]}))
assert.equal(api.initialBases,35)
const value={passed:true,baseHead:'a03d3c41ff012bd335f2268fe407a198ed58b6d6',branch:'workbench/top-bases-20261004',qaRoot:'codex/body-helmets-qa-20261004',project:'exile-body-helmets-20261004',ports:[19580,19581],backend:{unitAndArchUnit:tests('test'),integration:tests('integrationTest'),log:'backend-check-3.log',finalPackagingLog:'backend-final-package.log'},frontend:{tests:frontend,files:59,npmCiLog:'frontend-check-2.log',finalLog:'frontend-check-4.log'},api:{assertions:api.count,initialBases:api.initialBases},browser:{checks:browser.count,errors:browser.errors.length,screenshots:screenshotCount,localeScreenshots:84,orangeScreenshots:7,oldFilmScreenshots:14},coverage,replay,jarSha256:crypto.createHash('sha256').update(fs.readFileSync('/qa/poe2craft.jar')).digest('hex'),addedBases:bases.map(({key,id,name,family,armour,evasion,energyShield,requiredLevel,strength,dexterity,intelligence,baseMovementSpeed})=>({key,id,name,family,armour,evasion,energyShield,requiredLevel,strength,dexterity,intelligence,baseMovementSpeed})),remainingArmour:['Tasalian Greaves','Drakeskin Boots','Sekhema Sandals','Blacksteel Sabatons','Faithful Leggings','Daggerfoot Shoes'],failuresPreserved:['backend-check-1.log','backend-failure-1.xml','backend-launch-2.log','frontend-check-1.log','frontend-check-2.log','body-import-3.log','browser-launch-failure.log','browser-attempt-1'],review:'Author self-review; no separate reviewer; source roster, full pools, hard rows, runtime routing, class targets, six locales and historical data preservation checked'}
save('/qa/validation.json',value)
save(proof+'/validation.json',value)
for(const [name,v]of [['api-results.json',api],['browser-results.json',browser],['display-coverage.json',coverage],['importer-replay-results.json',replay]])save(proof+'/'+name,v)
console.log(JSON.stringify({backend:value.backend,frontend:value.frontend,api:value.api,browser:value.browser,replay},null,2))
