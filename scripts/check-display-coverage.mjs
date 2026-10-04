// Completeness is based on active catalog identities, not matching UI key counts.
import fs from 'node:fs'
import assert from 'node:assert/strict'
import { fileURLToPath } from 'node:url'
process.chdir(process.env.HEPHAISTOS_TRANSLATION_ROOT ?? fileURLToPath(new URL('..', import.meta.url)))
const locales=['en','ko','zh-CN','zh-TW','ja','es']
const registry=JSON.parse(fs.readFileSync('backend/src/main/resources/crafting/registry-v2.json','utf8'))
const terms=JSON.parse(fs.readFileSync('frontend/src/shared/i18n/gameTerms.json','utf8'))
const mods=JSON.parse(fs.readFileSync('frontend/src/shared/i18n/modifierTemplates.json','utf8'))
const results={locales:{},catalogDefinitions:0,compoundBindings:0}
const defs={}
for(const dir of fs.readdirSync('backend/src/main/resources/catalog')) {
 const root='backend/src/main/resources/catalog/'+dir
 if(!fs.statSync(root).isDirectory())continue
 for(const f of fs.readdirSync(root).filter(f=>f==='catalog.json'||f.endsWith('.catalog.json'))) {
  const c=JSON.parse(fs.readFileSync(root+'/'+f,'utf8'))
  if(c.base)for(const l of locales)assert(Object.values(terms[l]).some(t=>t.itemKey===c.base.id),`${l}:base:${c.base.id}`)
  for(const d of c.modifiers??[])defs[d.id]=d
 }
}
for(const base of Object.values(JSON.parse(fs.readFileSync('backend/src/main/resources/catalog/top-bases.json','utf8')))) {
 for(const l of locales) {
  assert.equal(terms[l][base.slug]?.itemKey,base.id,`${l}:top-base:${base.id}`)
  if(['Metadata/Items/Amulets/FourAmulet1','Metadata/Items/Amulets/FourAmulet2'].includes(base.id)) {
   assert.equal(base.requiredLevel,0,`${l}:source has no character requirement:${base.id}`)
   assert.equal(base.requirements[l],'',`${l}:no invented requirement:${base.id}`)
   const sourceLocale={en:'us',ko:'kr','zh-CN':'cn','zh-TW':'tw',ja:'jp',es:'sp'}[l]
   const source=JSON.parse(fs.readFileSync(`docs/evidence/amulets-source-bundle-2026-10-04/${base.slug}.${sourceLocale}.json`,'utf8'))
   assert.equal(source.fields.Type,base.id)
   assert.equal(source.requirements,base.requirements[l],`${l}:exact source absence:${base.id}`)
  } else assert(base.requirements[l],`${l}:top-base-requirements:${base.id}`)
 }
}
results.catalogDefinitions=Object.keys(defs).length
for(const[id,d]of Object.entries(defs)) {
 const b=mods.definitions[id]
 assert(b,`Missing active catalog binding: ${id}`)
 assert.equal(b.englishText,d.text,`Stale display source: ${id}`)
 assert.deepEqual(b.stats,d.stats??[],`Changed stat identity: ${id}`)
 for(const l of locales) {
  const t=mods.templates[l][b.template]
  assert(t?.template,`Missing active modifier locale: ${l}:${id}`)
  const placeholders=[...t.template.matchAll(/\{v(\d+)\}/g)].map(m=>+m[1])
  assert(placeholders.every(i=>i<b.values.length),`Unbound display value: ${l}:${id}`)
  assert.deepEqual([...new Set(placeholders)].sort(),[...new Set([...mods.templates.en[b.template].template.matchAll(/\{v(\d+)\}/g)].map(m=>+m[1]))].sort(),`Changed placeholders: ${l}:${id}`)
 }
 if(b.valueStats&&d.stats.length>1&&d.stats.some(s=>s.min!==s.max))results.compoundBindings++
}
for(const l of locales) {
 for(const [id,term] of Object.entries(terms[l]))assert(!(term.lines??[]).some(line=>/^&nbsp;?$/.test(line)),`${l}:HTML whitespace artifact:${id}`)
 const active=registry.entries.filter(e=>e.serviceScope!=='DEFERRED')
 const missing=active.filter(e=>!terms[l][e.id]?.name)
 // Deferred identities are explicit documented exceptions; newly active entries fail.
 assert.equal(missing.length,0,`${l}:missing active names:${missing.map(e=>e.id)}`)
 const exceptions=registry.entries.filter(e=>e.serviceScope==='DEFERRED'&&!terms[l][e.id]?.name)
 const liquid=registry.entries.filter(e=>e.category==='LIQUID_EMOTION'&&e.id!=='Liquid_Verisium')
 assert.equal(liquid.length,26)
 for(const e of liquid)assert(terms[l][e.id]?.lines.length,`${l}:Liquid tooltip:${e.id}`)
 results.locales[l]={names:registry.entries.filter(e=>terms[l][e.id]?.name).length,total:registry.entries.length,exceptions:exceptions.map(e=>e.id),liquidNamesAndDescriptions:liquid.length}
}
console.log(JSON.stringify(results,null,2))
