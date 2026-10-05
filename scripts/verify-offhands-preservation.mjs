import fs from 'node:fs'
import assert from 'node:assert/strict'
import {execFileSync} from 'node:child_process'
import crypto from 'node:crypto'
const head='ba9df63a0fddc6c15967103ebb4e2636b7488cd7'
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'))
const before=p=>JSON.parse(execFileSync('git',['show',`${head}:${p}`],{encoding:'utf8',maxBuffer:64*1024*1024}))
const paths=['backend/src/main/resources/catalog/top-bases.json','backend/src/main/resources/catalog/top-base-essences.json','frontend/src/shared/i18n/gameTerms.json','frontend/src/shared/i18n/modifierTemplates.json','backend/src/main/resources/crafting/registry-v2.json']
const proofs=[]
for(const p of paths) {
 const old=before(p),current=read(p)
 if(p.endsWith('gameTerms.json'))for(const locale of Object.keys(old))for(const [key,value]of Object.entries(old[locale]))assert.deepEqual(current[locale][key],value,`${locale}/${key}`)
 else if(p.endsWith('modifierTemplates.json')) {
  for(const [key,value]of Object.entries(old.definitions))assert.deepEqual(current.definitions[key],value,key)
  for(const locale of Object.keys(old.templates))for(const [key,value]of Object.entries(old.templates[locale]))assert.deepEqual(current.templates[locale][key],value,`${locale}/${key}`)
 } else if(p.endsWith('registry-v2.json')) {
  assert.equal(old.entries.length,current.entries.length)
  assert.equal(current.entries.filter(e=>e.serviceScope==='DEFERRED').length,50)
  for(const [key,value]of Object.entries(old.workbenchBases))assert.deepEqual(current.workbenchBases[key],value,key)
  for(const e of old.entries) {
   const next=current.entries.find(n=>n.id===e.id)
   if(e.serviceScope==='DEFERRED')assert.deepEqual(next,e,e.id)
   else {const {supportedBases:previous,...oldFields}=e,{supportedBases:now,...newFields}=next;assert.deepEqual(newFields,oldFields,e.id);if(previous)assert.deepEqual(now.filter(k=>!['tawhoan-tower-shield','golden-targe','blacksteel-crest-shield','desert-buckler','tasalian-focus'].includes(k)),previous,e.id)}
  }
 } else for(const [key,value]of Object.entries(old))assert.deepEqual(current[key],value,key)
 proofs.push(p)
}
const bases=read(paths[0])
assert.equal(Object.keys(bases).length,Object.keys(before(paths[0])).length+5)
assert.deepEqual(read(paths[0]),read('frontend/src/features/crafting/topBases.json'))
assert.deepEqual(read(paths[1]),read('frontend/src/features/crafting/topBaseEssences.json'))
const root='docs/evidence/offhands-source-bundle-2026-10-05',roster=[['tawhoan-tower-shield','Shields_str'],['golden-targe','Shields_str_dex'],['blacksteel-crest-shield','Shields_str_int'],['desert-buckler','Bucklers'],['tasalian-focus','Foci']]
for(const [key,page]of roster) {
 const source=read(`${root}/${page}.us.json`).data,catalog=read(`backend/src/main/resources/catalog/${key}/catalog.json`)
 const actual=catalog.modifiers.filter(d=>d.weight>0)
 assert.equal(actual.length,source.normal.length)
 source.normal.forEach((row,i)=>{
  const proof=read(`${root}/details/${page}/normal-${i}.json`),definition=actual[i]
  assert.equal(definition.weight,+row.DropChance)
  assert.deepEqual(definition.stats,proof.stats.map(({locality,...stat})=>stat))
  assert.deepEqual(definition.familyIds,row.ModFamilyList)
  const spawn=proof.spawn.find(s=>[...bases[key].sourceTags,'default'].includes(s.tag))
  assert(spawn?.weight>0,`${key}/${i}: ordered eligibility`)
 })
}
for(const file of fs.readdirSync(root).filter(p=>p.endsWith('.html'))) {
 const record=read(`${root}/${file.replace(/\.html$/,'.json')}`)
 assert.equal(crypto.createHash('sha256').update(fs.readFileSync(`${root}/${file}`)).digest('hex'),record.sha256)
}
const output={head,oldWorkbenchBases:97,newWorkbenchBases:102,oldManifestBases:Object.keys(before(paths[0])).length,newManifestBases:Object.keys(bases).length,oldValuesPreserved:proofs,registryEntries:220,deferredEntries:50,sourcePools:roster.map(([key,page])=>({key,page,ordinary:read(`${root}/${page}.us.json`).data.normal.length})),sourceWeightPolicy:'Published selection weights retained without claiming game weights',builtInSkillPolicy:'Source display without synthetic item stats or combat simulation'}
fs.writeFileSync(`${root}/${process.argv[2] || 'preservation.json'}`,JSON.stringify(output,null,2)+'\n',{flag:'wx'})
console.log(output)
