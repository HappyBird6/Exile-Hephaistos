import fs from '../evidence-files.cjs'
const read = (file) => JSON.parse(fs.readFileSync(file,'utf8'))
const write = (file,value) => fs.writeFileSync(file,JSON.stringify(value,null,2)+'\n')
const bases = read('backend/src/main/resources/catalog/top-bases.json')
const summary = read('evidence:base-audit/import-summary.json')
const canonical = read('backend/src/main/resources/catalog/manual-reveal.json')
const front = read('frontend/src/features/crafting/manualRevealDefinitions.json')
const sets = read('backend/src/main/resources/crafting/supported-base-sets-v1.json')
for (const row of summary) {
  if (!bases[row.key] || !bases[row.template]) throw new Error(`Unreviewed base template ${row.key}`)
  const ids = canonical.bases[bases[row.template].id]
  if (!ids) throw new Error(`No reviewed class reveal eligibility for ${row.template}`)
  canonical.bases[bases[row.key].id] = [...ids]
  front.bases[bases[row.key].id] = [...ids]
  for (const set of Object.values(sets.sets)) if (set.includes(row.template) && !set.includes(row.key)) set.push(row.key)
}
write('backend/src/main/resources/catalog/manual-reveal.json',canonical)
write('frontend/src/features/crafting/manualRevealDefinitions.json',front)
write('backend/src/main/resources/crafting/supported-base-sets-v1.json',sets)
