import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/base-registry-qa-20261005'
const report={command:'sh gradlew --no-daemon spotlessApply check generateJooq bootJar',attempt:2}
assert(fs.readFileSync(`${q}/backend-check-2.log`,'utf8').includes('BUILD SUCCESSFUL'))
for(const suite of ['test','integrationTest']) {
 const counts={tests:0,failures:0,errors:0,skipped:0}
 for(const file of fs.readdirSync(`${q}/backend-check/build/test-results/${suite}`).filter(f=>f.endsWith('.xml'))) {
  const header=fs.readFileSync(`${q}/backend-check/build/test-results/${suite}/${file}`,'utf8').match(/<testsuite [^>]+>/)[0]
  for(const key of Object.keys(counts))counts[key]+=Number(header.match(new RegExp(`${key}="(\\d+)"`))[1])
 }
 assert.equal(counts.failures+counts.errors+counts.skipped,0)
 report[suite]=counts
}
function sync(path,relative='') {
 for(const entry of fs.readdirSync(path,{withFileTypes:true})) {
  const next=relative?relative+'/'+entry.name:entry.name
  if(entry.isDirectory())sync(path+'/'+entry.name,next)
  else if(entry.name.endsWith('.java')) {
   const tested=fs.readFileSync(path+'/'+entry.name),target='backend/src/'+next
   assert(fs.existsSync(target),target)
   if(!tested.equals(fs.readFileSync(target)))fs.writeFileSync(target,tested)
  }
 }
}
sync(`${q}/backend-check/src`)
fs.writeFileSync('docs/evidence/base-registry-refactor-2026-10-05/backend-results.json',JSON.stringify(report,null,2)+'\n',{flag:'wx'})
console.log(report)
