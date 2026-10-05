import fs from 'node:fs'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
const q = 'E:/WORK/Exile-Hephaistos/codex/wands-qa-20261005'
const out = 'docs/evidence/wands-runtime-bundle-2026-10-05/qa-release.json'
assert(!fs.existsSync(out),'Preserve previous release evidence')
const remaining = execFileSync('docker',['ps','-a','--filter','label=com.docker.compose.project=exile-wands-20261005','--format','{{.ID}}'],{encoding:'utf8'}).trim()
assert.equal(remaining,'','Only own QA normally stopped')
const before = JSON.parse(fs.readFileSync(`${q}/live-before.json`)),after = JSON.parse(fs.readFileSync(`${q}/live-after.json`))
assert.deepEqual(before.services,after.services)
const releasedAt = new Date().toISOString()
const owner = JSON.parse(fs.readFileSync(`${q}/heavy-qa-owner.json`))
const released = {...owner,active:false,releasedAt}
fs.writeFileSync(`${q}/heavy-qa-release.json`,JSON.stringify(released,null,2)+'\n',{flag:'wx'})
fs.writeFileSync(`${q}/heavy-qa-owner.json`,JSON.stringify(released,null,2)+'\n')
fs.writeFileSync(out,JSON.stringify({passed:true,releasedAt,project:owner.project,qaContainersRemaining:0,liveServicesPreserved:after.services,volumeRemoval:false,remoteOperations:false,oldFilledLegacyChecks:7},null,2)+'\n',{flag:'wx'})
console.log('Own QA normally stopped; exclusive slot released; existing live4 preserved')
