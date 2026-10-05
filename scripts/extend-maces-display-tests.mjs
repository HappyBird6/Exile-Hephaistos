import fs from 'node:fs'
import assert from 'node:assert/strict'
const path='frontend/src/features/crafting/localizedModifiers.test.ts'
const compoundPath='frontend/src/features/crafting/remainingDisplay.test.tsx'
const bindings=JSON.parse(fs.readFileSync('frontend/src/shared/i18n/modifierTemplates.json')).definitions
const mace=/^(one-hand-maces|two-hand-maces|fortified-hammer|strife-pick|akoyan-club|ruination-maul|fanatic-greathammer|tawhoan-greatclub):/
const ids=Object.keys(bindings).filter(id=>mace.test(id))
const single=ids.filter(id=>bindings[id].valueStats&&bindings[id].stats.length===1)
const compound=ids.filter(id=>bindings[id].valueStats&&bindings[id].stats.length>1&&bindings[id].stats.some(s=>s.min!==s.max))
console.log({newDefinitions:ids.length,single:single.length,compound:compound.length})
for(const p of [path,compoundPath]) {
  let text=fs.readFileSync(p,'utf8')
  // Existing expected counts retain their historical partitions; new rows are independently asserted.
  text=text.replaceAll('offhand|quiver|.+-quiver):','offhand|quiver|.+-quiver|one-hand-maces|two-hand-maces|fortified-hammer|strife-pick|akoyan-club|ruination-maul|fanatic-greathammer|tawhoan-greatclub):')
  text=text.replaceAll('crossbow|.+-crossbow|offhand):','crossbow|.+-crossbow|offhand|one-hand-maces|two-hand-maces|fortified-hammer|strife-pick|akoyan-club|ruination-maul|fanatic-greathammer|tawhoan-greatclub):')
  text=text.replace(').toHaveLength(1955)',`).toHaveLength(${1955+ids.length})`)
  text=text.replace('expect(compound).toHaveLength(436)',`expect(compound.filter(([id]) => /${mace.source}/.test(id))).toHaveLength(${compound.length})\n    expect(compound).toHaveLength(${436+compound.length})`)
  text=text.replace('expect(single).toHaveLength(317)',`expect(single.filter(([id]) => /${mace.source}/.test(id))).toHaveLength(${single.length})\n    expect(single).toHaveLength(${317+single.length})`)
  assert.notEqual(text,fs.readFileSync(p,'utf8'),p)
  fs.writeFileSync(p,text)
}
