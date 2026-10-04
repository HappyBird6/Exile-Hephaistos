import fs from 'node:fs'
import assert from 'node:assert/strict'
import {execFileSync} from 'node:child_process'
const root='/qa/importer-replay-1';assert(!fs.existsSync(root),'Preserve earlier replay output');fs.mkdirSync(root)
for(const p of ['backend/src/main/resources/catalog','backend/src/main/resources/crafting','frontend/src/shared/i18n','docs/evidence/rings-source-bundle-2026-10-04'])fs.cpSync('/source/'+p,root+'/'+p,{recursive:true})
fs.mkdirSync(root+'/frontend/src/features/crafting',{recursive:true})
const files=['backend/src/main/resources/catalog/top-bases.json','backend/src/main/resources/catalog/top-base-essences.json','backend/src/main/resources/crafting/registry-v2.json','frontend/src/shared/i18n/gameTerms.json','frontend/src/shared/i18n/modifierTemplates.json']
const before=Object.fromEntries(files.map(p=>[p,JSON.parse(fs.readFileSync(root+'/'+p))]))
execFileSync(process.execPath,['/source/scripts/import-rings-bundle.mjs'],{cwd:root})
const checks=[]
for(const p of files){assert.deepEqual(JSON.parse(fs.readFileSync(root+'/'+p)),before[p],p+' deterministic import');checks.push(p)}
const manifest=before[files[0]],display=before[files[4]],priorDisplay=JSON.parse(fs.readFileSync('/qa/baseline-modifierTemplates.json'))
const clean=s=>s.replace(/<br\s*\/?>/gi,'\n').replace(/<[^>]*>/g,'').replace(/&nbsp;/g,' ').replace(/&amp;/g,'&').trim()
const sourceRows=JSON.parse(fs.readFileSync(root+'/docs/evidence/rings-source-bundle-2026-10-04/Rings.en.json')).data.normal
const legacy=JSON.parse(fs.readFileSync(root+'/backend/src/main/resources/catalog/iron-ring/catalog.json'))
for(const locale of ['en','ko','ja','zh-CN','zh-TW','es']){
 const rows=JSON.parse(fs.readFileSync(root+'/docs/evidence/rings-source-bundle-2026-10-04/Rings.'+locale+'.json')).data.normal
 assert.equal(rows.length,203)
 for(let i=0;i<sourceRows.length;i++){
  const row=sourceRows[i],d=legacy.modifiers.find(d=>d.name===row.Name && d.requiredItemLevel===+row.Level && d.familyIds.join(',')===row.ModFamilyList.join(','));assert(d)
  assert.equal(rows[i].Level,row.Level);assert.deepEqual(rows[i].ModFamilyList,row.ModFamilyList)
  const b=display.definitions[d.id],template=display.templates[locale][b.template].template.replace(/\{v(\d+)\}/g,(_,n)=>b.values[+n])
  assert.equal(template,clean(rows[i].str),locale+':'+d.id+' exact sourced affix template')
 }
 checks.push(locale+' all203 affix templates exactly match fresh source')
}
const added=Object.keys(display.definitions).filter(id=>!Object.hasOwn(priorDisplay.definitions,id)).sort()
const keys=Object.keys(manifest).filter(k=>manifest[k].family==='rings')
assert.deepEqual(added,[...keys.map(k=>manifest[k].implicitModifierId),'ring:suffix:essence-hysteria-mana-regeneration'].sort());checks.push('Exact eight implicit IDs and one Hysteria display binding')
let ordinary=0
for(const k of keys){
 const c=JSON.parse(fs.readFileSync(root+'/backend/src/main/resources/catalog/'+manifest[k].pool+'/catalog.json'))
 assert.equal(c.modifiers.filter(d=>d.weight>0).length,203);ordinary+=203
 for(const d of c.modifiers){const b=display.definitions[d.id];assert(b && b.englishText===d.text);assert.deepEqual(b.stats,d.stats);for(const locale of ['en','ko','ja','zh-CN','zh-TW','es'])assert(display.templates[locale][b.template]?.template)}
 checks.push(k+' complete pool and six source-correlated locale templates')
}
assert.equal(keys.length,8)
const registry=before[files[2]],old=JSON.parse(fs.readFileSync('/qa/baseline-registry.json'))
assert.deepEqual(registry.entries.filter(e=>e.serviceScope==='DEFERRED'),old.entries.filter(e=>e.serviceScope==='DEFERRED'));checks.push('All fifty deferred entries exactly unchanged')
fs.writeFileSync('/qa/importer-replay-results.json',JSON.stringify({passed:true,count:checks.length,ordinary,addedDisplayIds:added,checks},null,2)+'\n')
console.log(checks.length+' replay assertions; '+ordinary+' ordinary rows verified')
