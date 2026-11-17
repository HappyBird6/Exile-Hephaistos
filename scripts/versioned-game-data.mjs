import assert from 'node:assert/strict'
import { dataFiles as files } from './catalog-data.mjs'
import { gameDataOutputs } from './game-data-outputs.mjs'
import fs from 'node:fs'
import path from 'node:path'
import crypto from 'node:crypto'
import { execFileSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

export const versionRoot = 'docs/v0.5.5'
export const domains = {
  bases: 'b', modifiers: 'm', stats: 's', materials: 'r', actions: 'c',
  omens: 'o', augments: 'a', tags: 't', families: 'f', pseudos: 'p',
  itemClasses: 'k', terms: 'n', templates: 'x', messages: 'u',
}
const digest = bytes => crypto.createHash('sha256').update(bytes).digest('hex')
const json = value => JSON.stringify(value, null, 2) + '\n'
const read = file => JSON.parse(fs.readFileSync(file, 'utf8'))
// Append-only allocation: removed keys keep their ID and become retired tombstones.
export function allocate(previous, keys, domain) {
  const prefix = domains[domain]
  assert(prefix, `Unknown domain: ${domain}`)
  const active = new Set(keys)
  const used = new Set(), identities = new Set()
  let next = 1
  const entries = (previous?.entries ?? []).map(entry => {
    assert.equal(typeof entry.sourceKey, 'string')
    assert(entry.sourceKey.length && !identities.has(entry.sourceKey), `Duplicate source key: ${entry.sourceKey}`)
    assert.equal(typeof entry.retired, 'boolean', `Missing retirement state: ${entry.id}`)
    assert(new RegExp(`^${prefix}[0-9a-z]{4,}$`).test(entry.id), `Invalid ID: ${entry.id}`)
    const ordinal = parseInt(entry.id.slice(1), 36)
    assert(Number.isSafeInteger(ordinal) && ordinal > 0 && ordinal < Number.MAX_SAFE_INTEGER)
    assert.equal(entry.id, prefix + ordinal.toString(36).padStart(4, '0'), `Noncanonical ID: ${entry.id}`)
    assert(!used.has(entry.id), `Duplicate ID: ${entry.id}`)
    identities.add(entry.sourceKey); used.add(entry.id)
    next = Math.max(next, ordinal + 1)
    return { id: entry.id, sourceKey: entry.sourceKey, retired: !active.has(entry.sourceKey) }
  })
  if (previous) {
    assert.equal(previous.schemaVersion, 1)
    assert.equal(previous.domain, domain)
    assert.equal(previous.prefix, prefix)
  }
  for (const key of [...active].sort()) {
    assert.equal(typeof key, 'string')
    assert(key.length)
    if (!identities.has(key)) {
      entries.push({ id: prefix + (next++).toString(36).padStart(4, '0'), sourceKey: key, retired: false })
    }
  }
  return { schemaVersion: 1, domain, prefix, entries }
}

export function collect(resources) {
  const keys = Object.fromEntries(Object.keys(domains).map(domain => [domain, new Set()]))
  const add = (domain, value) => { if (typeof value === 'string' && value) keys[domain].add(value) }
  const walk = value => {
    if (Array.isArray(value)) { value.forEach(walk); return }
    if (!value || typeof value !== 'object') return
    if (value.affixType && Array.isArray(value.stats)) add('modifiers', value.id)
    for (const stat of value.stats ?? []) {
      if (stat && typeof stat === 'object') add('stats', stat.id ?? stat.sourceStatId)
    }
    add('stats', value.sourceStatId)
    add('stats', value.stat)
    for (const stat of value.sourceStatIds ?? []) add('stats', stat)
    for (const tag of value.tags ?? []) add('tags', tag)
    for (const family of value.familyIds ?? []) add('families', family)
    add('itemClasses', value.itemClass)
    Object.values(value).forEach(walk)
  }
  for (const [name, value] of resources) {
    // Raw snapshots are evidence, not an additional identity registry.
    if (!name.endsWith('.raw.json')) walk(value)
    if (name === 'game-data/catalog/top-bases.json') Object.keys(value).forEach(key => add('bases', key))
    if (name === 'game-data/catalog/base-policies.json') Object.keys(value.legacy).forEach(key => add('bases', key))
    if (name === 'game-data/catalog/augments.json') value.forEach(row => add('augments', row.id))
    if (name === 'game-data/crafting/registry-v2.json') value.entries.forEach(row => add('materials', row.id))
    if (name === 'game-data/crafting/workbench-definitions-v1.json') {
      value.currencies.forEach(row => add('actions', row.id))
      value.omens.forEach(row => add('omens', row.key))
    }
    if (name.startsWith('game-data/crafting/goalfilter/definitions-')) (value.pseudos ?? []).forEach(row => add('pseudos', row.id))
    if (name === 'i18n/itemClassNames.json') Object.keys(value).forEach(key => add('itemClasses', key))
    if (name === 'i18n/gameTerms.json' || name === 'i18n/messages.json') {
      const domain = name.endsWith('/messages.json') ? 'messages' : 'terms'
      Object.values(value).forEach(locale => Object.keys(locale).forEach(key => add(domain, key)))
    }
    if (name === 'i18n/modifierTemplates.json') {
      Object.keys(value.definitions).forEach(key => add('modifiers', key))
      Object.values(value.templates).forEach(locale => Object.keys(locale).forEach(key => add('templates', key)))
    }
  }
  return keys
}

export function synchronize(root, write = false) {
  const directory = path.join(root, versionRoot)
  const target = path.join(directory, 'manifest.json')
  const manifest = read(target)
  assert([1, 2].includes(manifest.schemaVersion), 'Unknown source manifest version')
  assert.equal(manifest.dataVersion, '0.5.5')
  const emit = (file, contents) => {
    const unchanged = fs.existsSync(file) && fs.readFileSync(file).equals(Buffer.from(contents))
    if (write) {
      if (!unchanged) { fs.mkdirSync(path.dirname(file), { recursive: true }); fs.writeFileSync(file, contents) }
    } else assert(unchanged, `Data drift: ${path.relative(root, file)}; review source and run node scripts/versioned-game-data.mjs --write`)
  }
  const generated = gameDataOutputs(directory)
  const runtimePaths = new Set(generated.keys())
  const sourceFiles = ['game-data', 'i18n'].flatMap(folder => files(path.join(directory, folder)))
  const entries = sourceFiles.map(file => ({ path: path.relative(directory, file).replaceAll('\\', '/'), sha256: digest(fs.readFileSync(file)) })).sort((a, b) => a.path.localeCompare(b.path, 'en'))
  // Stable IDs are collected from the executable projections, independent of source layout.
  const resources = new Map([...generated].map(([file, { value }]) => [
    file.startsWith('backend/src/main/resources/') ? 'game-data/' + file.slice('backend/src/main/resources/'.length)
      : file.startsWith('frontend/src/shared/i18n/') ? 'i18n/' + file.slice('frontend/src/shared/i18n/'.length)
      : 'game-data/display/' + file.slice('frontend/src/features/crafting/'.length), value,
  ]))
  const runtimeGameFiles = ['catalog', 'crafting'].flatMap(folder => files(path.join(root, 'backend/src/main/resources', folder))).filter(file => file.endsWith('.json')).map(file => path.relative(root, file).replaceAll('\\', '/'))
  const previousMirrors = (manifest.generated ?? manifest.resources ?? []).map(entry => entry.runtimePath).filter(file => {
    if (file == null) return false
    assert(typeof file === 'string' && !file.includes('\\') && file.split('/').every(part => part && part !== '.' && part !== '..'), 'Invalid generated resource path')
    assert(file.startsWith('backend/src/main/resources/') || file.startsWith('frontend/src/'), 'Invalid generated resource directory')
    return fs.existsSync(path.join(root, file))
  })
  const orphaned = [...runtimeGameFiles, ...previousMirrors].filter(file => !runtimePaths.has(file))
  assert.deepEqual([...new Set(orphaned)], [], 'Source data was removed or a runtime file has no canonical source; review and archive it explicitly')
  const keys = collect(resources), mappings = []
  // Validate every mapping before writing any runtime mirrors.
  const outputs = []
  for (const domain of Object.keys(domains)) {
    const relative = `mappings/${domain}.json`, file = path.join(directory, relative)
    assert(fs.existsSync(file) || !manifest.mappings?.some(row => row.path === relative), `Published mapping is missing: ${relative}; restore it, never reassign IDs`)
    const value = allocate(fs.existsSync(file) ? read(file) : null, keys[domain], domain)
    const contents = json(value)
    outputs.push([file, contents])
    mappings.push({ path: relative, domain, count: value.entries.length, sha256: digest(contents) })
  }
  assert.deepEqual(files(path.join(directory, 'mappings')).map(file => path.relative(directory, file).replaceAll('\\', '/')).sort(), mappings.filter(row => fs.existsSync(path.join(directory, row.path))).map(row => row.path).sort(), 'Unexpected mapping file')
  for (const [file, { bytes }] of generated) emit(path.join(root, file), bytes)
  for (const [file, contents] of outputs) emit(file, contents)
  const { rules, resources: previousResources, generated: previousGenerated, ...metadata } = manifest
  emit(target, json({ ...metadata, schemaVersion: 2, resources: entries, generated: [...generated].map(([runtimePath, { bytes }]) => ({ runtimePath, sha256: digest(bytes) })).sort((a, b) => a.runtimePath.localeCompare(b.runtimePath, 'en')), mappings }))
  console.log(`Versioned data verified: ${entries.length} resources, ${mappings.reduce((n, row) => n + row.count, 0)} IDs`)
}

export function synchronizeAll(root, write = false) {
  const flag = write ? '--write' : '--check'
  const run = script => execFileSync(process.execPath, [path.join(root, 'scripts', script), flag], { cwd: root, stdio: 'inherit' })
  // Seal the generated runtime data, then write mirrors from the same canonical source.
  // --check never updates seals, mirrors or generated documentation.
  run('update-ruleset-manifest.mjs')
  synchronize(root, write)
  run('sync-workbench-data.mjs')
  run('sync-public-contracts.mjs')
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  assert(process.argv.length === 3 && ['--write', '--check'].includes(process.argv[2]), 'Use --check or --write')
  synchronizeAll(fileURLToPath(new URL('..', import.meta.url)), process.argv[2] === '--write')
}
