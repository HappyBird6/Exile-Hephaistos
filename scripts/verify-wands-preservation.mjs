import fs from 'node:fs'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
const old = p => JSON.parse(execFileSync('git',['show',`25131b190fd242f90b51051bc87ae1e65936ae29:${p}`],{maxBuffer:32*1024*1024,encoding:'utf8'}))
const read = p => JSON.parse(fs.readFileSync(p,'utf8'))
const keys = ['bone','siphoning','volatile','galvanic','acrid','offering','critical','primordial','dueling']
for(const p of ['backend/src/main/resources/catalog/top-bases.json','frontend/src/features/crafting/topBases.json','backend/src/main/resources/catalog/top-base-essences.json','frontend/src/features/crafting/topBaseEssences.json']){
  const baseline=old(p), current=read(p)
  for(const key of Object.keys(baseline))assert.deepEqual(current[key],baseline[key],`${p}:${key}`)
  assert.equal(Object.keys(current).length,Object.keys(baseline).length+9)
}
const p='frontend/src/shared/i18n/gameTerms.json',baseline=old(p),current=read(p)
for(const locale of Object.keys(baseline))for(const key of Object.keys(baseline[locale]))assert.deepEqual(current[locale][key],baseline[locale][key],`${locale}:${key}`)
const rp='backend/src/main/resources/crafting/registry-v2.json',prior=old(rp),registry=read(rp)
assert.equal(registry.entries.length,prior.entries.length)
for(let i=0;i<registry.entries.length;i++){
 const entry=structuredClone(registry.entries[i]);if(entry.supportedBases)entry.supportedBases=entry.supportedBases.filter(k=>!keys.includes(k))
 assert.deepEqual(entry,prior.entries[i],entry.id)
}
for(const key of Object.keys(prior.workbenchBases))assert.deepEqual(registry.workbenchBases[key],prior.workbenchBases[key])
assert.equal(registry.entries.filter(e=>e.serviceScope === 'DEFERRED').length,50)
for(const name of ['Bone','Siphoning','Volatile','Galvanic','Acrid','Offering','Critical','Primordial','Dueling']){
 const source=read(`docs/evidence/wands-source-bundle-2026-10-05/${name}_Wand.us.json`)
 const priorSource=read(`docs/evidence/amulets-source-bundle-2026-10-04/next-roster/${name}_Wand.json`).variants
 assert(priorSource.some(v=>v.fields.Type === source.fields.Type && v.fields.BaseType === source.name),name)
 assert.equal(source.fields['Quality.max_quality'],'20')
}
console.log('All existing base data, Essence maps, translations, registry entries and deferred50 preserved')
