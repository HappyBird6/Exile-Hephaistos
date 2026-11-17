import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import { allocate, domains, runtimePathFor, synchronize, versionRoot } from '../versioned-game-data.mjs'

test('IDs survive sorting changes, retirement, restoration and JSON round trips', () => {
  const initial = allocate(null, ['zulu', 'middle'], 'bases')
  const next = allocate(JSON.parse(JSON.stringify(initial)), ['alpha', 'zulu'], 'bases')
  const byKey = value => Object.fromEntries(value.entries.map(row => [row.sourceKey, row]))
  assert.equal(byKey(next).zulu.id, byKey(initial).zulu.id)
  assert.equal(byKey(next).middle.retired, true)
  assert(!initial.entries.some(row => row.id === byKey(next).alpha.id))
  const restored = allocate(next, ['middle', 'alpha', 'zulu'], 'bases')
  assert.equal(byKey(restored).middle.id, byKey(initial).middle.id)
  assert.equal(byKey(restored).middle.retired, false)
  assert.deepEqual(allocate(restored, ['zulu', 'middle', 'alpha'], 'bases'), restored)
})

test('ambiguous or malformed registries fail instead of silently assigning new IDs', () => {
  const registry = allocate(null, ['one', 'two'], 'modifiers')
  const duplicateId = structuredClone(registry)
  duplicateId.entries[1].id = duplicateId.entries[0].id
  assert.throws(() => allocate(duplicateId, ['one', 'two'], 'modifiers'), /Duplicate ID/)
  const duplicateKey = structuredClone(registry)
  duplicateKey.entries[1].sourceKey = duplicateKey.entries[0].sourceKey
  assert.throws(() => allocate(duplicateKey, ['one', 'two'], 'modifiers'), /Duplicate source key/)
  assert.throws(() => allocate(registry, ['one'], 'stats'), /Invalid ID/)
  const malformed = structuredClone(registry)
  malformed.entries[0].id = 'm00001'
  assert.throws(() => allocate(malformed, ['one'], 'modifiers'), /Noncanonical ID/)
  assert.equal(new Set(Object.values(domains)).size, Object.keys(domains).length)
})

function fixture(t) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'versioned-data-'))
  t.after(() => fs.rmSync(root, { recursive: true, force: true }))
  const write = (name, value) => {
    const file = path.join(root, name)
    fs.mkdirSync(path.dirname(file), { recursive: true })
    fs.writeFileSync(file, typeof value === 'string' ? value : JSON.stringify(value))
  }
  const resources = [
    { path: 'game-data/catalog/top-bases.json', runtimePath: 'backend/src/main/resources/catalog/top-bases.json' },
    { path: 'game-data/crafting/workbench-definitions-v1.json', runtimePath: 'backend/src/main/resources/crafting/workbench-definitions-v1.json' },
    { path: 'i18n/messages.json', runtimePath: 'frontend/src/shared/i18n/messages.json' },
  ]
  const values = ['{\r\n "zulu": {"key":"zulu"}\r\n}\r\n', { currencies: [{ id: 'CHAOS' }], omens: [] }, { en: { hello: 'Hello' }, ko: { hello: '안녕' } }]
  resources.forEach((entry, i) => { write(`${versionRoot}/${entry.path}`, values[i]); write(entry.runtimePath, values[i]) })
  write(`${versionRoot}/manifest.json`, { schemaVersion: 1, dataVersion: '0.5.5', resources })
  write(`${versionRoot}/game-rule/currency.md`, '# Rules\n')
  fs.mkdirSync(path.join(root, versionRoot, 'mappings'))
  synchronize(root, true)
  return { root, write, resources, read: name => fs.readFileSync(path.join(root, name)) }
}

