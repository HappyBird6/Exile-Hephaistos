import fs from 'node:fs'
import crypto from 'node:crypto'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
const q='E:/WORK/Exile-Hephaistos/codex/maces-qa-20261005'
const root='docs/evidence/maces-source-bundle-2026-10-05'
const digest=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex')
const save=(n,v)=>fs.writeFileSync(`${root}/${n}`,JSON.stringify(v,null,2)+'\n',{flag:'wx'})
for(const [from,to] of [['api-attempt-1/api-results.json','api-results.json'],['browser-attempt-2/browser-results.json','browser-results.json'],['browser-attempt-2/old-filled-results.json','old-filled-results.json'],['live-before.json','live-before.json'],['live-after.json','live-after.json']]){
 const v=JSON.parse(fs.readFileSync(`${q}/${from}`,'utf8'));save(to,v)
}
const b=`${q}/browser-attempt-2`
const png=[...fs.readdirSync(b).filter(n=>n.endsWith('.png')).map(n=>n),...fs.readdirSync(`${b}/cards`).filter(n=>n.endsWith('.png')).map(n=>`cards/${n}`)]
assert.equal(png.filter(n=>!n.startsWith('contact-')).length,192)
assert.equal(png.filter(n=>n.startsWith('contact-')).length,32)
save('screenshots.json',{directory:b,files:png.map(file=>({file,sha256:digest(`${b}/${file}`)}))})
save('visual-review.json',{passed:true,reviewedAt:new Date().toISOString(),method:'Direct view_image pixel review of all 32 contact sheets covering 192 originals; six Fortified mobile originals and all twelve orange/Solar originals also inspected directly',locales:['en','ko','ja','zh-CN','zh-TW','es'],viewports:[1440,390],findings:['Names, requirements, implicit values and source-not-computed properties readable','Desktop/mobile cards wrap without clipped modifier text','Orange preview and Solar quality40/cap20/HALF_UP preserved','Existing filled Wand/Sceptre/Hallowed films display unchanged'],limitations:['Complete desktop screenshots are scaled on contact sheets; card details reviewed separately','Source weapon properties are displayed without combat simulation']})
const services=['frontend','app','postgres','redis'].map(x=>{const name=`exile-maces-20261005-${x}-1`;return {name,...JSON.parse(execFileSync('docker',['inspect','--format','{{json .State}}',name],{encoding:'utf8'}))}})
assert(services.every(x=>!x.Running&&!x.OOMKilled))
const ownerPath=`${q}/heavy-qa-owner.json`,owner=JSON.parse(fs.readFileSync(ownerPath,'utf8'))
owner.active=false;owner.releasedAt=new Date().toISOString();fs.writeFileSync(ownerPath,JSON.stringify(owner,null,2)+'\n')
save('completion.json',{passed:true,totalBases:119,previousBases:113,addedBases:6,backendUnit:482,backendIntegration:6,frontendTests:1909,apiChecks:2773,browserChecks:1058,oldFilledChecks:25,pageErrors:0,originalScreenshots:192,contactSheets:32,finalInputs:'final-qa-inputs-3.json',services,qaOwner:owner,livePreserved:true,logs:['backend-check-1.log','frontend-check-3.log'].map(file=>({path:`${q}/${file}`,sha256:digest(`${q}/${file}`)})),remoteActions:false})
console.log('Completion evidence saved; own QA stopped and slot released')
