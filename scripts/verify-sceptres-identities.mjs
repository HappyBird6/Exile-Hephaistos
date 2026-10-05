import fs from 'node:fs'
import assert from 'node:assert/strict'
const root='docs/evidence/sceptres-source-bundle-2026-10-05'
const identities=JSON.parse(fs.readFileSync(`${root}/skill-identities.json`))
const expected={stoic:'Discipline',omen:'Malice','shrine-fire':'PurityOfFire','shrine-ice':'PurityOfIce','shrine-lightning':'PurityOfLightning',clasped:'HeartOfIce',wrath:'Fulmination'}
const details=JSON.parse(fs.readFileSync('backend/src/main/resources/catalog/rattling-sceptre/details.raw.json'))
const catalog=JSON.parse(fs.readFileSync('backend/src/main/resources/catalog/hallowed-sceptre/catalog.json'))
for(const locale of ['us','kr','jp','cn','tw','sp']) {
 const current=JSON.parse(fs.readFileSync(`${root}/Sceptres.${locale}.json`)).data
 const prior=JSON.parse(fs.readFileSync(`docs/evidence/hallowed-source-bundle-2026-10-04/Sceptres.${locale}.json`)).data
 assert.deepEqual(current.essence,prior.essence,`${locale}: reused fixed Essence source`)
 assert.deepEqual(current.perfect_essence,prior.perfect_essence,`${locale}: reused Perfect Essence source`)
}
assert.equal(identities.length,7)
for(const b of identities) {
 const slug=b.key.startsWith('shrine-')?'Shrine_Sceptre':`${b.key[0].toUpperCase()+b.key.slice(1)}_Sceptre`
 const source=JSON.parse(fs.readFileSync(`${root}/${slug}.us.json`)).variants.find(v=>v.fields.Type===b.id)
 assert(source && source.card.includes('Spirit: 100'))
 assert.equal(source.fields['Quality.max_quality'],'20')
 assert.deepEqual(source.fields.Tags.split(', '),b.tags)
 assert.deepEqual(b.fields.filter(([k])=>k==='Id'),[['Id',expected[b.key]]])
 assert.equal(b.eligible,150)
 assert.equal(b.excluded,0)
 assert.equal(b.displayOnly,true)
 for(const d of details) {
  const spawn=[...d.html.matchAll(/class=['"]badge bg-primary['"]>([^<]+): (\d+)<\/span>/g)].map(m=>({tag:m[1],weight:+m[2]}))
  const first=spawn.find(s=>b.tags.includes(s.tag)||s.tag==='default')
  const definition=catalog.modifiers.find(m=>m.sourceUrl===d.url)
  assert(first && definition)
  assert(first.weight > 0, `Eligible tag evidence: ${b.key}/${d.code}`)
  assert.equal(definition.weight,+d.row.DropChance, `Published class selection weight: ${b.key}/${d.code}`)
  assert.equal(definition.requiredItemLevel,+d.row.Level)
  assert.deepEqual(definition.familyIds,d.row.ModFamilyList)
 }
}
const shrine=JSON.parse(fs.readFileSync(`${root}/Shrine_Sceptre.us.json`))
assert.equal(shrine.variants.length,4)
assert.equal(shrine.variants.find(b=>b.fields.Type.endsWith('Unique1')).skill,'Impurity')
assert(fs.readFileSync(`${root}/Shrine_Sceptre.us.html`,'utf8').includes('Palm_of_the_Dreamer'))
assert.equal(new Set(identities.map(b=>b.id)).size,7)
console.log('Seven exact skill IDs verified; Shrine ordinary variants distinct; unique Impurity excluded')
