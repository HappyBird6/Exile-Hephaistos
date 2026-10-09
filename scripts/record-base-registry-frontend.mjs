import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/base-registry-qa-20261005'
const log=fs.readFileSync(`${q}/frontend-check-1.log`,'utf8').replace(/\u001b\[[0-9;]*m/g,'')
assert(log.includes('built in'))
const report={passed:true,commands:['npm ci','npm run lint','npm run typecheck','npm run format:check','npm run test -- --run','npm run build'],testFiles:Number(log.match(/Test Files\s+(\d+) passed/)[1]),tests:Number(log.match(/Tests\s+(\d+) passed/)[1])}
assert.equal(report.testFiles,69);assert.equal(report.tests,1917)
function sync(path,relative='') {
 for(const entry of fs.readdirSync(path,{withFileTypes:true})) {
  const next=relative?relative+'/'+entry.name:entry.name
  if(entry.isDirectory())sync(path+'/'+entry.name,next)
  else {
   const tested=fs.readFileSync(path+'/'+entry.name),target='frontend/src/'+next
   assert(fs.existsSync(target),target)
   if(!tested.equals(fs.readFileSync(target)))fs.writeFileSync(target,tested)
  }
 }
}
sync(`${q}/frontend-check/src`)
fs.writeFileSync('docs/evidence/base-registry-refactor-2026-10-05/frontend-results.json',JSON.stringify(report,null,2)+'\n',{flag:'wx'})
console.log(report)
