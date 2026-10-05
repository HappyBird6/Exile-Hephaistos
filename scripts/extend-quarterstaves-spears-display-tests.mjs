import fs from 'node:fs'
import assert from 'node:assert/strict'
const bindings=JSON.parse(fs.readFileSync('frontend/src/shared/i18n/modifierTemplates.json')).definitions
const pattern=/^(quarterstaves|spears|aegis-quarterstaff|bolting-quarterstaff|dreaming-quarterstaff|grand-spear|flying-spear|akoyan-spear):/
const ids=Object.keys(bindings).filter(id=>pattern.test(id))
const single=ids.filter(id=>bindings[id].valueStats&&bindings[id].stats.length===1)
const compound=ids.filter(id=>bindings[id].valueStats&&bindings[id].stats.length>1&&bindings[id].stats.some(s=>s.min!==s.max))
for(const path of ['frontend/src/features/crafting/localizedModifiers.test.ts','frontend/src/features/crafting/remainingDisplay.test.tsx']) {
 let s=fs.readFileSync(path,'utf8')
 // Extend only negative historical partition filters, leaving the prior Mace positive assertions intact.
 s=s.replace(/!\/\^\(([^]*?)tawhoan-greatclub\):/g,'!/^($1tawhoan-greatclub|quarterstaves|spears|aegis-quarterstaff|bolting-quarterstaff|dreaming-quarterstaff|grand-spear|flying-spear|akoyan-spear):')
 s=s.replace(').toHaveLength(1984)',`).toHaveLength(${1984+ids.length})`)
 s=s.replace('expect(compound).toHaveLength(436)',`expect(compound.filter(([id])=>/${pattern.source}/.test(id))).toHaveLength(${compound.length})\n expect(compound).toHaveLength(${436+compound.length})`)
 s=s.replace('expect(single).toHaveLength(341)',`expect(single.filter(([id])=>/${pattern.source}/.test(id))).toHaveLength(${single.length})\n expect(single).toHaveLength(${341+single.length})`)
 assert.notEqual(s,fs.readFileSync(path,'utf8'),path)
 fs.writeFileSync(path,s)
}
console.log({newDefinitions:ids.length,single:single.length,compound:compound.length})
