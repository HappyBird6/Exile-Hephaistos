import fs from 'node:fs'
import assert from 'node:assert/strict'
import {execFileSync} from 'node:child_process'
const start='aa741d572e23067e74bb17aaf6daf3a3949fb87b'
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'))
const before=p=>JSON.parse(execFileSync('git',['show',`${start}:${p}`],{encoding:'utf8',maxBuffer:32*1024*1024}))
const paths=['frontend/src/features/crafting/topBases.json','backend/src/main/resources/catalog/top-bases.json','frontend/src/features/crafting/topBaseEssences.json','backend/src/main/resources/catalog/top-base-essences.json']
for(const p of paths){const old=before(p),now=read(p);for(const [k,v]of Object.entries(old))assert.deepEqual(now[k],v,`${p}/${k}`);assert.equal(Object.keys(now).length,Object.keys(old).length+11)}
for(const p of ['frontend/src/shared/i18n/gameTerms.json','frontend/src/shared/i18n/modifierTemplates.json']) {
 const old=before(p),now=read(p)
 if(p.includes('gameTerms'))for(const [l,items]of Object.entries(old))for(const [k,v]of Object.entries(items))assert.deepEqual(now[l][k],v,`${l}/${k}`)
 else {for(const [k,v]of Object.entries(old.definitions))assert.deepEqual(now.definitions[k],v,k);for(const [l,t]of Object.entries(old.templates))for(const [k,v]of Object.entries(t))assert.deepEqual(now.templates[l][k],v,`${l}/${k}`)}
}
const p='backend/src/main/resources/crafting/registry-v2.json',old=before(p),now=read(p),added=Object.keys(read(paths[0])).filter(k=>!Object.hasOwn(before(paths[0]),k))
for(const entry of old.entries){const current={...now.entries.find(e=>e.id===entry.id)};if(current.supportedBases)current.supportedBases=current.supportedBases.filter(k=>!added.includes(k));assert.deepEqual(current,entry,entry.id)}
assert.equal(now.entries.filter(e=>e.serviceScope==='DEFERRED').length,50)
for(const b of Object.values(read(paths[0])).filter(b=>b.family==='quivers')) {
 const c=read(`backend/src/main/resources/catalog/${b.pool}/catalog.json`),ordinary=c.modifiers.filter(d=>d.weight>0)
 assert.equal(ordinary.length,100);assert.equal(c.modifiers.filter(d=>d.layer==='IMPLICIT').length,1)
 assert.equal(b.classId,20);assert.equal(b.maximumQuality,null);assert.equal(b.ordinarySocketMaximum,0)
 const raw=read(`backend/src/main/resources/catalog/${b.pool}/base.raw.json`)
 for(const [i,r]of raw.entries())assert.equal(ordinary[i].weight,+r.DropChance)
}
for(const locale of ['kr','jp','cn','tw','sp']) {
 const original=read('docs/evidence/quivers-source-bundle-2026-10-05/Quivers.us.json').data,local=read(`docs/evidence/quivers-source-bundle-2026-10-05/Quivers.${locale}.json`).data
 for(const kind of ['normal']) {
  assert.equal(local[kind].length,original[kind].length)
  for(const [i,r]of original[kind].entries())for(const field of ['Level','ModGenerationTypeID','ModFamilyList','DropChance','Code'])assert.deepEqual(local[kind][i][field],r[field],`${locale}/${kind}/${i}/${field}`)
 }
 const identity=r=>[r.Code,r.Level,r.ModGenerationTypeID,r.ModFamilyList,r.DropChance]
 const rows=d=>[...d.essence,...d.perfect_essence].map(identity).sort((a,b)=>JSON.stringify(a).localeCompare(JSON.stringify(b)))
 // Spanish source groups Perfect rows in essence instead of perfect_essence; verify the full exact union.
 assert.deepEqual(rows(local),rows(original),`${locale}/complete Essence identity`)
}
const report={passed:true,baseHead:start,previousBases:102,addedBases:11,totalBases:113,deferred:50,oldDefinitionsAndLocalesPreserved:true,ordinaryPerQuiver:100,sixLocaleRowIdentityVerified:true}
const out=process.argv[2]??'preservation.json'
fs.writeFileSync(`docs/evidence/quivers-source-bundle-2026-10-05/${out}`,JSON.stringify(report,null,2)+'\n',{flag:'wx'})
console.log(report)
