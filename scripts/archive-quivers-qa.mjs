import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'

const qa = 'E:/WORK/Exile-Hephaistos/codex/quivers-qa-20261005'
const target = 'docs/evidence/quivers-source-bundle-2026-10-05'
const hash = p => crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex')
const read = p => JSON.parse(fs.readFileSync(p, 'utf8'))
const save = (name, value) => fs.writeFileSync(`${target}/${name}`, JSON.stringify(value, null, 2) + '\n', { flag: 'wx' })
const browser = `${qa}/browser-attempt-4`
for (const [source, name] of [
  [`${qa}/api-attempt-1/api-results.json`, 'api-results.json'],
  [`${browser}/browser-results.json`, 'browser-results.json'],
  [`${browser}/old-filled-results.json`, 'old-filled-results.json'],
  [`${qa}/input-equivalence-final.json`, 'final-qa-inputs.json'],
]) {
  const value = read(source)
  assert.equal(value.passed, true, source)
  save(name, value)
}
assert.equal(read(`${qa}/heavy-qa-owner.json`).active, false)
assert.deepEqual(read(`${qa}/live-before.json`).services, read(`${qa}/live-after.json`).services)
const unit = kind => {
  const root = `${qa}/backend-check/build/test-results/${kind}`
  const result = { tests: 0, failures: 0, errors: 0, skipped: 0 }
  for (const name of fs.readdirSync(root).filter(n => /^TEST.*\.xml$/.test(n))) {
    const header = fs.readFileSync(`${root}/${name}`, 'utf8').match(/<testsuite\s[^>]+>/)[0]
    for (const key of Object.keys(result)) result[key] += Number(header.match(new RegExp(`${key}="(\\d+)"`))?.[1] ?? 0)
  }
  assert.equal(result.failures + result.errors + result.skipped, 0)
  return result
}
const logs = ['backend-check-1.log', 'backend-check-2.log', 'backend-check-3.log', 'frontend-check-1.log', 'frontend-check-2.log', 'frontend-check-3.log', 'frontend-check-4.log'].map(name => ({ name, sha256: hash(`${qa}/${name}`) }))
assert.match(fs.readFileSync(`${qa}/backend-check-3.log`, 'utf8'), /BUILD SUCCESSFUL/)
assert.match(fs.readFileSync(`${qa}/frontend-check-4.log`, 'utf8').replace(/\x1b\[[0-9;]*m/g, ''), /1903 passed/)
const captures = fs.readdirSync(browser, { recursive: true }).filter(name => name.endsWith('.png')).sort().map(name => ({ name: name.replaceAll('\\', '/'), bytes: fs.statSync(`${browser}/${name}`).size, sha256: hash(`${browser}/${name}`) }))
const original = captures.filter(c => !c.name.startsWith('contact-'))
assert.equal(original.length, 312)
assert.equal(captures.length - original.length, 52)
save('screenshots.json', { directory: browser, originalCount: original.length, contactSheets: 52, captures })
save('qa-completion.json', {
  baseHead: 'aa741d572e23067e74bb17aaf6daf3a3949fb87b', addedBases: 11, totalBases: 113,
  backend: { command: 'sh gradlew --no-daemon spotlessApply check generateJooq bootJar', unit: unit('test'), integration: unit('integrationTest') },
  frontend: { npmCi: true, lint: true, typecheck: true, formatCheck: true, tests: 1903, testFiles: 66, build: true },
  api: read(`${qa}/api-attempt-1/api-results.json`).count,
  browser: read(`${browser}/browser-results.json`).count,
  legacy: read(`${browser}/old-filled-results.json`).count,
  browserErrors: read(`${browser}/browser-results.json`).errors,
  visualReview: read(`${qa}/pixel-review.json`), logs,
  livePreservation: { before: read(`${qa}/live-before.json`), after: read(`${qa}/live-after.json`) },
  release: read(`${qa}/release.json`),
  failedAttempts: { frontend1: read(`${qa}/frontend-attempt-1-failure.json`), frontend2: read(`${qa}/frontend-attempt-2-failure.json`), browser1: read(`${qa}/browser-attempt-1/failure.json`), browser2: read(`${qa}/browser-attempt-2/failure.json`), browser3: read(`${qa}/browser-attempt-3/failure.json`) },
  availabilityGap: 'GGG trade2 public endpoint HTTP403; current PoE2DB ordinary class list and normal popup rarity flags verified. Exact drop locations/rates unverified.',
  gitBoundary: 'Local branch commit only; no push/merge/deploy',
})
console.log('Final Quiver QA evidence archived without overwriting prior output')
