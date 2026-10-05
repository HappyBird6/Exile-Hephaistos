import fs from 'node:fs'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
const q = 'E:/WORK/Exile-Hephaistos/codex/hallowed-qa-20261004'
const root = `${q}/importer-replay`
assert(!fs.existsSync(root), 'Preserve previous replay')
fs.mkdirSync(root)
for (const p of ['backend/src/main/resources', 'frontend/src', 'docs/evidence/hallowed-source-bundle-2026-10-04']) fs.cpSync(p, `${root}/${p}`, { recursive: true })
fs.mkdirSync(`${root}/scripts`)
for (const p of ['import-hallowed-bundle.mjs', 'armour-source.mjs', 'verify-hallowed-source.mjs']) fs.copyFileSync(`scripts/${p}`, `${root}/scripts/${p}`)
execFileSync(process.execPath, ['scripts/import-hallowed-bundle.mjs'], { cwd: root })
execFileSync(process.execPath, ['scripts/verify-hallowed-source.mjs'], { cwd: root })
const files = ['backend/src/main/resources/catalog/top-bases.json', 'frontend/src/features/crafting/topBases.json', 'backend/src/main/resources/catalog/top-base-essences.json', 'frontend/src/features/crafting/topBaseEssences.json', 'frontend/src/shared/i18n/gameTerms.json', 'backend/src/main/resources/crafting/registry-v2.json', ...['catalog.json', 'base.raw.json', 'details.raw.json'].map(p => `backend/src/main/resources/catalog/hallowed-sceptre/${p}`)]
for (const p of files) assert.deepEqual(JSON.parse(fs.readFileSync(p)), JSON.parse(fs.readFileSync(`${root}/${p}`)), p)
fs.writeFileSync(`${q}/importer-replay-results.json`, JSON.stringify({ passed: true, count: files.length, files, policy: 'Replay in fresh isolated copy; semantic comparison allows existing formatter whitespace' }, null, 2) + '\n')
console.log(files.length, 'importer replay outputs equivalent')
