import fs from 'node:fs'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
const attempt = process.argv[2] ?? '1'
assert(/^[1-9][0-9]*$/.test(attempt))
const root = `/qa/evasion-importer-replay-${attempt}`
assert(!fs.existsSync(root), 'Preserve existing importer replay output')
for (const dir of ['backend/src/main/resources/catalog', 'docs/evidence/armour-source-bundle-2026-10-04', 'docs/evidence/evasion-gloves-runtime-bundle-2026-10-04', 'docs/evidence/evasion-helmets-runtime-bundle-2026-10-04', 'frontend/src/features/crafting', 'frontend/src/shared/i18n']) fs.cpSync('/source/' + dir, root + '/' + dir, { recursive: true, filter: p => !p.endsWith('.png') })
for (const flag of ['--evasion', '--helmets']) {
  const output = execFileSync(process.execPath, ['/source/scripts/import-gloves-bundle.mjs', flag], { cwd: root, encoding: 'utf8' })
  fs.appendFileSync(`/qa/importer-replay-${attempt}.log`, output)
}
const read = p => JSON.parse(fs.readFileSync(p, 'utf8'))
const files = ['backend/src/main/resources/catalog/top-bases.json', 'backend/src/main/resources/catalog/top-base-essences.json', 'frontend/src/features/crafting/topBases.json', 'frontend/src/features/crafting/topBaseEssences.json', 'frontend/src/shared/i18n/gameTerms.json', 'frontend/src/shared/i18n/modifierTemplates.json']
for (const pool of ['polished-bracers', 'blacksteel-gauntlets', 'war-wraps', 'freebooter-cap', 'gladiatorial-helm', 'grinning-mask']) for (const file of ['catalog.json', 'base.raw.json', 'details.raw.json']) files.push(`backend/src/main/resources/catalog/${pool}/${file}`)
for (const file of files) assert.deepEqual(read(root + '/' + file), read('/source/' + file), file)
fs.writeFileSync(`/qa/importer-replay-results-${attempt}.json`, JSON.stringify({ passed: true, comparedDocuments: files.length, assertions: 'Complete pools, stable source IDs, historical definitions and templates, hashes, snapshots and source timestamps are identical after cached replay' }, null, 2) + '\n')
console.log(files.length, 'exact cached replay documents passed')
