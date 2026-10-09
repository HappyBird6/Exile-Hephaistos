import fs from 'node:fs'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
const attempt=process.argv[2]??'1'
assert(/^[1-9][0-9]*$/.test(attempt))
const root='/qa/body-helmets-importer-replay-'+attempt
assert(!fs.existsSync(root),'Preserve earlier replay output')
for(const dir of ['backend/src/main/resources/catalog','backend/src/main/resources/crafting','docs/evidence/armour-source-bundle-2026-10-04','docs/evidence/body-helmets-runtime-bundle-2026-10-04','frontend/src/features/crafting','frontend/src/shared/i18n'])fs.cpSync('/source/'+dir,root+'/'+dir,{recursive:true,filter:p=>!p.endsWith('.png')})
for(const flags of [['--body'],[]])fs.appendFileSync('/qa/importer-replay-'+attempt+'.log',execFileSync(process.execPath,['/source/scripts/import-body-helmets-bundle.mjs',...flags],{cwd:root,encoding:'utf8'}))
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'))
const files=['backend/src/main/resources/catalog/top-bases.json','backend/src/main/resources/catalog/top-base-essences.json','backend/src/main/resources/crafting/registry-v2.json','frontend/src/features/crafting/topBases.json','frontend/src/features/crafting/topBaseEssences.json','frontend/src/shared/i18n/gameTerms.json','frontend/src/shared/i18n/modifierTemplates.json']
const pools=['slipstrike-vest','death-mail','sleek-jacket','vile-robe','wolfskin-mantle','ancestral-tiara','cryptic-crown']
let rows=0, exactDetails=0
for(const pool of pools) {
  for(const file of ['catalog.json','base.raw.json','details.raw.json'])files.push(`backend/src/main/resources/catalog/${pool}/${file}`)
  const catalog=read(`/source/backend/src/main/resources/catalog/${pool}/catalog.json`)
  const proof=read(`/source/backend/src/main/resources/catalog/${pool}/details.raw.json`)
  assert.equal(proof.proofs.length,catalog.modifiers.filter(d=>d.weight>0).length)
  for(const p of proof.proofs) {
    const d=catalog.modifiers.find(d=>d.id===p.id)
    assert.equal(d.requiredItemLevel,+p.row.Level)
    assert.equal(d.affixType,p.row.ModGenerationTypeID==='1'?'PREFIX':'SUFFIX')
    assert.deepEqual([...d.familyIds].sort(),[...p.row.ModFamilyList].sort())
    assert.deepEqual([...d.tags].sort(),[...p.row.fossil_no].sort())
    if(p.proof.parsed) {
      assert(p.proof.parsed.fields.GenerationType.endsWith(`(${p.row.ModGenerationTypeID})`))
      assert.deepEqual(p.proof.parsed.stats.map(({locality,...s})=>s),d.stats)
      assert.deepEqual(p.proof.parsed.spawn.map(s=>s.tag),p.row.spawn_no)
      exactDetails++
    }
    rows++
  }
}
for(const file of files)assert.deepEqual(read(root+'/'+file),read('/source/'+file),file)
fs.writeFileSync('/qa/importer-replay-results-'+attempt+'.json',JSON.stringify({passed:true,comparedDocuments:files.length,ordinaryRows:rows,exactDetails,assertions:'Cached source, stable IDs, ranges, family, generation, tags, spawn ordering, six-language templates and timestamps preserved'},null,2)+'\n')
console.log(files.length,rows,exactDetails,'cached importer assertions passed')
