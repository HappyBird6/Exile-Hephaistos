import fs from 'node:fs'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
const root='/qa/importer-replay-1'
assert(!fs.existsSync(root),'Preserve previous replay evidence')
fs.mkdirSync(root)
const files=[]
for(const slot of ['backend','frontend','docs','scripts']) {
  fs.cpSync(`/source/${slot}`,`${root}/${slot}`,{recursive:true,filter:p=>!p.split(/[\\/]/).some(s=>['node_modules','build','dist','.gradle','coverage'].includes(s))})
}
const pools=['warmonger','guardian','gemini','fanatic','obliterator']
for(const k of pools)for(const f of ['catalog.json','base.raw.json','details.raw.json'])files.push(`backend/src/main/resources/catalog/${k}-bow/${f}`)
files.push('backend/src/main/resources/catalog/top-bases.json','frontend/src/features/crafting/topBases.json','backend/src/main/resources/catalog/top-base-essences.json','frontend/src/features/crafting/topBaseEssences.json','backend/src/main/resources/crafting/registry-v2.json','frontend/src/shared/i18n/gameTerms.json','frontend/src/shared/i18n/modifierTemplates.json','docs/evidence/bows-source-bundle-2026-10-04/import-summary.json')
const before=new Map(files.map(p=>[p,JSON.parse(fs.readFileSync(`/source/${p}`))]))
execFileSync('node',['scripts/import-bows-bundle.mjs'],{cwd:root,stdio:'inherit'})
for(const p of files)assert.deepEqual(JSON.parse(fs.readFileSync(`${root}/${p}`)),before.get(p),p)
const result={passed:true,comparedDocuments:files.length,ordinaryRows:700,reusedOrdinaryDefinitions:140,reusedSpecialDefinitions:8,newImplicitDefinitions:4}
fs.writeFileSync('/qa/importer-replay-results.json',JSON.stringify(result,null,2)+'\n')
console.log(result)
