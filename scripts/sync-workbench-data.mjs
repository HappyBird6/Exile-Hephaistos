// Backend resources are canonical. Browser projections are explicit and checked without writes by default.
import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import { fileURLToPath } from 'node:url'
export const mirrors = [
 ['top-bases.json','topBases.json'], ['top-base-essences.json','topBaseEssences.json'],
 ['base-policies.json','basePolicies.json'], ['equipment-currencies.json','equipmentCurrencies.json'],
 ['augment-crafting.json','augmentCrafting.json'], ['augment-effect-templates.json','augmentEffectTemplates.json'],
 ['augments.json','augments.json'],
]
export function browserData(source, value) {
 // The chooser starts new ordinary bases at Normal; legacy server initialText is an import fixture.
 if(source === 'base-policies.json') {
  value = structuredClone(value)
  for(const base of Object.values(value.legacy))
   base.initialText = base.initialText.replace(/^Rarity: .+$/m, `Rarity: ${base.rareOnly ? 'Rare' : 'Normal'}`)
 }
 return value
}
export function synchronize(root, write = false, selected = mirrors) {
 for(const [source, destination] of selected) {
  const value=browserData(source, JSON.parse(fs.readFileSync(path.join(root,'backend/src/main/resources/catalog',source))))
  const target=path.join(root,'frontend/src/features/crafting',destination)
  if(write) fs.writeFileSync(target,JSON.stringify(value,null,2)+'\n')
  else assert.deepStrictEqual(JSON.parse(fs.readFileSync(target)),value,`Workbench data differs: ${destination}; review canonical changes, then run node scripts/sync-workbench-data.mjs --write`)
 }
}
if(process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
 synchronize(fileURLToPath(new URL('..',import.meta.url)),process.argv.includes('--write'))
 console.log('Workbench data mirrors verified')
}
