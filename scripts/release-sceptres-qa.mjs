import fs from 'node:fs'
import assert from 'node:assert/strict'
import {execFileSync} from 'node:child_process'
const q='E:/WORK/Exile-Hephaistos/codex/sceptres-qa-20261005',out='docs/evidence/sceptres-runtime-bundle-2026-10-05/qa-release.json'
assert(!fs.existsSync(out),'Preserve previous evidence')
assert.equal(execFileSync('docker',['ps','-a','--filter','label=com.docker.compose.project=exile-sceptres-20261005','--format','{{.ID}}'],{encoding:'utf8'}).trim(),'')
const before=JSON.parse(fs.readFileSync(`${q}/live-before.json`)),after=JSON.parse(fs.readFileSync(`${q}/live-after.json`))
assert.deepEqual(before.services,after.services)
const releasedAt=new Date().toISOString(),owner=JSON.parse(fs.readFileSync(`${q}/heavy-qa-owner.json`)),released={...owner,active:false,releasedAt}
fs.writeFileSync(`${q}/heavy-qa-release.json`,JSON.stringify(released,null,2)+'\n',{flag:'wx'})
fs.writeFileSync(`${q}/heavy-qa-owner.json`,JSON.stringify(released,null,2)+'\n')
fs.writeFileSync(out,JSON.stringify({passed:true,releasedAt,project:owner.project,qaContainersRemaining:0,liveServicesPreserved:after.services,volumeRemoval:false,remoteOperations:false},null,2)+'\n',{flag:'wx'})
console.log('Own QA normally stopped; slot released; live4 IDs/start times/mounts preserved')
