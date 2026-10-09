import fs from 'node:fs'
import assert from 'node:assert/strict'
const root='E:/WORK/Exile-Hephaistos/codex/amulets-qa-20261004'
let s=fs.readFileSync(`${root}/qa-browser-4.cjs`,'utf8')
s=s.replace("await seed(magic, 'full-suffix-' + k)","const fire=defs.find(d=>d.id==='amulet:suffix:of-the-salamander'); check(k+' source Fire fixture exists',Boolean(fire)); await seed({...magic,explicits:[{modifierId:fire.id,values:Object.fromEntries(fire.stats.map(s=>[s.id,s.min])),fractured:false}]}, 'conflicting-fire-' + k)")
s=s.replace('source Fire suffix refuses occupied Magic suffix atomically','source Fire family conflict refuses atomically')
assert(!fs.existsSync(`${root}/qa-browser-5.cjs`))
fs.writeFileSync(`${root}/qa-browser-5.cjs`,s)
fs.mkdirSync(`${root}/browser-attempt-4`)
for(const f of ['api-initials.json','baseline-api-initials.json'])fs.copyFileSync(`${root}/${f}`,`${root}/browser-attempt-4/${f}`)
