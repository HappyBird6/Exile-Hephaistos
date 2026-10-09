import fs from 'node:fs'
import assert from 'node:assert/strict'
const keys=['fortified-hammer','strife-pick','akoyan-club','ruination-maul','fanatic-greathammer','tawhoan-greatclub']
for(const path of ['frontend/src/features/crafting/draft.ts','frontend/src/features/crafting/CraftingPage.tsx']) {
  const before=fs.readFileSync(path,'utf8')
  const after=before.replace(/(^\s*)\| 'broadhead-quiver'\n(?=\s*\| 'bone')/gm,(_,indent)=>`${indent}| 'broadhead-quiver'\n`+keys.map(k=>`${indent}| '${k}'\n`).join(''))
  assert.notEqual(before,after)
  fs.writeFileSync(path,after)
}
const q='E:/WORK/Exile-Hephaistos/codex/maces-qa-20261005'
for(const path of ['draft.ts','CraftingPage.tsx'])fs.copyFileSync(`frontend/src/features/crafting/${path}`,`${q}/frontend-check/src/features/crafting/${path}`)
fs.writeFileSync(`${q}/frontend-check-2.sh`,fs.readFileSync(`${q}/frontend-check-1.sh`,'utf8').replaceAll('frontend-check-4.log','frontend-check-2.log'),{flag:'wx'})
