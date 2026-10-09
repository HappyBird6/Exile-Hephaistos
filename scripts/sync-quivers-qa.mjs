import fs from 'node:fs'
import {execFileSync} from 'node:child_process'
const q='E:/WORK/Exile-Hephaistos/codex/quivers-qa-20261005'
const files=execFileSync('git',['diff','--name-only'],{encoding:'utf8'}).trim().split('\n')
for(const p of [...files,'backend/src/main/java/com/poe2craft/item/ReviewedQuivers.java','backend/src/test/java/com/poe2craft/crafting/ReviewedQuiversTest.java'].filter(p=>p.startsWith('backend/')&&p.endsWith('.java')))fs.copyFileSync(`${q}/backend-check/${p.slice(8)}`,p)
for(const n of ['backend','frontend'])fs.cpSync(n,`${q}/${n}-check`,{recursive:true,filter:p=>!p.split(/[\\/]/).some(s=>['node_modules','build','.gradle','dist'].includes(s))})
fs.writeFileSync(`${q}/backend-check-2.sh`,fs.readFileSync(`${q}/backend-check-1.sh`,'utf8').replaceAll('backend-check-1','backend-check-2'),{flag:'wx'})
console.log('Final resources and formatted Backend copied; prior output preserved')
