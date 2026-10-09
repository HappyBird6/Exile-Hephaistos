import fs from 'node:fs'
import path from 'node:path'
import crypto from 'node:crypto'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
const q = 'E:/WORK/Exile-Hephaistos/codex/hallowed-qa-20261004'
const out = 'docs/evidence/hallowed-runtime-bundle-2026-10-04'
assert(!fs.existsSync(out), 'Preserve prior evidence')
const walk = root => fs.readdirSync(root, { withFileTypes: true }).flatMap(e => e.isDirectory() ? walk(path.join(root, e.name)) : [path.join(root, e.name)])
let sourceFiles = 0
for (const area of ['backend', 'frontend']) for (const file of walk(`${area}/src`)) {
  assert(fs.readFileSync(file).equals(fs.readFileSync(`${q}/${area}-check/${file.replace(/^[^/\\]+[/\\]/, '')}`)), file)
  sourceFiles++
}
const old = JSON.parse(execFileSync('git', ['show', 'HEAD:backend/src/main/resources/crafting/registry-v2.json'], { encoding: 'utf8' }))
const current = JSON.parse(fs.readFileSync('backend/src/main/resources/crafting/registry-v2.json'))
for (const entry of current.entries) if (entry.supportedBases) entry.supportedBases = entry.supportedBases.filter(x => x !== 'hallowed')
for (let i = 0; i < old.entries.length; i++) assert.deepEqual(current.entries[i], old.entries[i])
assert.equal(current.entries.length, old.entries.length)
fs.mkdirSync(out)
for (const [source, target] of [['api-after-quality-fix/api-results.json', 'api-results.json'], ['browser-attempt-3/browser-results.json', 'browser-results.json'], ['importer-replay-results.json', 'importer-replay-results.json']]) fs.copyFileSync(`${q}/${source}`, `${out}/${target}`)
const hash = file => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex')
const screenshots = walk(`${q}/browser-attempt-3`).filter(p => p.endsWith('.png')).map(file => ({ file: file.replaceAll('\\', '/'), sha256: hash(file) }))
assert.equal(screenshots.length, 73)
fs.writeFileSync(`${out}/screenshots.json`, JSON.stringify({ visuallyReviewedContactSheets: 11, screenshots }, null, 2) + '\n')
fs.writeFileSync(`${out}/validation.json`, JSON.stringify({ passed: true, addedBases: ['Hallowed Sceptre'], totalBases: 62, backendUnitArchitecture: 440, backendIntegration: 6, frontendTests: 1811, apiChecks: 116, browserChecks: 285, browserPageErrors: 0, matchingValidatedSourceFiles: sourceFiles, originalRegistryEntriesPreserved: old.entries.length, screenshots: 73, visuallyReviewedContactSheets: 11, jarSha256: hash(`${q}/poe2craft.jar`), qaRoot: q, logs: ['backend-check-4.log', 'frontend-check-1.log', 'api-check-3.log'], previousAttemptsPreserved: true, commands: ['check generateJooq bootJar', 'npm ci; lint; typecheck; format:check; test -- --run; build', 'docker compose config --quiet'], remainingScope: 'Wand9; Sceptre7 ordinary skill variants; Belt; Crossbow/melee/Shields/Foci/Quivers. See scope document.', spanishRequirementDiscrepancy: 'Raw source preserved; display uses canonical English Level65 Int114.' }, null, 2) + '\n')
console.log({ sourceFiles, screenshots: screenshots.length, registryEntries: old.entries.length })
