import fs from 'node:fs'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
const start = 'e5d7c4e663049565de869e0aed826cfa714c8eb5'
const old = p => JSON.parse(execFileSync('git', ['show', `${start}:${p}`], { encoding: 'utf8', maxBuffer: 32*1024*1024 }))
const read = p => JSON.parse(fs.readFileSync(p, 'utf8'))
const keys = Object.keys(read('frontend/src/features/crafting/topBases.json')).filter(k => k.endsWith('-belt'))
assert.equal(keys.length,13)
for (const p of ['backend/src/main/resources/catalog/top-bases.json', 'frontend/src/features/crafting/topBases.json', 'backend/src/main/resources/catalog/top-base-essences.json', 'frontend/src/features/crafting/topBaseEssences.json']) {
  const prior = old(p), current = read(p)
  for (const key of Object.keys(prior)) assert.deepEqual(current[key],prior[key],`${p}/${key}`)
  assert.equal(Object.keys(current).length,Object.keys(prior).length+13)
}
for (const p of ['frontend/src/shared/i18n/gameTerms.json','frontend/src/shared/i18n/messages.json']) {
  const prior = old(p), current = read(p)
  for (const locale of Object.keys(prior)) for (const key of Object.keys(prior[locale])) assert.deepEqual(current[locale][key],prior[locale][key],`${p}/${locale}/${key}`)
}
const mp = 'frontend/src/shared/i18n/modifierTemplates.json', beforeDisplay = old(mp), afterDisplay = read(mp)
for (const id of Object.keys(beforeDisplay.definitions)) assert.deepEqual(afterDisplay.definitions[id],beforeDisplay.definitions[id],id)
for (const locale of Object.keys(beforeDisplay.templates)) for (const id of Object.keys(beforeDisplay.templates[locale])) assert.deepEqual(afterDisplay.templates[locale][id],beforeDisplay.templates[locale][id],`${locale}/${id}`)
const rp = 'backend/src/main/resources/crafting/registry-v2.json', prior = old(rp), current = read(rp)
assert.equal(current.entries.length,prior.entries.length)
for (let i = 0; i < current.entries.length; i++) {
  const entry = structuredClone(current.entries[i])
  if (entry.supportedBases) entry.supportedBases=entry.supportedBases.filter(k=>!keys.includes(k))
  assert.deepEqual(entry,prior.entries[i])
}
for (const key of Object.keys(prior.workbenchBases)) assert.deepEqual(current.workbenchBases[key],prior.workbenchBases[key])
assert.equal(current.entries.filter(e=>e.serviceScope==='DEFERRED').length,50)
console.log('Existing78 bases, ordinary/special IDs, six-locale terms/bindings, registry220 and deferred50 preserved')
