import fs from 'node:fs'
import assert from 'node:assert/strict'
import {execFileSync} from 'node:child_process'
const root='/qa/boots-importer-replay-1'
assert(!fs.existsSync(root),'Preserve earlier replay output')
for(const dir of ['backend/src/main/resources/catalog','backend/src/main/resources/crafting','docs/evidence/armour-source-bundle-2026-10-04','docs/evidence/boots-runtime-bundle-2026-10-04','frontend/src/features/crafting','frontend/src/shared/i18n']) fs.cpSync('/source/'+dir,root+'/'+dir,{recursive:true,filter:p=>!p.endsWith('.png')})
fs.writeFileSync('/qa/importer-replay-1.log',execFileSync(process.execPath,['/source/scripts/import-body-helmets-bundle.mjs','--boots'],{cwd:root,encoding:'utf8'}))
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'))
const files=['backend/src/main/resources/catalog/top-bases.json','backend/src/main/resources/catalog/top-base-essences.json','backend/src/main/resources/crafting/registry-v2.json','frontend/src/features/crafting/topBases.json','frontend/src/features/crafting/topBaseEssences.json','frontend/src/shared/i18n/gameTerms.json','frontend/src/shared/i18n/modifierTemplates.json']
let rows=0,exactDetails=0
for(const pool of ['tasalian-greaves','drakeskin-boots','sekhema-sandals','blacksteel-sabatons','faithful-leggings','daggerfoot-shoes']) {
 for(const file of ['catalog.json','base.raw.json','details.raw.json'])files.push(`backend/src/main/resources/catalog/${pool}/${file}`)
 const catalog=read(`/source/backend/src/main/resources/catalog/${pool}/catalog.json`),proof=read(`/source/backend/src/main/resources/catalog/${pool}/details.raw.json`)
 assert.equal(proof.proofs.length,catalog.modifiers.filter(d=>d.weight>0).length)
 for(const p of proof.proofs) {
  const d=catalog.modifiers.find(d=>d.id===p.id)
  assert.equal(d.requiredItemLevel,+p.row.Level)
  assert.equal(d.affixType,p.row.ModGenerationTypeID==='1'?'PREFIX':'SUFFIX')
  assert.deepEqual(d.familyIds,p.row.ModFamilyList);assert.deepEqual(d.tags,p.row.fossil_no)
  if(p.proof.parsed) {assert.deepEqual(p.proof.parsed.stats.map(({locality,...s})=>s),d.stats);assert.deepEqual(p.proof.parsed.spawn.map(s=>s.tag),p.row.spawn_no);assert(p.proof.parsed.fields.GenerationType.endsWith(`(${p.row.ModGenerationTypeID})`));exactDetails++}
  rows++
 }
}
for(const file of files)assert.deepEqual(read(root+'/'+file),read('/source/'+file),file)
fs.writeFileSync('/qa/importer-replay-results.json',JSON.stringify({passed:true,comparedDocuments:files.length,ordinaryRows:rows,exactDetails},null,2)+'\n')
console.log(files.length,rows,exactDetails,'cached importer assertions passed')
