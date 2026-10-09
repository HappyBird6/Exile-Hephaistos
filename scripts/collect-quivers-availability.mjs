import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
const root='docs/evidence/quivers-source-bundle-2026-10-05'
const url='https://www.pathofexile.com/api/trade2/data/items'
const r=await fetch(url)
assert(r.ok,`${r.status}`)
const text=await r.text(),data=JSON.parse(text)
const entries=data.result.flatMap(g=>g.entries??[])
const names=['Visceral','Volant','Penetrating','Primed','Serrated','Toxic','Blunt','Two-Point','Sacral','Fire','Broadhead'].map(n=>n+' Quiver')
const matches=names.map(name=>({name,entries:entries.filter(e=>e.type===name&&!e.name)}))
assert(matches.every(m=>m.entries.length),'Ordinary GGG trade base entry required')
fs.writeFileSync(`${root}/ggg-trade-items.json`,text,{flag:'wx'})
fs.writeFileSync(`${root}/availability.json`,JSON.stringify({url,retrievedAt:new Date().toISOString(),sha256:crypto.createHash('sha256').update(text).digest('hex'),matches,scope:'GGG ordinary unnamed trade base entries plus current PoE2DB normal popup and normal/magic/rare rarity flags; does not infer exact drop sources or drop rates'},null,2)+'\n',{flag:'wx'})
console.log(matches)
