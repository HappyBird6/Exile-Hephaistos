import fs from 'node:fs'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
const root = '/qa/importer-replay-1'
assert(!fs.existsSync(root), 'Preserve previous importer evidence')
const dirs = ['backend/src/main/resources/catalog/stocky-mitts', 'docs/evidence/armour-source-bundle-2026-10-04', 'docs/evidence/gloves-runtime-bundle-2026-10-04']
for (const dir of dirs) fs.cpSync('/source/' + dir, root + '/' + dir, { recursive: true, filter: p => !p.endsWith('.png') })
const files = ['backend/src/main/resources/catalog/top-bases.json', 'frontend/src/features/crafting/topBases.json', 'frontend/src/shared/i18n/gameTerms.json', 'frontend/src/shared/i18n/modifierTemplates.json']
const pools = ['massive-mitts', 'sirenscale-gloves', 'adherent-cuffs']
for (const pool of pools) {
  const dir = 'backend/src/main/resources/catalog/' + pool
  fs.cpSync('/source/' + dir, root + '/' + dir, { recursive: true })
  files.push(dir + '/catalog.json')
}
for (const file of files) {
  fs.mkdirSync(root + '/' + file.slice(0, file.lastIndexOf('/')), { recursive: true })
  fs.copyFileSync('/source/' + file, root + '/' + file)
}
fs.mkdirSync(root + '/frontend/src/features/crafting', { recursive: true })
execFileSync(process.execPath, ['/source/scripts/import-gloves-bundle.mjs'], { cwd: root, stdio: 'pipe' })
const read = p => JSON.parse(fs.readFileSync(p, 'utf8'))
for (const file of [...files, 'backend/src/main/resources/catalog/top-base-essences.json', 'frontend/src/features/crafting/topBaseEssences.json']) assert.deepEqual(read(root + '/' + file), read('/source/' + file), file)
fs.writeFileSync('/qa/importer-replay-results.json', JSON.stringify({ passed: true, comparedDocuments: files.length + 2, pools, assertions: 'Exact catalog identities, definitions, hashes, original timestamps, six-language terms/templates, top-base manifests and complete Essence targets survive cached re-import' }, null, 2) + '\n')
console.log('Exact cached importer replay passed')
