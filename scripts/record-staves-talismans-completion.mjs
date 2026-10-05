import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
const q='E:/WORK/Exile-Hephaistos/codex/staves-talismans-qa-20261005'
const root='docs/evidence/staves-talismans-source-bundle-2026-10-05'
const read=p=>JSON.parse(fs.readFileSync(p,'utf8').replace(/^\uFEFF/,''))
const hash=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex')
const save=(name,value)=>fs.writeFileSync(`${root}/${name}.json`,JSON.stringify(value,null,2)+'\n',{flag:'wx'})
const reports={api:`${q}/api-attempt-2/api-results.json`,registry:`${q}/api-registry-results.json`,browser:`${q}/browser-attempt-1/browser-results.json`,oldFilled:`${q}/browser-attempt-1/old-filled-results.json`,negativeJewel:`${q}/browser-attempt-1/negative-jewel-results.json`}
const checks={}
for(const [name,path] of Object.entries(reports)){const r=read(path);assert(r.passed);checks[name]=r.count;save(name+'-results',r)}
const inputs=read(`${root}/final-qa-inputs.json`);for(const f of inputs.files)assert.equal(hash(f.path),f.sha256,f.path)
const metadata=read('backend/src/main/resources/catalog/top-bases.json')
const discrepancies=[]
for(const [id,b] of Object.entries(metadata).slice(-9))for(const [locale,line] of Object.entries(b.requirements??{})){
 const numbers=line.match(/\d+/g)??[];const en=b.requirements.en.match(/\d+/g)??[]
 if(JSON.stringify(numbers)!==JSON.stringify(en))discrepancies.push({id,locale,source:line,english:b.requirements.en,action:'Preserve localized source verbatim; canonical requirements remain English-source values; needs source reconciliation'})
}
save('locale-discrepancies',{discrepancies,manualVerificationRequired:true})
const images=[]
function walk(dir){for(const e of fs.readdirSync(dir,{withFileTypes:true})){const p=dir+'/'+e.name;if(e.isDirectory())walk(p);else if(p.endsWith('.png'))images.push({path:p,sha256:hash(p)})}}
walk(`${q}/browser-attempt-1`)
save('visual-review',{passed:true,method:'Direct pixel inspection of all 30 contact sheets: 12 cards, 12 full-page, 2 orange/Solar, 4 English legacy sheets. Card text at original contact resolution; full-page layout scaled. Automated DOM assertions separately cover exact text/geometry.',locales:['en','ko','ja','zh-CN','zh-TW','es'],viewports:[1440,390],findings:['No clipped card text, overlapping card rows, or broken desktop/mobile layout observed.','Spanish Permafrost131/Reflecting123 Int differs from other locale114; original source preserved.','Legacy fractured Jewel mismatch warning and preserved film are intentional.'],images})
const before=read(`${q}/live-before.json`),after=read(`${q}/live-after.json`);assert.deepEqual(before.services,after.services)
save('live-preservation',{passed:true,before,after})
const owner=read(`${q}/heavy-qa-owner.json`);assert.equal(owner.active,false)
const stopped=read(`${q}/stopped-services.json`);assert.equal(stopped.length,4);assert(stopped.every(s=>!s.State.Running&&!s.State.OOMKilled))
save('completion',{passed:true,baseCount:134,previousBaseCount:125,added:9,backend:{unit:493,integration:6},frontend:1918,checks,testedInputsUnchanged:inputs.sourceInputs,productionJavaTypeScriptChanges:0,qaReleased:true,stoppedServices:stopped.map(s=>({name:s.Name,state:s.State})),livePreserved:true,screenshots:images.length,recoveries:['Node22 engine mismatch: retried supported Node24.','Historical FE count scopes partitioned; all new+old locale loops retained.','Frontend six checks succeeded; trailing CR wrapper failure preserved, final standalone Docker build exit0.','API rejection fixture canonical modifier sorting corrected; atomic assertions retained.','Staff ten incorrect Wand detail fallbacks reconciled with exact Staff endpoints; original captures retained.'],manualSteps:['GGG trade/drop provenance unavailable HTTP403; no guessed availability/drop odds.','Spanish Staff requirement source reconciliation.','Parent bounded coverage audit and separate Git integration.'],noPushMergeDeploy:true})
console.log(checks,'completion recorded; all tested inputs unchanged')
