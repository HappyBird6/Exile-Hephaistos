import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { execFileSync } from 'node:child_process'
const root = 'docs/evidence/quivers-source-bundle-2026-10-05'
const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex')
const proofs = []
for (const name of fs.readdirSync(root).filter(n => n.endsWith('.html')).sort()) {
  const path = `${root}/${name}`
  const recorded = JSON.parse(fs.readFileSync(path.replace(/\.html$/, '.json'))).sha256
  const staged = execFileSync('git', ['show', `:${path}`], { maxBuffer: 16 * 1024 * 1024 })
  assert.equal(hash(staged), recorded, path)
  assert.equal(hash(fs.readFileSync(path)), recorded, path)
  proofs.push({ path, sha256: recorded, stagedBytesPreserved: true })
}
assert.equal(proofs.length, 72)
const detailRoot = `${root}/details/Quivers`
let details = 0
for (const name of fs.readdirSync(detailRoot).filter(n => n.endsWith('.json'))) {
  const path = `${detailRoot}/${name}`
  const staged = JSON.parse(execFileSync('git', ['show', `:${path}`], { encoding: 'utf8', maxBuffer: 4 * 1024 * 1024 }))
  assert.equal(hash(Buffer.from(staged.html)), staged.sha256, path)
  details++
}
fs.writeFileSync(`${root}/git-index-proofs.json`, JSON.stringify({ passed: true, rawResponses: 72, detailResponses: details, proofs }, null, 2) + '\n', { flag: 'wx' })
console.log('Git index checksum proofs passed:', proofs.length, 'raw responses and', details, 'detail responses')
