import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/quarterstaves-spears-qa-20261005'
const results={command:'sh gradlew --no-daemon spotlessApply check generateJooq bootJar'}
assert(fs.readFileSync(`${q}/backend-check-1.log`,'utf8').includes('BUILD SUCCESSFUL'))
for(const suite of ['test','integrationTest']) {
  const root=`${q}/backend-check/build/test-results/${suite}`
  const counts={tests:0,failures:0,errors:0,skipped:0}
  for(const f of fs.readdirSync(root).filter(f=>f.endsWith('.xml'))) {
    const header=fs.readFileSync(`${root}/${f}`,'utf8').match(/<testsuite [^>]+>/)[0]
    for(const key of Object.keys(counts))counts[key]+=+header.match(new RegExp(`${key}="(\\d+)"`))[1]
  }
  assert.equal(counts.failures+counts.errors+counts.skipped,0)
  results[suite]=counts
}
fs.writeFileSync('docs/evidence/quarterstaves-spears-source-bundle-2026-10-05/backend-results.json',JSON.stringify(results,null,2)+'\n',{flag:'wx'})
console.log(results)
