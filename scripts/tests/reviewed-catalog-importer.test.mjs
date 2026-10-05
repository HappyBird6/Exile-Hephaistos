import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import { createDisplayBinder, orderedSpawnEligible } from '../reviewed-catalog-importer.mjs'
import { cleanSource } from '../armour-source.mjs'
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'))
const old=read('docs/evidence/base-registry-refactor-2026-10-05/importer-baseline.json')
const contexts=read('docs/evidence/base-registry-refactor-2026-10-05/importer-before-context.json')
const locales={en:'us',ko:'kr',ja:'jp','zh-CN':'cn','zh-TW':'tw',es:'sp'}
test('ordered spawn uses the first matched source tag, including explicit denial',()=>{
 assert.equal(orderedSpawnEligible({url:'source',spawn:[{tag:'staff',weight:0},{tag:'weapon',weight:100}]},['staff','weapon']),false)
 assert.equal(orderedSpawnEligible({url:'source',spawn:[{tag:'unknown',weight:0},{tag:'weapon',weight:100}]},['staff','weapon']),true)
 assert.throws(()=>orderedSpawnEligible({url:'source',spawn:[]},['weapon']))
})
for(const [namespace,pages]of Object.entries({maces:['One_Hand_Maces','Two_Hand_Maces'],'quarterstaves-spears':['Quarterstaves','Spears']}))test(namespace+' six-locale binder reproduces the pre-refactor implementation for every ordinary row',()=>{
 const proof=old[namespace]
 const legacyFactory=Function('display','locales','equal','assert',proof.numberPattern+'\n'+proof.binder+'\nreturn bind')
 // Reused canonical definitions already have reviewed literal/unit bindings in the real importer.
 // Use the actual pre-bundle state rather than assigning fake new IDs to those definitions.
 const legacyDisplay=structuredClone(contexts[namespace].display),newDisplay=structuredClone(contexts[namespace].display)
 const legacy=legacyFactory(legacyDisplay,locales,(a,b)=>JSON.stringify(a)===JSON.stringify(b),assert)
 const shared=createDisplayBinder(newDisplay,Object.keys(locales),namespace)
 let count=0
 for(const [index,page]of pages.entries()) {
  const root=`docs/evidence/${namespace}-source-bundle-2026-10-05`
  const rows=read(`${root}/${page}.us.json`).data.normal
  for(let i=0;i<rows.length;i++) {
   const detail=read(`${root}/details/${page}/normal-${i}.json`)
   const row=rows[i],text=cleanSource(row.str),stats=detail.stats.map(({locality,...s})=>s)
   const pool=read(`backend/src/main/resources/catalog/${contexts[namespace].pools[index]}/catalog.json`)
   const matching=pool.modifiers.filter(d=>d.weight>0&&d.requiredItemLevel===Number(row.Level)&&d.affixType===(Number(row.ModGenerationTypeID)===1?'PREFIX':'SUFFIX')&&d.text===text&&JSON.stringify(d.stats)===JSON.stringify(stats))
   assert.equal(matching.length,1,`${page}/${i}: exact canonical ordinary identity`)
   const d=matching[0]
   const texts=Object.fromEntries(Object.entries(locales).map(([l,r])=>[l,cleanSource(read(`${root}/${page}.${r}.json`).data.normal[i].str)]))
   legacy(d,texts,detail.code);shared(d,texts,detail.code);count++
  }
 }
 assert.equal(count,namespace==='maces'?300:320)
 assert.deepEqual(newDisplay,legacyDisplay)
})
