import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
const read = p => JSON.parse(fs.readFileSync(p, 'utf8'))
const hash = p => crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex')
const attempt = process.argv[2] ?? '1'
assert(/^[1-9][0-9]*$/.test(attempt))
const browserRoot = `/qa/browser-attempt-${attempt}`
const api = read('/qa/api-results.json'), browser = read(`${browserRoot}/browser-results.json`)
assert(api.passed && api.initialBases === 28)
assert(browser.passed && browser.errors.length === 0)
let backendTests = 0
for (const suite of ['test', 'integrationTest']) for (const file of fs.readdirSync(`/qa/backend-check/build/test-results/${suite}`).filter(f => f.endsWith('.xml'))) {
  const header = fs.readFileSync(`/qa/backend-check/build/test-results/${suite}/${file}`, 'utf8').match(/<testsuite\b[^>]*>/)[0]
  for (const key of ['failures', 'errors', 'skipped']) assert.equal(Number(header.match(new RegExp(`${key}="(\\d+)"`))[1]), 0, `${suite}/${file}/${key}`)
  backendTests += Number(header.match(/tests="(\d+)"/)[1])
}
assert.equal(backendTests, 411)
const frontendLog = fs.readFileSync('/qa/frontend-check-1.log', 'utf8').replace(/\u001b\[[0-9;]*m/g, '')
assert(/Tests\s+1768 passed/.test(frontendLog), 'Complete frontend suite required')
const added = ['polished', 'blacksteel-gloves', 'war-wraps', 'freebooter', 'gladiatorial', 'grinning']
for (const key of added) {
  for (const locale of ['en', 'ko', 'ja', 'zh-CN', 'zh-TW', 'es']) for (const width of [1440, 390]) assert(fs.existsSync(`${browserRoot}/${key}-${locale}-${width}.png`))
  assert(fs.existsSync(`${browserRoot}/${key}-orange-preview.png`))
}
for (const key of ['stocky', 'body', 'helmet', 'massive', 'sirenscale', 'adherent', 'soldier', 'imperial']) assert(fs.existsSync(`${browserRoot}/old-film-${key}.png`))
const coverage = read('/qa/display-coverage.json')
assert.equal(coverage.catalogDefinitions, 2444)
assert.equal(coverage.compoundBindings, 290)
const failureHistory = [
  { stage: 'source', outcome: 'failed then resolved', reason: 'Guessed EvasionAppliesToDeflection Code returned empty detail; exact original source-ID detail proves stat/range/locality/spawn order. Five suffix rows retained.' },
  { stage: 'source', outcome: 'failed then resolved', reason: 'Helmet slot source tag is helmet, not plural helmets. Corrected slot alias without weakening extra-tag assertion.' },
  { stage: 'backend-check-1', outcome: 'compile failed', reason: 'New test passed display StateBucket to concrete ItemState API. Concrete fixture added; log retained.' },
  { stage: 'backend-check-2', outcome: '405 completed, 3 failed', reason: 'Helmet cold recoup routed to Ice rather than source-listed Thawing. Importer now derives action from source Name; all assertions retained. XML and log preserved.' },
  { stage: 'review', outcome: 'resolved', reason: 'New Helmet basic and Gloves Infinite support markers were missing. Added only source-backed new base IDs; historical support and deferred 50 unchanged.' },
  { stage: 'api-check-1', outcome: 'positive crafting paths passed; registry identity failed', reason: 'Windows final shell-script line produced a carriage-return suffix on copied JAR filename. Preserved earlier JAR and failure log, copied final built JAR to intended QA filename, restarted only isolated QA app. API check 2 passed all 397 assertions.' },
]
const result = { passed: true, baseHead: '1c8fa7d3294f5ffdc86e2325dd263588aa38ece6', added, totalBases: 28, remainingArmour: 13, backendTests, frontendTests: 1768, apiAssertions: api.count, browserChecks: browser.count, screenshots: 86, browserAttempt: attempt, jarSha256: hash('/qa/poe2craft.jar'), coverage, failureHistory }
assert(!fs.existsSync('/qa/validation.json'), 'Preserve validation output')
fs.writeFileSync('/qa/validation.json', JSON.stringify(result, null, 2) + '\n')
console.log(JSON.stringify({ backendTests, frontendTests: 1768, apiAssertions: api.count, browserChecks: browser.count, screenshots: 86 }))
