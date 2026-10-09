import fs from 'node:fs'
import assert from 'node:assert/strict'
const check=process.argv.includes('--check')
const mappings=[['catalog/top-bases.json','topBases.json'],['catalog/top-base-essences.json','topBaseEssences.json'],['catalog/base-policies.json','basePolicies.json']]
for(const [source,destination]of mappings) {
 const canonical=fs.readFileSync(`backend/src/main/resources/${source}`)
 const target=`frontend/src/features/crafting/${destination}`
 if(check)assert.deepEqual(JSON.parse(fs.readFileSync(target)),JSON.parse(canonical),target)
 else fs.writeFileSync(target,canonical)
}
console.log(check?'Base registry mirrors match canonical resources':'Base registry mirrors synchronized')
