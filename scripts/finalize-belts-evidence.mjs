import fs from 'node:fs'
import path from 'node:path'
import crypto from 'node:crypto'
import assert from 'node:assert/strict'
import './verify-belts-preservation.mjs'
const q = 'E:/WORK/Exile-Hephaistos/codex/belts-qa-20261005', out = 'docs/evidence/belts-runtime-bundle-2026-10-05'
assert(!fs.existsSync(out))
const walk = root => fs.readdirSync(root, { withFileTypes: true }).flatMap(e => e.isDirectory() ? walk(path.join(root, e.name)) : [path.join(root, e.name)])
const hash = file => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex')
let sourceFiles = 0
for (const area of ['backend', 'frontend']) for (const file of walk(`${area}/src`)) {
  assert(fs.readFileSync(file).equals(fs.readFileSync(`${q}/${area}-check/${file.replace(/^[^/\\]+[/\\]/, '')}`)), file)
  sourceFiles++
}
const count = kind => walk(`${q}/backend-check/build/test-results/${kind}`).filter(p => p.endsWith('.xml')).reduce((n, p) => {
  const root = fs.readFileSync(p, 'utf8').match(/<testsuite[^>]+>/)[0]
  for (const flag of ['failures', 'errors', 'skipped']) assert.equal(+root.match(new RegExp(`${flag}="(\\d+)"`))[1], 0, p)
  return n + +root.match(/tests="(\d+)"/)[1]
}, 0)
assert(fs.readFileSync(`${q}/backend-check-2.log`, 'utf8').includes('BUILD SUCCESSFUL'))
const frontendFinal = fs.readFileSync(`${q}/frontend-check-5.log`, 'utf8').replace(/\x1b\[[0-9;]*m/g, '')
assert(frontendFinal.includes('1859 passed (1859)') && frontendFinal.includes('All matched files use Prettier code style!') && frontendFinal.includes('built in'))
assert(fs.readFileSync(`${q}/frontend-check-4.log`, 'utf8').includes('> prettier --check .')) // set-e reached format only after npm ci/lint/typecheck passed
const read = p => JSON.parse(fs.readFileSync(`${q}/${p}`, 'utf8'))
const api = read('api-attempt-3/api-results.json'), browser = read('browser-attempt-2/browser-results.json'), legacy = read('browser-attempt-2/old-filled-results.json'), implicit = read('implicit-attempt-2/results.json')
assert(api.passed && browser.passed && legacy.passed && implicit.passed && browser.errors.length === 0)
assert.equal(Object.keys(read('api-attempt-3/api-initials.json')).length, 91)
const pngs = walk(`${q}/browser-attempt-2`).filter(p => p.endsWith('.png')), contacts = pngs.filter(p => path.basename(p).startsWith('contact-'))
assert.equal(contacts.length, 54)
assert.equal(pngs.length - contacts.length, 324)
fs.mkdirSync(out)
const failures = [
  { stage: 'source collection', error: 'Sandbox EACCES and prior output overwrite rejection', resolution: 'Authorized reads and new-only isolated paths; prior outputs preserved; no denial bypass' },
  { stage: 'Frontend attempt1', error: 'Linen absent requirement incorrectly imported as Level1', resolution: 'Exact six-locale source absence retained with requiredLevel0; scoped source identity exception checked, no skip' },
  { stage: 'Frontend attempt2', error: 'Forking duplicate literal1 corrupted a generated placeholder; single-stat binding count omitted nine additions', resolution: 'One-pass source-backed numeric substitution, exact reconstruction assertion; retained canonical values and full tests' },
  { stage: 'next-candidate review', error: 'Flattened HTML joined Level55 and implicit100 into55100', resolution: 'Original inventory preserved, reviewed candidates verify source HTML boundaries' },
  { stage: 'GGG provenance snapshot', error: 'HTTP403 from collector', resolution: 'Golden Obi raw snapshot preserved; official patch page verified by public web reader; no retry overwrites' },
  { stage: 'Frontend wrapper', error: 'Trailing blank CR row after all required commands completed returned exit1', resolution: 'Each required command success verified in preserved log, including1833 tests and production build' },
  { stage: 'API attempt1', error: 'Abyss requiredItemLevel1 generated invalid ilvl0 fixture but expected200', resolution: 'Assert minimum1 crafting succeeds and ilvl0 returns422 Problem Details; other minimum boundaries remain atomic state-equal' },
  { stage: 'API attempt2', error: 'host.docker.internal ConnectTimeout', resolution: 'QA app health UP verified; same assertions run on private Compose network into new attempt3;1248 checks passed' },
  { stage: 'Implicit attempt1', error: 'Historical bucket fixture omitted nullable concrete fields', resolution: 'Apply established concrete null-field adapter to Rawhide fixture; retain exact state equality and blocked Divine;84 checks passed' },
  { stage: 'Browser attempt1', error: 'Frontend decoder rejected verified Belt null quality cap after successful HTTP200 craft', resolution: 'Permit null only for reviewed Belt class; add13 actual-response accepts and13 forged-cap rejects; repeat full Frontend checks and build, then browser into new attempt2' },
  { stage: 'Frontend attempt4 formatter', error: 'Locked Prettier required second pass for a newly expanded method chain', resolution: 'Second pass on exact new test file; npm ci/lint/typecheck already passed, continue full format/test/build into new log5; formatter settings unchanged' },
]
fs.writeFileSync(`${out}/failure-history.json`, JSON.stringify({ failures }, null, 2) + '\n')
for (const [from, to] of [['api-attempt-3/api-results.json', 'api-results.json'], ['browser-attempt-2/browser-results.json', 'browser-results.json'], ['browser-attempt-2/old-filled-results.json', 'old-filled-results.json'], ['implicit-attempt-2/results.json', 'implicit-results.json']]) fs.copyFileSync(`${q}/${from}`, `${out}/${to}`)
fs.writeFileSync(`${out}/screenshots.json`, JSON.stringify({ visuallyReviewedContactSheets: 54, files: pngs.map(p => ({ file: p.replaceAll('\\', '/'), sha256: hash(p) })) }, null, 2) + '\n')
const validation = { passed: true, totalBases: 91, addedBases: Object.keys(JSON.parse(fs.readFileSync('frontend/src/features/crafting/topBases.json'))).filter(k => k.endsWith('-belt')), backendUnitArchitecture: count('test'), backendIntegration: count('integrationTest'), frontendTests: 1859, apiChecks: api.count, browserChecks: browser.count, implicitChecks: implicit.count, oldFilledLegacyChecks: legacy.count, browserErrors: browser.errors, matchingValidatedSourceFiles: sourceFiles, screenshots: 324, visuallyReviewedContactSheets: 54, jarSha256: hash(`${q}/poe2craft.jar`), qaRoot: q, qaProject: 'exile-belts-20261005', qaPorts: [20380, 20381], successfulLogs: ['backend-check-2.log', 'frontend-check-4.log', 'frontend-check-5.log'], preservation: 'Existing78 identities/data/translations/API initials/films, registry220/deferred50, Solar-only Support/Explorer, quality overflow/HALF_UP, Shift/Alt and orange preview', remainingScope: 'Crossbow/melee/Staff/Talisman/Shields/Bucklers/Foci/Quivers: bounded source-backed next equipment plan' }
fs.writeFileSync(`${out}/validation.json`, JSON.stringify(validation, null, 2) + '\n')
console.log(validation)
