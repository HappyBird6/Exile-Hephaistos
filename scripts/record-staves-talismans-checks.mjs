import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/staves-talismans-qa-20261005',root='docs/evidence/staves-talismans-source-bundle-2026-10-05'
const phase=process.argv[2];assert(['backend','frontend'].includes(phase))
function sync(path,relative='') { for(const entry of fs.readdirSync(path,{withFileTypes:true})) {const next=relative?relative+'/'+entry.name:entry.name;if(entry.isDirectory())sync(path+'/'+entry.name,next);else {const target=`${phase}/src/${next}`,tested=fs.readFileSync(path+'/'+entry.name);assert(fs.existsSync(target));if(!tested.equals(fs.readFileSync(target)))fs.writeFileSync(target,tested)}} }
const log=fs.readFileSync(`${q}/${phase}-check-${phase==='backend'?1:3}.log`,'utf8').replace(/\u001b\[[0-9;]*m/g,'')
let report
if(phase==='backend') {
 assert(log.includes('BUILD SUCCESSFUL'));report={command:'sh gradlew --no-daemon spotlessApply check generateJooq bootJar',passed:true}
 for(const suite of ['test','integrationTest']) {const counts={tests:0,failures:0,errors:0,skipped:0};for(const file of fs.readdirSync(`${q}/backend-check/build/test-results/${suite}`).filter(f=>f.endsWith('.xml'))) {const header=fs.readFileSync(`${q}/backend-check/build/test-results/${suite}/${file}`,'utf8').match(/<testsuite [^>]+>/)[0];for(const key of Object.keys(counts))counts[key]+=+header.match(new RegExp(`${key}="(\\d+)"`))[1]}assert.equal(counts.failures+counts.errors+counts.skipped,0);report[suite]=counts}
} else {assert(log.includes('built in'));report={passed:true,commands:['npm ci','npm run lint','npm run typecheck','npm run format:check','npm run test -- --run','npm run build'],testFiles:+log.match(/Test Files\s+(\d+) passed/)[1],tests:+log.match(/Tests\s+(\d+) passed/)[1]}}
sync(`${q}/${phase}-check/src`)
fs.writeFileSync(`${root}/${phase}-results.json`,JSON.stringify(report,null,2)+'\n',{flag:'wx'})
console.log(report)
