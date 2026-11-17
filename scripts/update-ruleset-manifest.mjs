// Explicitly run after reviewing changes; this updates data digests, never rules or provenance.
import fs from 'node:fs'
import { catalogOutputs } from './catalog-data.mjs'
import assert from 'node:assert/strict'
import path from 'node:path'
import crypto from 'node:crypto'
import { fileURLToPath } from 'node:url'
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const resources = path.join(root, 'docs/v0.5.5/game-data')
const target = path.join(resources, 'crafting/ruleset-v1.json')
const manifest = JSON.parse(fs.readFileSync(target, 'utf8'))
const files = []
const catalog = catalogOutputs(path.join(resources, 'catalog')).filter(row => row.runtimePath !== null)
files.push(path.join(resources, 'crafting/goalfilter/base-inventory-v5-readonly.json'))
for (const name of ['registry-v2.json', 'workbench-definitions-v1.json', 'automatic-currency-policy-v1.json', 'supported-base-sets-v1.json', 'goalfilter/definitions-v1.json', 'goalfilter/definitions-v3-readonly.json', 'goalfilter/definitions-v4-readonly.json', 'goalfilter/definitions-v5-readonly.json', 'goalfilter/bases-v1.json']) files.push(path.join(resources, 'crafting', name))
const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex')
const entries = [...catalog.map(row => ['catalog/' + row.runtimePath, hash(row.runtimeBytes)]), ...files.map(file => [path.relative(resources, file).replaceAll('\\', '/'), hash(fs.readFileSync(file))])]
const filesByDigest = Object.fromEntries(entries.sort(([a], [b]) => a.localeCompare(b, 'en')))
if (process.argv.includes('--check')) assert.deepStrictEqual(manifest.files, filesByDigest, 'Ruleset resource digests differ; review changes before updating the manifest')
else { manifest.files = filesByDigest; fs.writeFileSync(target, JSON.stringify(manifest, null, 2) + '\n') }
console.log(process.argv.includes('--check') ? `Reviewed ruleset: ${entries.length} resource digests verified` : `Reviewed ruleset ${manifest.rulesetVersion}: ${entries.length} resource digests updated. Review the diff before committing.`)
