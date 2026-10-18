// Refresh provenance after a reviewed data edit. Never changes items, goals or numeric oracles.
import fs from 'node:fs'
import path from 'node:path'
import crypto from 'node:crypto'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const resources = path.join(root, 'backend/src/main/resources')
const manifest = JSON.parse(fs.readFileSync(path.join(resources, 'crafting/ruleset-v1.json'), 'utf8'))
const hash = (value) => crypto.createHash('sha256').update(value).digest('hex')
// RulesetManifestLoader.Manifest stores a TreeMap and hashes its Java Map.toString representation.
const files = '{' + Object.entries(manifest.files).sort(([a], [b]) => a < b ? -1 : a > b ? 1 : 0).map(([key, value]) => `${key}=${value}`).join(', ') + '}'
const identity = manifest.rulesetVersion + '-' + hash([manifest.rulesetVersion, manifest.gameSeason, manifest.gamePatch, manifest.engineRuleVersion, manifest.ledgerVersion, files].join('|'))
const target = path.join(root, 'contracts/crafting-paths-v1/solar-source-fixture.json')
const fixture = JSON.parse(fs.readFileSync(target, 'utf8'))
fixture.rulesetIdentity = identity
for (const source of fixture.sources) source.sha256 = hash(fs.readFileSync(path.join(resources, source.path)))
fs.writeFileSync(target, JSON.stringify(fixture, null, 2) + '\n')
console.log(`Source fixture provenance refreshed: ${identity}; goals and numeric oracles preserved.`)
