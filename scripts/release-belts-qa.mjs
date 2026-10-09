import fs from 'node:fs'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
const q = 'E:/WORK/Exile-Hephaistos/codex/belts-qa-20261005'
const ownerPath = `${q}/heavy-qa-owner.json`, owner = JSON.parse(fs.readFileSync(ownerPath))
assert(owner.active && owner.project === 'exile-belts-20261005')
const remaining = execFileSync('docker', ['ps', '-a', '--filter', 'label=com.docker.compose.project=exile-belts-20261005', '--format', '{{.Names}}'], { encoding: 'utf8' }).trim()
assert.equal(remaining, '')
const before = JSON.parse(fs.readFileSync(`${q}/live-before.json`)), after = JSON.parse(fs.readFileSync(`${q}/live-after.json`))
assert.deepEqual(after.services, before.services)
const releasedAt = new Date().toISOString(), release = { project: owner.project, releasedAt, active: false, shutdown: 'docker compose down (without -v)', liveServiceIdsStartTimesMountsPreserved: true, userBrowserStorage: 'Only new isolated Chromium contexts were used', existingVolumesDeleted: false, qaEvidencePreserved: q, remainingOwnContainers: [] }
fs.writeFileSync(`${q}/qa-release.json`, JSON.stringify(release, null, 2) + '\n', { flag: 'wx' })
fs.writeFileSync(ownerPath, JSON.stringify({ ...owner, active: false, releasedAt }, null, 2) + '\n')
const out = 'docs/evidence/belts-runtime-bundle-2026-10-05'
for (const file of ['live-before.json', 'live-after.json', 'qa-release.json']) { assert(!fs.existsSync(`${out}/${file}`)); fs.copyFileSync(`${q}/${file}`, `${out}/${file}`) }
fs.appendFileSync('docs/workbench-belts-bundle-2026-10-05.md', '\nQA는 전용 project만 `docker compose down`으로 정상 종료했고 heavy QA slot을 반환했다. [release evidence](evidence/belts-runtime-bundle-2026-10-05/qa-release.json)의 live container ID/start time/mount 비교는 일치한다. 기존 volumes와 사용자 browser storage를 보존했으며 push/merge/deploy는 수행하지 않았다.\n')
console.log(release)
