import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
const root = 'docs/evidence/wands-source-bundle-2026-10-05'
const roster = JSON.parse(fs.readFileSync(`${root}/applicability.json`))
for (const base of roster.bases) {
  const path = `${root}/${base.name}_Wand.us.html`
  assert(!fs.existsSync(path),'Preserve previous evidence')
  const response = await fetch(`https://poe2db.tw/us/${base.name}_Wand`)
  assert(response.ok)
  fs.writeFileSync(path,await response.text())
}
console.log('Preserved complete source HTML for built-in skill IDs and properties')
