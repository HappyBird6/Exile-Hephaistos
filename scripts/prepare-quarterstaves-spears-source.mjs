import fs from 'node:fs'
import assert from 'node:assert/strict'
const root='docs/evidence/quarterstaves-spears-source-bundle-2026-10-05'
const pages="['Quarterstaves', 'Spears']"
for (const [from,to] of [['collect-maces-bundle.mjs','collect-quarterstaves-spears-bundle.mjs'],['collect-maces-details.mjs','collect-quarterstaves-spears-details.mjs']]) {
  const path=`scripts/${to}`
  assert(!fs.existsSync(path),'Preserve existing source scripts')
  let source=fs.readFileSync(`scripts/${from}`,'utf8').replaceAll('docs/evidence/maces-source-bundle-2026-10-05',root).replaceAll("['One_Hand_Maces', 'Two_Hand_Maces']",pages)
  source=source.replace("['Fortified_Hammer', 'Strife_Pick', 'Akoyan_Club', 'Ruination_Maul', 'Fanatic_Greathammer', 'Tawhoan_Greatclub']","['Aegis_Quarterstaff', 'Bolting_Quarterstaff', 'Dreaming_Quarterstaff', 'Grand_Spear', 'Flying_Spear', 'Akoyan_Spear']")
  fs.writeFileSync(path,source,{flag:'wx'})
}
