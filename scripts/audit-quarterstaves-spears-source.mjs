import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { cleanSource as clean } from './armour-source.mjs'
const root='docs/evidence/quarterstaves-spears-source-bundle-2026-10-05'
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'))
const bases=read('backend/src/main/resources/catalog/top-bases.json')
const report={passed:true,checkedAt:new Date().toISOString(),classes:[],bases:[],scope:'Complete ordinary and Essence source; Local/Global are display semantics without combat simulation'}
for(const page of ['Quarterstaves','Spears']) {
  const data=read(`${root}/${page}.us.json`).data
  assert.equal(data.normal.length,page==='Quarterstaves'?158:162)
  const classId=page==='Quarterstaves'?58:79
  assert.equal(data.baseitem.name,classId)
  const classBases=Object.values(bases).filter(b=>b.classId===classId)
  assert.equal(classBases.length,3)
  for(const base of classBases) {
    const catalog=read(`backend/src/main/resources/catalog/${base.pool}/catalog.json`)
    for(const [kind,rows]of Object.entries(data).filter(([k])=>['normal','essence','perfect_essence'].includes(k)))for(let i=0;i<rows.length;i++) {
      const proof=read(`${root}/details/${page}/${kind}-${i}.json`),row=rows[i]
      assert.equal(crypto.createHash('sha256').update(proof.html).digest('hex'),proof.sha256)
      assert.deepEqual(proof.row,row)
      assert.equal(proof.fields.Family.split(', ').sort().join(','),row.ModFamilyList.slice().sort().join(','),`${page}/${kind}/${i}: family`)
      assert(proof.stats.length)
      if(kind!=='normal')continue
      const spawn=proof.spawn.find(s=>[...base.sourceTags,'default'].includes(s.tag))
      assert(spawn&&spawn.weight>0,`${base.key}/${i}: actual base tags`)
      const stats=proof.stats.map(({locality,...s})=>s)
      const candidate=catalog.modifiers.find(d=>d.weight>0&&d.requiredItemLevel===+row.Level&&JSON.stringify(d.stats)===JSON.stringify(stats)&&JSON.stringify(d.familyIds)===JSON.stringify(row.ModFamilyList)&&d.text===clean(row.str))
      assert(candidate,`${base.key}/${i}: complete row`)
      assert.equal(candidate.weight,+row.DropChance)
      assert.deepEqual(candidate.tags,row.mod_no.map(m=>m.match(/data-tag="([^"]+)"/)[1]))
    }
    report.bases.push({key:base.key,classId,ordinary:data.normal.length,sourceTags:base.sourceTags,implicit:base.implicitStats,localities:base.implicitLocalities,properties:base.sourceProperties.en})
  }
  report.classes.push({page,classId,ordinary:data.normal.length,essence:data.essence.length,perfectEssence:data.perfect_essence.length})
}
fs.writeFileSync(`${root}/source-audit.json`,JSON.stringify(report,null,2)+'\n',{flag:'wx'})
console.log(report.classes)
