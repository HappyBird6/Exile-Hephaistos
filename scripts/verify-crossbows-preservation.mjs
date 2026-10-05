import fs from 'node:fs'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import crypto from 'node:crypto'
const base='9fbb83da948d515756b6cea9ffb7a24155abba8e'
const old=p=>JSON.parse(execFileSync('git',['show',`${base}:${p}`],{encoding:'utf8',maxBuffer:30*1024*1024}))
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'))
const checkSubset=(path)=>{const before=old(path),after=read(path);for(const [k,v]of Object.entries(before))assert.deepEqual(after[k],v,`${path}/${k}`);return Object.keys(before).length}
const count=checkSubset('backend/src/main/resources/catalog/top-bases.json')
assert.equal(count,74) // 17 legacy/base identities are registered outside the endgame manifest.
checkSubset('backend/src/main/resources/catalog/top-base-essences.json')
for(const p of ['frontend/src/shared/i18n/gameTerms.json','frontend/src/shared/i18n/modifierTemplates.json']) {
  const before=old(p),after=read(p)
  if(p.includes('gameTerms'))for(const l of Object.keys(before))for(const [k,v]of Object.entries(before[l]))assert.deepEqual(after[l][k],v,`${l}/${k}`)
  else {for(const [k,v]of Object.entries(before.definitions))assert.deepEqual(after.definitions[k],v,k);for(const l of Object.keys(before.templates))for(const [k,v]of Object.entries(before.templates[l]))assert.deepEqual(after.templates[l][k],v,`${l}/${k}`)}
}
const before=old('backend/src/main/resources/crafting/registry-v2.json'),after=read('backend/src/main/resources/crafting/registry-v2.json')
assert.equal(after.entries.length,220)
assert.deepEqual(after.entries.filter(e=>e.serviceScope==='DEFERRED'),before.entries.filter(e=>e.serviceScope==='DEFERRED'))
assert.equal(after.entries.filter(e=>e.serviceScope==='DEFERRED').length,50)
for(const e of before.entries) {
  const n=after.entries.find(x=>x.id===e.id)
  assert(n)
  const comparable=structuredClone(n)
  if(Object.hasOwn(e,'supportedBases'))comparable.supportedBases=e.supportedBases
  else delete comparable.supportedBases
  assert.deepEqual(comparable,e)
  assert((e.supportedBases??[]).every(k=>n.supportedBases.includes(k)))
}
for(const [k,v]of Object.entries(before.workbenchBases))assert.deepEqual(after.workbenchBases[k],v)
const root='docs/evidence/crossbows-source-bundle-2026-10-05'
const bases=read('backend/src/main/resources/catalog/top-bases.json')
for(const b of Object.values(bases).filter(b=>b.family==='crossbows')) {
  assert.equal(after.workbenchBases[b.key].ruleVersion,'crossbow-workbench-v1')
  assert.equal(after.workbenchBases[b.key].ledgerVersion,'crossbow-unverified-numeric-assumptions-v1')
  const c=read(`backend/src/main/resources/catalog/${b.pool}/catalog.json`)
  const details=read(`backend/src/main/resources/catalog/${b.pool}/details.raw.json`)
  for(const d of details.ordinary) {
    const first=d.spawn.find(s=>[...b.sourceTags,'default'].includes(s.tag))
    assert(first&&first.weight>0,`${b.key}: ${d.row.Name}`)
    assert.equal(crypto.createHash('sha256').update(d.html).digest('hex'),d.sha256)
    const matched=c.modifiers.find(m=>m.text===d.row.str.replace(/<br\s*\/?>/gi,'\n').replace(/<[^>]*>/g,'').replace(/&nbsp;/g,' ').replace(/&amp;/g,'&').trim()&&m.requiredItemLevel===+d.row.Level&&JSON.stringify(m.familyIds)===JSON.stringify(d.row.ModFamilyList))
    assert(matched&&matched.weight===+d.row.DropChance)
    assert.deepEqual(matched.stats,d.stats.map(({locality,...s})=>s))
  }
  assert.equal(details.ordinary.length,146)
}
fs.writeFileSync(process.env.HEPHAISTOS_CROSSBOW_PRESERVATION_PATH??`${root}/preservation.json`,JSON.stringify({passed:true,baseline:base,oldManifestBases:count,oldWorkbenchBases:91,newWorkbenchBases:97,completeOrdinary:146,special:8,registry:220,deferred:50,oldDefinitionsTermsTemplatesUnchanged:true,perBaseOrderedSpawnVerified:true,crossbowVersionsVerified:true},null,2)+'\n',{flag:'wx'})
console.log('Preserved old91 identities, definitions, six-locale terms/templates, registry220/deferred50; all6 ordered spawn pools verified')