test('canonical bytes are preserved and read-only checks reject stale runtime data', t => {
  const { root, read, write, resources } = fixture(t)
  const entry = resources[0]
  assert.deepEqual(read(entry.runtimePath), read(`${versionRoot}/${entry.path}`))
  synchronize(root)
  const original = read(`${versionRoot}/manifest.json`)
  write(entry.runtimePath, '{}')
  assert.throws(() => synchronize(root), /Data drift/)
  assert.equal(read(entry.runtimePath).toString(), '{}')
  assert.deepEqual(read(`${versionRoot}/manifest.json`), original)
  synchronize(root, true)
  synchronize(root)
  const mirror = path.join(root, entry.runtimePath)
  fs.utimesSync(mirror, new Date('2000-01-01'), new Date('2000-01-01'))
  const modifiedAt = fs.statSync(mirror).mtimeMs
  synchronize(root, true)
  assert.equal(fs.statSync(mirror).mtimeMs, modifiedAt, 'No-op generation must not trigger file watchers')
})

test('translation changes keep IDs while new keys append; prose changes need no generation', t => {
  const { root, read, write } = fixture(t)
  const mapping = `${versionRoot}/mappings/messages.json`
  const first = JSON.parse(read(mapping)).entries[0]
  write(`${versionRoot}/i18n/messages.json`, { en: { earlier: 'New', hello: 'Updated' }, ko: { hello: '수정' } })
  synchronize(root, true)
  const rows = JSON.parse(read(mapping)).entries
  assert.deepEqual(rows[0], first)
  assert.equal(rows[1].sourceKey, 'earlier')
  write(`${versionRoot}/game-rule/currency.md`, '# Changed\n')
  const before = read(`${versionRoot}/manifest.json`)
  synchronize(root)
  assert.deepEqual(read(`${versionRoot}/manifest.json`), before)
})

test('missing published mappings fail before mirror writes; new files need no manifest editing', t => {
  const { root, read, write, resources } = fixture(t)
  const mapping = `${versionRoot}/mappings/bases.json`
  const saved = read(mapping)
  fs.unlinkSync(path.join(root, mapping))
  write(`${versionRoot}/${resources[0].path}`, { alpha: { key: 'alpha' } })
  const original = read(resources[0].runtimePath)
  assert.throws(() => synchronize(root, true), /Published mapping is missing/)
  assert.deepEqual(read(resources[0].runtimePath), original)
  write(mapping, saved.toString())
  write(`${versionRoot}/game-data/catalog/unlisted.json`, {})
  synchronize(root, true)
  assert.equal(read('backend/src/main/resources/catalog/unlisted.json').toString(), '{}')
  synchronize(root)
})

test('destinations come from four directory conventions, never editable manifest paths', t => {
  const { root, read, write } = fixture(t)
  assert.equal(runtimePathFor('game-data/display/base-picker/images.json'), 'frontend/src/features/crafting/base-picker/images.json')
  assert.throws(() => runtimePathFor('game-data/catalog/../../outside.json'), /Invalid data path/)
  assert.throws(() => runtimePathFor('game-data/unknown/new.json'), /Unknown data directory/)
  const manifest = JSON.parse(read(`${versionRoot}/manifest.json`))
  manifest.resources[0].runtimePath = 'backend/src/main/resources/catalog/../../../../../../outside.json'
  write(`${versionRoot}/manifest.json`, manifest)
  assert.throws(() => synchronize(root), /Data drift/)
  synchronize(root, true)
  assert.equal(fs.existsSync(path.join(root, 'outside.json')), false)
  assert.equal(JSON.parse(read(`${versionRoot}/manifest.json`)).resources[0].runtimePath, 'backend/src/main/resources/catalog/top-bases.json')
})

