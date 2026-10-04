import fs from 'node:fs'
const q='E:/WORK/Exile-Hephaistos/codex/amulets-qa-20261004'
for(const d of ['test','integrationTest']){
 let tests=0,failures=0,skipped=0
 for(const n of fs.readdirSync(`${q}/backend-check/build/test-results/${d}`).filter(x=>x.endsWith('.xml'))){
 const s=fs.readFileSync(`${q}/backend-check/build/test-results/${d}/${n}`,'utf8')
 tests+=Number(s.match(/tests="(\d+)"/)[1]);failures+=Number(s.match(/failures="(\d+)"/)[1]);skipped+=Number(s.match(/skipped="(\d+)"/)[1])
 }
 console.log(d,{tests,failures,skipped})
}
for(const n of ['api-results.json','material-paths-results.json']){const j=JSON.parse(fs.readFileSync(`${q}/${n}`));console.log(n,Object.keys(j),j.count,j.passed)}
