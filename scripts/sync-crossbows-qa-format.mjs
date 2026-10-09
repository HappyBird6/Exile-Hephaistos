import fs from 'node:fs'
import assert from 'node:assert/strict'

const qa = 'E:/WORK/Exile-Hephaistos/codex/crossbows-qa-20261005'
const java = [
  'bootstrap/CraftingConfiguration.java',
  'crafting/application/WorkbenchService.java',
  'crafting/domain/QualityLimitRules.java',
  'crafting/domain/WorkbenchSimulator.java',
  'item/infrastructure/ItemCatalogLoader.java',
  'item/ReviewedCrossbows.java',
]
for (const name of java) {
  const relative = `src/main/java/com/poe2craft/${name}`
  fs.copyFileSync(`${qa}/backend-check/${relative}`, `backend/${relative}`)
}
const test = 'src/test/java/com/poe2craft/crafting/ReviewedCrossbowsTest.java'
fs.copyFileSync(`${qa}/backend-check/${test}`, `backend/${test}`)
for (const name of [
  'features/crafting/CraftingPage.tsx',
  'features/crafting/craftingApi.ts',
  'features/crafting/draft.ts',
  'features/crafting/topBaseEssences.json',
  'features/crafting/topBases.json',
  'features/crafting/topBases.test.ts',
  'features/crafting/localizedModifiers.test.ts',
  'features/crafting/remainingDisplay.test.tsx',
  'features/crafting/topBases.ts',
  'features/crafting/workbenchApi.ts',
  'shared/i18n/gameTerms.json',
  'shared/i18n/modifierTemplates.json',
]) {
  const source = `${qa}/frontend-check/src/${name}`
  const target = `frontend/src/${name}`
  if (name.endsWith('.json'))
    assert.deepEqual(JSON.parse(fs.readFileSync(source)), JSON.parse(fs.readFileSync(target)))
  fs.copyFileSync(source, target)
}
console.log('Copied only Crossbow authored files after QA formatter; JSON semantics unchanged')
