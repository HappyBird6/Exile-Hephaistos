import fs from 'node:fs'
import assert from 'node:assert/strict'
import {execFileSync} from 'node:child_process'
const q='E:/WORK/Exile-Hephaistos/codex/amulets-qa-20261004',out='docs/evidence/amulets-runtime-bundle-2026-10-04/qa-release.json'
assert(!fs.existsSync(out),'Preserve prior output')
const expected=['c8e74d399af207f171b43577a6106ebd341942ca00658a246dc8dfa55a9c83b4','c4bbebf6e190002f7536eaa9b60aac1df4d684a7287d7d75470a491fc2c4a408','747188687b03c214715c9ab66d6286fd890bba81d6247c618e5afd39d32c0eba','9f0962732bc25e1b9e06acbac0fe47fd43e33a835e304962081e7c09a9222a1f']
const live=expected.map(id=>{
 const d=JSON.parse(execFileSync('docker',['inspect','--format','{{json .}}',id],{encoding:'utf8'}))
 assert.equal(d.Id,id);assert.equal(d.State.Running,true)
 return {id:d.Id,name:d.Name,startedAt:d.State.StartedAt,image:d.Config.Image,ports:d.NetworkSettings.Ports}
})
assert.equal(live[0].startedAt,'2026-10-04T13:18:17.193444193Z')
assert.equal(live[1].startedAt,'2026-10-04T12:43:17.378679745Z')
const remaining=execFileSync('docker',['ps','-a','--filter','label=com.docker.compose.project=exile-amulets-20261004','--format','{{.ID}}'],{encoding:'utf8'}).trim()
assert.equal(remaining,'','Only own QA normally stopped')
const releasedAt=new Date().toISOString()
fs.writeFileSync(out,JSON.stringify({passed:true,releasedAt,project:'exile-amulets-20261004',qaContainersRemaining:0,live,qaPorts:[19980,19981],volumeRemoval:false,remoteOperations:false},null,2)+'\n')
const owner=JSON.parse(fs.readFileSync(q+'/heavy-qa-owner.json'))
fs.writeFileSync(q+'/heavy-qa-release.json',JSON.stringify({...owner,active:false,releasedAt},null,2)+'\n',{flag:'wx'})
fs.writeFileSync(q+'/heavy-qa-owner.json',JSON.stringify({...owner,active:false,releasedAt},null,2)+'\n')
