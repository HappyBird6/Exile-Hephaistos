import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { allocate, domains, synchronize, versionRoot } from '../versioned-game-data.mjs'

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
})

test('translation changes keep IDs while new keys append; manifest catches rule drift', t => {
  const { root, read, write } = fixture(t)
  const mapping = `${versionRoot}/mappings/messages.json`
  const first = JSON.parse(read(mapping)).entries[0]
  write(`${versionRoot}/i18n/messages.json`, { en: { earlier: 'New', hello: 'Updated' }, ko: { hello: '수정' } })
  synchronize(root, true)
  const rows = JSON.parse(read(mapping)).entries
  assert.deepEqual(rows[0], first)
  assert.equal(rows[1].sourceKey, 'earlier')
  write(`${versionRoot}/game-rule/currency.md`, '# Changed\n')
  assert.throws(() => synchronize(root), /Data drift/)
})

test('missing published mappings, unlisted files and path traversal fail before mirror writes', t => {
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
  assert.throws(() => synchronize(root, true), /Every canonical JSON/)
  fs.unlinkSync(path.join(root, versionRoot, 'game-data/catalog/unlisted.json'))
  const manifest = JSON.parse(read(`${versionRoot}/manifest.json`))
  manifest.resources[0].runtimePath = 'backend/src/main/resources/catalog/../../../../../../outside.json'
  write(`${versionRoot}/manifest.json`, manifest)
  assert.throws(() => synchronize(root, true))
  assert.deepEqual(read(resources[0].runtimePath), original)
})
