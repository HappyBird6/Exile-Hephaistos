import fs from 'node:fs'
import crypto from 'node:crypto'
const q='E:/WORK/Exile-Hephaistos/codex/quivers-qa-20261005'
const log=fs.readFileSync(`${q}/frontend-check-1.log`)
fs.writeFileSync(`${q}/frontend-attempt-1-failure.json`,JSON.stringify({phase:'pretest source coverage',result:'Broadhead source has no character requirement; generic nonblank assertion rejected it',lint:true,typecheck:true,formatCheck:true,testsExecuted:false,buildExecuted:false,logSha256:crypto.createHash('sha256').update(log).digest('hex'),correction:'Exact source absence check and requirement0; generation itemLevel1 unchanged; no invented requirement, skip or relaxed generic coverage'},null,2)+'\n',{flag:'wx'})
for(const p of ['src/main/resources/catalog/top-bases.json','src/main/resources/crafting/registry-v2.json'])fs.copyFileSync(`backend/${p}`,`${q}/backend-check/${p}`)
fs.cpSync('frontend',`${q}/frontend-check`,{recursive:true,filter:p=>!p.split(/[\\/]/).some(s=>['node_modules','dist'].includes(s))})
fs.copyFileSync('scripts/check-display-coverage.mjs',`${q}/scripts/check-display-coverage.mjs`)
fs.writeFileSync(`${q}/backend-check-3.sh`,fs.readFileSync(`${q}/backend-check-1.sh`,'utf8').replaceAll('backend-check-1','backend-check-3'),{flag:'wx'})
fs.writeFileSync(`${q}/frontend-check-2.sh`,fs.readFileSync(`${q}/frontend-check-1.sh`,'utf8').replaceAll('frontend-check-1','frontend-check-2'),{flag:'wx'})
