import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
const root='docs/evidence/staves-talismans-source-bundle-2026-10-05'
const url='https://www.pathofexile.com/api/trade2/data/items'
const response=await fetch(url);assert(response.ok,String(response.status))
const text=await response.text(),data=JSON.parse(text),entries=data.result.flatMap(g=>g.entries??[])
const names=['Permafrost Staff','Reflecting Staff','Dark Staff','Ravenous Staff','Perching Staff','Sanctified Staff','Maji Talisman','Fungal Talisman','Jade Talisman']
const matches=names.map(name=>({name,entries:entries.filter(e=>e.type===name&&!e.name)}))
assert(matches.every(m=>m.entries.length),'GGG ordinary unnamed trade entry required for every selected base')
fs.writeFileSync(`${root}/ggg-trade-items.json`,text,{flag:'wx'})
fs.writeFileSync(`${root}/availability.json`,JSON.stringify({url,retrievedAt:new Date().toISOString(),sha256:crypto.createHash('sha256').update(text).digest('hex'),matches,scope:'GGG ordinary unnamed trade base entries plus current source normal popup and rarity flags; exact drop sources and rates are not inferred'},null,2)+'\n',{flag:'wx'})
console.log(matches)
