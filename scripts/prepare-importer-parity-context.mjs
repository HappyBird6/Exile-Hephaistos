import fs from 'node:fs'
import assert from 'node:assert/strict'
import {execFileSync} from 'node:child_process'
const specs={maces:{head:'3c40950098146be6b2b80fda3ec54c380f833927',pools:['fortified-hammer','ruination-maul']},'quarterstaves-spears':{head:'2fe3f86d101ef8f3f35636470a9e4789ae7c7edf',pools:['aegis-quarterstaff','grand-spear']}}
const result={}
for(const [namespace,{head,pools}]of Object.entries(specs)) {
 const prior=JSON.parse(execFileSync('git',['show',`${head}:frontend/src/shared/i18n/modifierTemplates.json`],{maxBuffer:32*1024*1024}))
 const definitions={},templates=Object.fromEntries(Object.keys(prior.templates).map(l=>[l,{}]))
 const ids=new Set(pools.flatMap(pool=>JSON.parse(fs.readFileSync(`backend/src/main/resources/catalog/${pool}/catalog.json`)).modifiers.filter(d=>d.weight>0).map(d=>d.id)))
 for(const id of ids)if(prior.definitions[id]) {
  const definition=definitions[id]=prior.definitions[id]
  for(const locale of Object.keys(templates))templates[locale][definition.template]=prior.templates[locale][definition.template]
 }
 assert(Object.keys(definitions).length>0)
 result[namespace]={baseHead:head,pools,display:{definitions,templates}}
}
fs.writeFileSync('docs/evidence/base-registry-refactor-2026-10-05/importer-before-context.json',JSON.stringify(result,null,2)+'\n',{flag:'wx'})
console.log(Object.fromEntries(Object.entries(result).map(([key,r])=>[key,Object.keys(r.display.definitions).length])))
