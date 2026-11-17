import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import crypto from 'node:crypto'
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
const files = directory => fs.readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
  const file = path.join(directory, entry.name)
  return entry.isDirectory() ? files(file) : [file]
}).sort()

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
  assert.equal(manifest.schemaVersion, 1)
  assert.equal(manifest.dataVersion, '0.5.5')
  const emit = (file, contents) => {
    if (write) { fs.mkdirSync(path.dirname(file), { recursive: true }); fs.writeFileSync(file, contents) }
    else assert(fs.existsSync(file) && fs.readFileSync(file).equals(Buffer.from(contents)), `Data drift: ${path.relative(root, file)}; review source and run node scripts/versioned-game-data.mjs --write`)
  }
  const resources = new Map(), paths = new Set(), runtimePaths = new Set()
  const sourceFiles = files(directory).filter(file => /\/(game-data|i18n)\//.test(file)).map(file => path.relative(directory, file).replaceAll('\\', '/'))
  const entries = manifest.resources.map(entry => {
    assert(/^(game-data|i18n)\/.+\.json$/.test(entry.path) && !entry.path.split('/').includes('..'))
    assert(/^(backend\/src\/main\/resources\/(catalog|crafting)\/|frontend\/src\/)/.test(entry.runtimePath) && entry.runtimePath.endsWith('.json') && !entry.runtimePath.split('/').includes('..'))
    assert(!paths.has(entry.path) && !runtimePaths.has(entry.runtimePath), 'Duplicate resource or mirror path')
    paths.add(entry.path); runtimePaths.add(entry.runtimePath)
    const bytes = fs.readFileSync(path.join(directory, entry.path))
    resources.set(entry.path, JSON.parse(bytes))
    return { path: entry.path, runtimePath: entry.runtimePath, sha256: digest(bytes) }
  })
  assert.deepEqual([...paths].sort(), sourceFiles.sort(), 'Every canonical JSON must have an explicit runtime destination in manifest.resources')
  const runtimeGameFiles = ['catalog', 'crafting'].flatMap(folder => files(path.join(root, 'backend/src/main/resources', folder))).filter(file => file.endsWith('.json')).map(file => path.relative(root, file).replaceAll('\\', '/'))
  assert.deepEqual([...runtimePaths].filter(file => file.startsWith('backend/')).sort(), runtimeGameFiles.sort(), 'Backend game resource inventory differs')
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
  for (const entry of entries) emit(path.join(root, entry.runtimePath), fs.readFileSync(path.join(directory, entry.path)))
  for (const [file, contents] of outputs) emit(file, contents)
  const rules = files(path.join(directory, 'game-rule')).map(file => {
    assert(file.endsWith('.md'), 'Game rules must be Markdown')
    return { path: path.relative(directory, file).replaceAll('\\', '/'), sha256: digest(fs.readFileSync(file)) }
  })
  emit(target, json({ ...manifest, resources: entries, mappings, rules }))
  console.log(`Versioned data verified: ${entries.length} resources, ${mappings.reduce((n, row) => n + row.count, 0)} IDs, ${rules.length} rules`)
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  assert(process.argv.length === 3 && ['--write', '--check'].includes(process.argv[2]), 'Use --check or --write')
  synchronize(fileURLToPath(new URL('..', import.meta.url)), process.argv[2] === '--write')
}
