import fs from 'node:fs'
import assert from 'node:assert/strict'
const names=['Fortified Hammer','Strife Pick','Akoyan Club','Ruination Maul','Fanatic Greathammer','Tawhoan Greatclub']
const keys=names.map(n=>n.toLowerCase().replaceAll(' ','-'))
function edit(path, transform) {
  const before=fs.readFileSync(path,'utf8'),after=transform(before)
  assert.notEqual(before,after,path)
  fs.writeFileSync(path,after)
}
edit('frontend/src/features/crafting/draft.ts',s=>s.replace('const baseTexts = {','const baseTexts = {\n'+names.map((n,i)=>`  '${keys[i]}': 'Item Class: ${i<3?'One':'Two'} Hand Maces\\nRarity: Normal\\n${n}',`).join('\n')).replace("    | 'broadhead-quiver'","    | 'broadhead-quiver'\n"+keys.map(k=>`    | '${k}'`).join('\n')))
edit('frontend/src/features/crafting/craftingApi.ts',s=>s.replace("    | 'broadhead-quiver'","    | 'broadhead-quiver'\n"+keys.map(k=>`    | '${k}'`).join('\n')))
edit('frontend/src/features/crafting/CraftingPage.tsx',s=>s.replace('const baseSlugs = {','const baseSlugs = {\n'+names.map((n,i)=>`  '${keys[i]}': '${n.replaceAll(' ','_')}',`).join('\n')).replaceAll("topBase(catalogBase)?.family === 'quivers'", "['one-hand-maces', 'two-hand-maces'].includes(topBase(catalogBase)?.family ?? '')\n                ? (topBase(catalogBase)?.family === 'one-hand-maces' ? 'One Hand Maces' : 'Two Hand Maces')\n                : topBase(catalogBase)?.family === 'quivers'").replace(/(id: 'source-property-' \+ i,\s*text:\s*\[\s*)'bows'/,"$1'one-hand-maces',\n 'two-hand-maces',\n 'bows'"))
for(const path of ['frontend/src/features/crafting/topBases.ts','frontend/src/features/crafting/workbenchApi.ts'])edit(path,s=>s.replace("      'quivers',","      'quivers',\n      'one-hand-maces',\n      'two-hand-maces',"))
