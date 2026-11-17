// Explicitly run after reviewing changes; this updates data digests, never rules or provenance.
import fs from 'node:fs'
import { gameDataOutputs } from './game-data-outputs.mjs'
import assert from 'node:assert/strict'
import path from 'node:path'
import crypto from 'node:crypto'
import { fileURLToPath } from 'node:url'
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const resources = path.join(root, 'docs/v0.5.5/game-data')
const target = path.join(resources, 'crafting/ruleset-v1.json')
const manifest = JSON.parse(fs.readFileSync(target, 'utf8'))
const generated = gameDataOutputs(path.join(root, 'docs/v0.5.5'))
const sealedCrafting = new Set(['goalfilter/base-inventory-v5-readonly.json', 'registry-v2.json', 'workbench-definitions-v1.json', 'automatic-currency-policy-v1.json', 'supported-base-sets-v1.json', 'goalfilter/definitions-v1.json', 'goalfilter/definitions-v3-readonly.json', 'goalfilter/definitions-v4-readonly.json', 'goalfilter/definitions-v5-readonly.json', 'goalfilter/bases-v1.json'])
const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex')
const prefix = 'backend/src/main/resources/'
const entries = [...generated].filter(([file]) => file.startsWith(prefix + 'catalog/') || file.startsWith(prefix + 'crafting/') && sealedCrafting.has(file.slice((prefix + 'crafting/').length))).map(([file, { bytes }]) => [file.slice(prefix.length), hash(bytes)])
const filesByDigest = Object.fromEntries(entries.sort(([a], [b]) => a.localeCompare(b, 'en')))
if (process.argv.includes('--check')) assert.deepStrictEqual(manifest.files, filesByDigest, 'Ruleset resource digests differ; review changes before updating the manifest')
else { manifest.files = filesByDigest; fs.writeFileSync(target, JSON.stringify(manifest, null, 2) + '\n') }
console.log(process.argv.includes('--check') ? `Reviewed ruleset: ${entries.length} resource digests verified` : `Reviewed ruleset ${manifest.rulesetVersion}: ${entries.length} resource digests updated. Review the diff before committing.`)
