import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { execFileSync } from 'node:child_process'
const baseline='5287663f383d5199b43ff963fc51c55d70859270',root='docs/evidence/staves-talismans-source-bundle-2026-10-05'
const read=p=>JSON.parse(fs.readFileSync(p)),old=p=>JSON.parse(execFileSync('git',['show',`${baseline}:${p}`],{maxBuffer:32*1024*1024}))
const canonical='backend/src/main/resources/catalog/'
const before=old(canonical+'top-bases.json'),after=read(canonical+'top-bases.json'),policies=read(canonical+'base-policies.json')
assert.equal(Object.keys(before).length,108);assert.equal(Object.keys(after).length,117)
for(const [key,value]of Object.entries(before))assert.deepEqual(after[key],value,key+' old base metadata')
for(const path of [canonical+'top-base-essences.json',canonical+'base-policies.json','frontend/src/shared/i18n/gameTerms.json','frontend/src/shared/i18n/modifierTemplates.json']) {
 const previous=old(path),current=read(path)
 const recursive=(a,b,p)=>{for(const [key,value]of Object.entries(a)) { if(value&&typeof value==='object'&&!Array.isArray(value))recursive(value,b[key],p+'/'+key);else assert.deepEqual(b[key],value,p+'/'+key) }}
 recursive(previous,current,path)
}
const registry=read('backend/src/main/resources/crafting/registry-v2.json'),previous=old('backend/src/main/resources/crafting/registry-v2.json')
assert.equal(registry.entries.length,previous.entries.length)
for(let i=0;i<registry.entries.length;i++) {
 const a=previous.entries[i],b=registry.entries[i]
 assert.deepEqual({...b,supportedBases:a.supportedBases},{...a,supportedBases:a.supportedBases},a.id+' registry semantics')
 if(a.supportedBases)for(const key of a.supportedBases)assert(b.supportedBases.includes(key))
}
assert.deepEqual(registry.entries.filter(e=>e.serviceScope==='DEFERRED'),previous.entries.filter(e=>e.serviceScope==='DEFERRED'))
assert.equal(registry.entries.filter(e=>e.serviceScope==='DEFERRED').length,50)
const paths=execFileSync('git',['ls-tree','-r','--name-only',baseline,canonical],{encoding:'utf8'}).trim().split('\n').filter(p=>!['top-bases.json','top-base-essences.json','base-policies.json'].includes(p.split('/').at(-1)))
for(const path of paths)assert(fs.readFileSync(path).equals(execFileSync('git',['show',`${baseline}:${path}`],{maxBuffer:32*1024*1024})),path+' old catalog bytes')
assert.equal(Object.keys(after).length+Object.keys(policies.legacy).length,134)
fs.writeFileSync(`${root}/preservation.json`,JSON.stringify({passed:true,baseHead:baseline,oldBases:125,newBases:9,totalBases:134,oldCatalogFilesByteIdentical:paths.length,deferred:50,existingRegistryActionsUnchanged:true,sha256:crypto.createHash('sha256').update(fs.readFileSync(canonical+'top-bases.json')).digest('hex')},null,2)+'\n',{flag:'wx'})
console.log(paths.length,'old catalog files unchanged; 125 old metadata/display definitions and deferred50 preserved')
