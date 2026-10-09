import fs from 'node:fs'
const q='E:/WORK/Exile-Hephaistos/codex/base-registry-qa-20261005'
let s=fs.readFileSync(`${q}/qa-old-filled-1.cjs`,'utf8').replaceAll('baseline119-api-initials.json','baseline125-api-initials.json')
const bases=JSON.parse(fs.readFileSync('frontend/src/features/crafting/topBases.json'))
const seen=new Set()
const keys=['solar','stocky','sapphire','time-lost-sapphire',...Object.entries(bases).filter(([,v])=>{if(seen.has(v.family))return false;seen.add(v.family);return true}).map(([k])=>k)]
s=s.replace("['wand','sceptre','hallowed']",JSON.stringify(keys)).replace('d.stats.length > 1','d.stats.length >= 1').replaceAll('real compound legacy fixture','real sourced legacy fixture')
s=s.replace("await page.screenshot({path:'/evidence/legacy-'","if(l==='en')await page.screenshot({path:'/evidence/legacy-'")
fs.writeFileSync(`${q}/qa-crossclass-filled-1.cjs`,s,{flag:'wx'})
console.log(keys.length,'crossclass sourced filled films, six locales')