test('removing a source never silently leaves a live runtime copy', t => {
  const { root, resources } = fixture(t)
  const source = path.join(root, versionRoot, resources[0].path)
  fs.unlinkSync(source)
  assert.throws(() => synchronize(root, true), /review and archive it explicitly/)
  assert(fs.existsSync(path.join(root, resources[0].runtimePath)))
  // Explicitly archive/remove the old runtime copy before regenerating the inventory.
  fs.unlinkSync(path.join(root, resources[0].runtimePath))
  synchronize(root, true)
  synchronize(root)
  const rows = JSON.parse(fs.readFileSync(path.join(root, versionRoot, 'mappings/bases.json'))).entries
  assert.equal(rows[0].sourceKey, 'zulu')
  assert.equal(rows[0].retired, true)
})

test('one CLI command updates seals, stable IDs, both mirrors and public policy from source edits', t => {
  const repository = fileURLToPath(new URL('../..', import.meta.url))
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'data-workflow-'))
  t.after(() => fs.rmSync(root, { recursive: true, force: true }))
  for (const file of [
    versionRoot, 'README.md', 'backend/src/main/resources/contracts',
    'docs/contracts/workbench-definitions-v1.schema.json', 'docs/contracts/goal-definitions-v1.schema.json',
    'scripts/versioned-game-data.mjs', 'scripts/update-ruleset-manifest.mjs',
    'scripts/sync-workbench-data.mjs', 'scripts/sync-public-contracts.mjs', 'scripts/public-contracts.mjs',
  ]) {
    fs.mkdirSync(path.dirname(path.join(root, file)), { recursive: true })
    fs.cpSync(path.join(repository, file), path.join(root, file), { recursive: true })
  }
  for (const folder of ['frontend/src/shared/contracts', 'infra/docker']) fs.mkdirSync(path.join(root, folder), { recursive: true })
  const load = file => JSON.parse(fs.readFileSync(path.join(root, file)))
  const save = (file, value) => fs.writeFileSync(path.join(root, file), JSON.stringify(value))
  const basePath = `${versionRoot}/game-data/catalog/top-bases.json`
  const bases = load(basePath)
  bases.soldier.name = 'Workflow test base'
  save(basePath, bases)
  save(`${versionRoot}/game-data/catalog/workflow-test.json`, { modifiers: [{ id: 'workflow:test', affixType: 'PREFIX', stats: [] }] })
  const policyPath = 'backend/src/main/resources/contracts/chaos-repeat.json'
  save(policyPath, { ...load(policyPath), defaultUses: 700 })
  const oldIds = load(`${versionRoot}/mappings/modifiers.json`).entries
  const run = flag => spawnSync(process.execPath, [path.join(root, 'scripts/versioned-game-data.mjs'), flag], {
    cwd: os.tmpdir(), encoding: 'utf8', env: { ...process.env, HEPHAISTOS_CONTRACT_ROOT: root },
  })
  assert.notEqual(run('--check').status, 0)
  const result = run('--write')
  assert.equal(result.status, 0, result.stderr)
  assert.equal(run('--check').status, 0)
  assert.equal(load('backend/src/main/resources/catalog/top-bases.json').soldier.name, 'Workflow test base')
  assert.equal(load('frontend/src/features/crafting/topBases.json').soldier.name, 'Workflow test base')
  assert(load('backend/src/main/resources/crafting/ruleset-v1.json').files['catalog/workflow-test.json'])
  const newIds = load(`${versionRoot}/mappings/modifiers.json`).entries
  assert.deepEqual(newIds.slice(0, oldIds.length), oldIds)
  assert.equal(newIds.at(-1).sourceKey, 'workflow:test')
  assert.match(fs.readFileSync(path.join(root, 'README.md'), 'utf8'), /기본 700/)
  assert.match(fs.readFileSync(path.join(root, 'frontend/src/shared/contracts/public.ts'), 'utf8'), /"defaultUses":700/)
  const manifest = fs.readFileSync(path.join(root, versionRoot, 'manifest.json'))
  assert.equal(run('--write').status, 0)
  assert.deepEqual(fs.readFileSync(path.join(root, versionRoot, 'manifest.json')), manifest)
})
