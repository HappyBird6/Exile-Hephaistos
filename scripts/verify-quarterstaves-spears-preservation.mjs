import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { execFileSync } from 'node:child_process'
const head='2fe3f86d101ef8f3f35636470a9e4789ae7c7edf'
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'))
const old=p=>JSON.parse(execFileSync('git',['show',`${head}:${p}`],{encoding:'utf8',maxBuffer:20*1024*1024}))
const results={baseHead:head,checks:[],sourceFiles:[]}
for(const path of ['backend/src/main/resources/catalog/top-bases.json','backend/src/main/resources/catalog/top-base-essences.json','frontend/src/features/crafting/topBases.json','frontend/src/features/crafting/topBaseEssences.json']) {
  const before=old(path),after=read(path)
  for(const [key,value] of Object.entries(before)) assert.deepEqual(after[key],value,`${path}/${key}`)
  assert.equal(Object.keys(after).length,Object.keys(before).length+6)
  results.checks.push(`${path}: all prior entries identical; six additions`)
}
const templatePath='frontend/src/shared/i18n/modifierTemplates.json',before=old(templatePath),after=read(templatePath)
for(const [key,value] of Object.entries(before.definitions)) assert.deepEqual(after.definitions[key],value,key)
for(const [locale,templates] of Object.entries(before.templates))for(const [key,value]of Object.entries(templates))assert.deepEqual(after.templates[locale][key],value,key)
const termPath='frontend/src/shared/i18n/gameTerms.json'
for(const [locale,terms]of Object.entries(old(termPath)))for(const [key,value]of Object.entries(terms))assert.deepEqual(read(termPath)[locale][key],value,key)
const registryPath='backend/src/main/resources/crafting/registry-v2.json',oldRegistry=old(registryPath),registry=read(registryPath)
assert.equal(registry.entries.length,oldRegistry.entries.length)
for(const entry of oldRegistry.entries) {
  const current=registry.entries.find(e=>e.id===entry.id)
  const {supportedBases,...rest}=entry,{supportedBases:currentBases,...currentRest}=current
  assert.deepEqual(currentRest,rest,entry.id)
  if(supportedBases)assert.deepEqual(currentBases.filter(k=>!['aegis-quarterstaff','bolting-quarterstaff','dreaming-quarterstaff','grand-spear','flying-spear','akoyan-spear'].includes(k)),supportedBases,entry.id)
  else assert.equal(currentBases,undefined)
  if(entry.serviceScope==='DEFERRED')assert.deepEqual(current,entry)
}
assert.equal(registry.entries.filter(e=>e.serviceScope==='DEFERRED').length,50)
results.checks.push('Historical templates/names/registry entries/deferred50 identical except six supported-base additions')
const root='docs/evidence/quarterstaves-spears-source-bundle-2026-10-05'
for(const file of fs.readdirSync(root).filter(f=>f.endsWith('.html'))) {
  const bytes=fs.readFileSync(`${root}/${file}`),digest=crypto.createHash('sha256').update(bytes).digest('hex')
  assert.equal(digest,read(`${root}/${file.replace('.html','.json')}`).sha256)
  results.sourceFiles.push({file,sha256:digest})
}
results.totalBases=125
fs.writeFileSync(`${root}/${process.argv[2] ?? 'preservation.json'}`,JSON.stringify(results,null,2)+'\n',{flag:'wx'})
console.log(results.checks,results.sourceFiles.length)
