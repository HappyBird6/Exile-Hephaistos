import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { cleanSource as clean } from './armour-source.mjs'
import { orderedSpawnEligible } from './reviewed-catalog-importer.mjs'
const root='docs/evidence/staves-talismans-source-bundle-2026-10-05'
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'))
const bases=read('backend/src/main/resources/catalog/top-bases.json')
const report={passed:true,classes:[],bases:[],scope:'Complete eligible ordinary pool, source ranges/families/tags/weights and six-locale display; numeric model remains unverified'}
for(const [page,classId,count]of [['Staves',14,185],['Talismans',110,158]]) {
 const data=read(`${root}/${page}.us.json`).data
 assert.equal(data.normal.length,count);assert.equal(data.baseitem.name,classId)
 const selected=Object.values(bases).filter(b=>b.family===page.toLowerCase())
 assert.equal(selected.length,page==='Staves'?6:3)
 for(const base of selected) {
  const catalog=read(`backend/src/main/resources/catalog/${base.pool}/catalog.json`)
  const details=read(`backend/src/main/resources/catalog/${base.pool}/details.raw.json`).ordinary
  assert.equal(details.length,count)
  for(let i=0;i<count;i++) {
   const row=data.normal[i],proof=details[i]
   assert.equal(crypto.createHash('sha256').update(proof.html).digest('hex'),proof.sha256)
   assert.deepEqual(proof.row,row)
   assert(orderedSpawnEligible(proof,[...base.sourceTags,'default']),`${base.key}/${i}: source eligibility`)
   const stats=proof.stats.map(({locality,...s})=>s)
   const candidate=catalog.modifiers.find(d=>d.weight>0&&d.requiredItemLevel===+row.Level&&JSON.stringify(d.stats)===JSON.stringify(stats)&&JSON.stringify(d.familyIds)===JSON.stringify(row.ModFamilyList)&&d.text===clean(row.str))
   assert(candidate,`${base.key}/${i}: source row missing`)
   assert.equal(candidate.weight,+row.DropChance)
   assert.deepEqual(candidate.tags,row.mod_no.map(m=>m.match(/data-tag="([^"]+)"/)[1]))
  }
  for(const locale of ['en','ko','ja','zh-CN','zh-TW','es'])assert(base.requirements[locale]&&base.sourceProperties[locale].length)
  report.bases.push({key:base.key,id:base.id,classId:base.classId,ordinary:count,special:catalog.modifiers.filter(d=>d.layer==='EXPLICIT'&&d.weight===0).length,implicit:base.implicitStats,skills:base.skillLines??null,sourceTags:base.sourceTags})
 }
 report.classes.push({page,classId:data.baseitem.name,ordinary:count,essence:data.essence.length,perfectEssence:data.perfect_essence.length})
}
fs.writeFileSync(`${root}/${process.argv[2]??'source-audit.json'}`,JSON.stringify(report,null,2)+'\n',{flag:'wx'})
console.log(report.classes)
