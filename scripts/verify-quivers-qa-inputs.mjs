import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { execFileSync } from 'node:child_process'

const qa = 'E:/WORK/Exile-Hephaistos/codex/quivers-qa-20261005'
const digest = p => crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex')
const tracked = execFileSync('git', ['ls-files'], { encoding: 'utf8' }).trim().split('\n')
const added = execFileSync('git', ['ls-files', '--others', '--exclude-standard'], { encoding: 'utf8' }).trim().split('\n')
const files = [...new Set([...tracked, ...added])].filter(p => /^(backend\/src\/|frontend\/src\/)/.test(p)
  || /^(backend\/(build.gradle|settings.gradle|gradle.properties|gradlew)|frontend\/(package.*\.json|.*config.*))$/.test(p))
for (const p of files) {
  const [area, ...rest] = p.split('/')
  assert.equal(digest(p), digest(`${qa}/${area}-check/${rest.join('/')}`), p)
}
const jar = `${qa}/poe2craft.jar`
assert.equal(digest(jar), digest(`${qa}/backend-check/build/libs/poe2craft.jar`))
const distRoot = `${qa}/frontend-check/dist`
const dist = fs.readdirSync(distRoot, { recursive: true }).filter(name => fs.statSync(`${distRoot}/${name}`).isFile()).sort().map(name => ({ name, sha256: digest(`${distRoot}/${name}`) }))
const report = { passed: true, sourceFiles: files.length, jarSha256: digest(jar), dist }
fs.writeFileSync(`${qa}/${process.argv[2] ?? 'input-equivalence.json'}`, JSON.stringify(report, null, 2) + '\n', { flag: 'wx' })
console.log({ passed: true, sourceFiles: files.length, jarSha256: report.jarSha256, distFiles: dist.length })
