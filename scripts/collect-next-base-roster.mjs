import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import {cleanSource as clean} from './armour-source.mjs'
const root='docs/evidence/amulets-source-bundle-2026-10-04/next-roster'
fs.mkdirSync(root,{recursive:true})
for(const page of ['Wands','Sceptres','Belts']){
 const roster=JSON.parse(fs.readFileSync(`docs/evidence/top-base-inventory-2026-10-04/${page}.json`))
 for(const slug of new Set(roster.cards.map(c=>c.slug))){
  const path=`${root}/${slug}.json`;if(fs.existsSync(path))continue
  const url=`https://poe2db.tw/us/${slug}`,r=await fetch(url);assert(r.ok);const html=await r.text()
  const sections=html.split(/(?=<div class="newItemPopup NormalPopup)/).slice(1)
  const variants=sections.map(section=>{
   const popup=section.match(/^<div class="newItemPopup NormalPopup[^]*?(?=<div class="itemboximage")/)?.[0]
   const fields=Object.fromEntries([...section.matchAll(/<tr><td>([^]*?)<\/td><td>([^]*?)<\/td><\/tr>/g)].map(m=>[clean(m[1]),clean(m[2])]))
   return {card:clean(popup??''),fields}
  }).filter(v=>v.fields.Type?.startsWith('Metadata/Items/'))
  assert(variants.length,slug)
  fs.writeFileSync(path,JSON.stringify({url,retrievedAt:new Date().toISOString(),sha256:crypto.createHash('sha256').update(html).digest('hex'),variants,html},null,2)+'\n')
 }
}
