import fs from 'node:fs'
import assert from 'node:assert/strict'
import {execFileSync} from 'node:child_process'
const root='/qa/importer-replay-1';assert(!fs.existsSync(root),'Preserve replay output');fs.mkdirSync(root)
for(const p of ['backend/src/main/resources/catalog','backend/src/main/resources/crafting','frontend/src/shared/i18n','docs/evidence/amulets-source-bundle-2026-10-04'])fs.cpSync('/source/'+p,root+'/'+p,{recursive:true})
fs.mkdirSync(root+'/frontend/src/features/crafting',{recursive:true})
const files=['backend/src/main/resources/catalog/top-bases.json','backend/src/main/resources/catalog/top-base-essences.json','backend/src/main/resources/crafting/registry-v2.json','frontend/src/shared/i18n/gameTerms.json','frontend/src/shared/i18n/modifierTemplates.json']
const before=Object.fromEntries(files.map(p=>[p,JSON.parse(fs.readFileSync(root+'/'+p))]))
execFileSync(process.execPath,['/source/scripts/import-amulets-bundle.mjs'],{cwd:root})
const checks=[]
for(const p of files){assert.deepEqual(JSON.parse(fs.readFileSync(root+'/'+p)),before[p],p+' deterministic import');checks.push(p)}
const old=JSON.parse(fs.readFileSync('/qa/baseline-registry.json')),registry=before[files[2]]
assert.deepEqual(registry.entries.filter(e=>e.serviceScope==='DEFERRED'),old.entries.filter(e=>e.serviceScope==='DEFERRED'));checks.push('deferred50 exactly unchanged')
const prior=JSON.parse(fs.readFileSync('/qa/baseline-modifierTemplates.json')),display=before[files[4]],bases=before[files[0]]
const added=Object.keys(display.definitions).filter(id=>!Object.hasOwn(prior.definitions,id)).sort()
const keys=Object.keys(bases).filter(k=>bases[k].family==='amulets')
assert.deepEqual(added,[...keys.map(k=>bases[k].implicitModifierId),'amulet:suffix:essence-hysteria-life-recoup'].sort());checks.push('exact seven new implicits and Hysteria binding')
for(const key of keys){
 const c=JSON.parse(fs.readFileSync(root+'/backend/src/main/resources/catalog/'+bases[key].pool+'/catalog.json'))
 assert.equal(c.modifiers.length,218)
 assert.equal(c.modifiers.filter(d=>d.weight>0).length,209)
 assert.equal(c.modifiers.filter(d=>d.layer==='IMPLICIT').length,1)
 const implicit=c.modifiers.find(d=>d.layer==='IMPLICIT');assert.equal(implicit.id,bases[key].implicitModifierId)
 for(const d of c.modifiers){const b=display.definitions[d.id];assert(b && b.englishText===d.text);assert.deepEqual(b.stats,d.stats);for(const l of ['en','ko','ja','zh-CN','zh-TW','es'])assert(display.templates[l][b.template])}
 checks.push(key+' source identity and218 definitions/six locales')
}
fs.writeFileSync('/qa/importer-replay-results.json',JSON.stringify({passed:true,count:checks.length,checks,added},null,2)+'\n')
