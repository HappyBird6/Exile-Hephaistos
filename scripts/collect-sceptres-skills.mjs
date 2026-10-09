import fs from 'node:fs'
import assert from 'node:assert/strict'
import { cleanSource as clean } from './armour-source.mjs'
const root='docs/evidence/sceptres-source-bundle-2026-10-05'
const roster=JSON.parse(fs.readFileSync(`${root}/verification.json`)).bases
const identities=[]
for(const b of roster) {
 const response=await fetch(b.skillUrl)
 assert(response.ok)
 const html=await response.text()
 fs.writeFileSync(`${root}/${b.skill.replaceAll(' ','_')}.us.html`,html,{flag:'wx'})
 const fields=[...html.matchAll(/<tr><td>([^]*?)<\/td><td>([^]*?)<\/td><\/tr>/g)].map(m=>[clean(m[1]),clean(m[2])])
 const ids=fields.filter(([key])=>key==='Id')
 assert(ids.length,b.skill)
 identities.push({...b,fields,displayOnly:true})
}
fs.writeFileSync(`${root}/skill-identities.json`,JSON.stringify(identities,null,2)+'\n',{flag:'wx'})
console.log(identities.map(b=>[b.skill,b.fields.filter(([k])=>k==='Id')]))
