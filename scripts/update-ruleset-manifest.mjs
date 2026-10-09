// Explicitly run after reviewing changes; this updates data digests, never rules or provenance.
import fs from 'node:fs'
import path from 'node:path'
import crypto from 'node:crypto'
import { fileURLToPath } from 'node:url'
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const resources = path.join(root, 'backend/src/main/resources')
const target = path.join(resources, 'crafting/ruleset-v1.json')
const manifest = JSON.parse(fs.readFileSync(target, 'utf8'))
const files = []
function visit(folder) {
  for (const entry of fs.readdirSync(folder, { withFileTypes: true })) {
    const file = path.join(folder, entry.name)
    if (entry.isDirectory()) visit(file)
    else if (entry.name.endsWith('.json') && file !== target) files.push(file)
  }
}
visit(path.join(resources, 'catalog'))
for (const name of ['registry-v2.json', 'supported-base-sets-v1.json', 'goalfilter/definitions-v1.json', 'goalfilter/bases-v1.json']) files.push(path.join(resources, 'crafting', name))
manifest.files = Object.fromEntries(files.sort().map(file => [path.relative(resources, file).replaceAll('\\', '/'), crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex')]))
fs.writeFileSync(target, JSON.stringify(manifest, null, 2) + '\n')
console.log(`Reviewed ruleset ${manifest.rulesetVersion}: ${files.length} resource digests updated. Review the diff before committing.`)
