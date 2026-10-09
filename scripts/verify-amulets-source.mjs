import fs from 'node:fs'
import assert from 'node:assert/strict'
import {cleanSource as clean} from './armour-source.mjs'
const root='docs/evidence/amulets-source-bundle-2026-10-04'
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'))
const old=read('backend/src/main/resources/catalog/solar-amulet/catalog.json'),raw=read('backend/src/main/resources/catalog/solar-amulet/base.raw.json')
const display=read('frontend/src/shared/i18n/modifierTemplates.json'),manifest=read('backend/src/main/resources/catalog/top-bases.json')
const checks=[]
for(let i=0;i<raw.length;i++){
 const proof=read(`${root}/full-details/${i}.json`),d=old.modifiers.find(d=>d.sourceUrl===proof.row.hover)
 assert.deepEqual(proof.row,raw[i]);assert(d)
 const spawn=[...proof.html.matchAll(/class=['"]badge bg-primary['"]>([^<]+): (\d+)<\/span>/g)].map(m=>({tag:m[1],weight:+m[2]}))
 const first=spawn.find(s=>['amulet','default'].includes(s.tag));assert(first && first.tag==='amulet' && first.weight>0,d.id)
 const stats=[...proof.html.matchAll(/<li>([^<]*?) <span class="badge bg-primary">([^]*?)<\/span> <span class="badge bg-secondary">([^]*?)<\/span><\/li>/g)].map(m=>{const n=clean(m[2]).match(/-?\d+/g).map(Number);return {id:clean(m[1]).replace(/ /g,'_'),min:n[0],max:n[1]??n[0]}})
 assert.deepEqual(stats,d.stats,d.id+' exact source stats')
 assert.equal(d.weight,+proof.row.DropChance)
}
checks.push('209 exact full stat tables and ordered Amulet eligibility; published weights retained separately')
for(const locale of ['en','ko','ja','zh-CN','zh-TW','es']){
 const rows=read(`${root}/Amulets.${locale}.json`).data.normal
 assert.equal(rows.length,raw.length)
 for(let i=0;i<raw.length;i++){
  const d=old.modifiers.find(d=>d.sourceUrl===raw[i].hover),binding=display.definitions[d.id]
  assert.equal(rows[i].Level,raw[i].Level);assert.deepEqual(rows[i].ModFamilyList,raw[i].ModFamilyList)
  const text=display.templates[locale][binding.template].template.replace(/\{v(\d+)\}/g,(_,n)=>binding.values[+n])
  assert.equal(text,clean(rows[i].str),locale+':'+d.id)
 }
 checks.push(locale+' source-matched209 ordinary templates')
}
for(const [key,b] of Object.entries(manifest).filter(([,b])=>b.family==='amulets')){
 const c=read(`backend/src/main/resources/catalog/${b.pool}/catalog.json`)
 assert.equal(c.modifiers.filter(d=>d.weight>0).length,209)
 assert(!c.modifiers.some(d=>d.id==='solar-amulet:implicit:spirit'))
 for(const d of c.modifiers){const binding=display.definitions[d.id];assert(binding && binding.englishText===d.text);assert.deepEqual(binding.stats,d.stats);for(const l of ['en','ko','ja','zh-CN','zh-TW','es'])assert(display.templates[l][binding.template])}
 checks.push(key+' complete pool and source-specific implicit, six locale coverage')
}
const output=`${root}/verification.json`;assert(!fs.existsSync(output),'Preserve output')
fs.writeFileSync(output,JSON.stringify({passed:true,checks,count:checks.length},null,2)+'\n');console.log(checks)
