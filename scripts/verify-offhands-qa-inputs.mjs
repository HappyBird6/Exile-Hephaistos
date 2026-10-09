import fs from 'node:fs'
import path from 'node:path'
import crypto from 'node:crypto'
import assert from 'node:assert/strict'

const qa = 'E:/WORK/Exile-Hephaistos/codex/offhands-qa-20261005'
const digest = p => crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex')
const files = []
function verify(source, target) {
  for (const entry of fs.readdirSync(source, { withFileTypes: true })) {
    const a = path.join(source, entry.name), b = path.join(target, entry.name)
    if (entry.isDirectory()) verify(a, b)
    else {
      assert(fs.existsSync(b), a + ' missing QA input')
      const hash = digest(a)
      assert.equal(digest(b), hash, a + ' differs from final QA input')
      files.push({ path: a.replaceAll('\\', '/'), sha256: hash })
    }
  }
}
verify('backend/src', qa + '/backend-check/src')
verify('frontend/src', qa + '/frontend-check/src')
for (const [source, target] of [
  ['backend/build.gradle.kts', 'backend-check/build.gradle.kts'],
  ['frontend/package.json', 'frontend-check/package.json'],
  ['frontend/package-lock.json', 'frontend-check/package-lock.json'],
  ['scripts/check-display-coverage.mjs', 'scripts/check-display-coverage.mjs'],
]) assert.equal(digest(source), digest(qa + '/' + target), source)
const report = { verifiedAt: new Date().toISOString(), sourceFiles: files.length, jarSha256: digest(qa + '/poe2craft.jar'), files }
fs.writeFileSync('docs/evidence/offhands-source-bundle-2026-10-05/final-qa-inputs.json', JSON.stringify(report, null, 2) + '\n', { flag: 'wx' })
console.log('Final runtime and test source equivalence:', files.length, 'files')
