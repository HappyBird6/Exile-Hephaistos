import fs from 'node:fs'
import assert from 'node:assert/strict'
import {execFileSync} from 'node:child_process'
const old=p=>JSON.parse(execFileSync('git',['show',`5ae15bfbeaa29a134a422f8a84013928e688064a:${p}`],{maxBuffer:32*1024*1024,encoding:'utf8'}))
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'))
const keys=['stoic','omen','shrine-fire','shrine-ice','shrine-lightning','clasped','wrath']
for(const p of ['backend/src/main/resources/catalog/top-bases.json','frontend/src/features/crafting/topBases.json','backend/src/main/resources/catalog/top-base-essences.json','frontend/src/features/crafting/topBaseEssences.json']) {
 const prior=old(p),current=read(p)
 for(const key of Object.keys(prior)) assert.deepEqual(current[key],prior[key])
 assert.equal(Object.keys(current).length,Object.keys(prior).length+7)
}
const p='frontend/src/shared/i18n/gameTerms.json',prior=old(p),current=read(p)
for(const locale of Object.keys(prior)) for(const key of Object.keys(prior[locale])) assert.deepEqual(current[locale][key],prior[locale][key])
const mp='frontend/src/shared/i18n/messages.json',previousMessages=old(mp),messages=read(mp)
for(const locale of Object.keys(previousMessages)) for(const key of Object.keys(previousMessages[locale])) assert.deepEqual(messages[locale][key],previousMessages[locale][key])
const rp='backend/src/main/resources/crafting/registry-v2.json',before=old(rp),registry=read(rp)
assert.equal(registry.entries.length,before.entries.length)
for(let i=0;i<registry.entries.length;i++) {
 const e=structuredClone(registry.entries[i])
 if(e.supportedBases) e.supportedBases=e.supportedBases.filter(k=>!keys.includes(k))
 assert.deepEqual(e,before.entries[i])
}
for(const key of Object.keys(before.workbenchBases)) assert.deepEqual(registry.workbenchBases[key],before.workbenchBases[key])
assert.equal(registry.entries.filter(e=>e.serviceScope==='DEFERRED').length,50)
console.log('Existing71 bases, translations, registry and deferred50 preserved')
