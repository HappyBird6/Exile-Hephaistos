import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import { gameDataOutputs } from '../game-data-outputs.mjs'
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

const repository = fileURLToPath(new URL('../..', import.meta.url))
function fixture(t) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'versioned-data-'))
  t.after(() => fs.rmSync(root, { recursive: true, force: true }))
  fs.cpSync(path.join(repository, versionRoot), path.join(root, versionRoot), { recursive: true })
  const read = name => fs.readFileSync(path.join(root, name))
  const load = name => JSON.parse(read(name))
  const write = (name, value) => {
    const file = path.join(root, name)
    fs.mkdirSync(path.dirname(file), { recursive: true })
    fs.writeFileSync(file, Buffer.isBuffer(value) || typeof value === 'string' ? value : JSON.stringify(value))
  }
  synchronize(root, true)
  return { root, read, load, write }
}

test('read-only checks reject drift and no-op generation leaves file watchers alone', t => {
  const { root, read, write } = fixture(t)
  const mirror = 'backend/src/main/resources/catalog/top-bases.json'
  synchronize(root)
  const readonly = 'crafting/goalfilter/definitions-v5-readonly.json'
  assert.deepEqual(read(`backend/src/main/resources/${readonly}`), read(`${versionRoot}/game-data/${readonly}`))
  const original = read(`${versionRoot}/manifest.json`)
  write(mirror, '{}')
  assert.throws(() => synchronize(root), /Data drift/)
  assert.equal(read(mirror).toString(), '{}')
  assert.deepEqual(read(`${versionRoot}/manifest.json`), original)
  synchronize(root, true)
  fs.utimesSync(path.join(root, mirror), new Date('2000-01-01'), new Date('2000-01-01'))
  synchronize(root, true)
  assert.equal(fs.statSync(path.join(root, mirror)).mtimeMs, new Date('2000-01-01').getTime())
})

test('translation changes preserve IDs and prose needs no generation', t => {
  const { root, load, read, write } = fixture(t)
  const mapping = `${versionRoot}/mappings/bases.json`
  const initial = read(mapping)
  const texts = `${versionRoot}/i18n/catalog/bases.json`
  const names = load(texts)
  const id = load('backend/src/main/resources/catalog/top-bases.json').soldier.id
  names[id].name = 'Changed display name'
  write(texts, names)
  synchronize(root, true)
  assert.equal(load('backend/src/main/resources/catalog/top-bases.json').soldier.name, 'Changed display name')
  assert.deepEqual(read(mapping), initial)
  write(`${versionRoot}/game-rule/currency.md`, '# Changed prose\n')
  synchronize(root)
})

test('missing published mappings fail before runtime writes', t => {
  const { root, read, write } = fixture(t)
  fs.unlinkSync(path.join(root, versionRoot, 'mappings/bases.json'))
  write(`${versionRoot}/game-data/crafting/new-resource.json`, {})
  const original = read('backend/src/main/resources/catalog/top-bases.json')
  assert.throws(() => synchronize(root, true), /Published mapping is missing/)
  assert.deepEqual(read('backend/src/main/resources/catalog/top-bases.json'), original)
  assert(!fs.existsSync(path.join(root, 'backend/src/main/resources/crafting/new-resource.json')))
})

test('generated paths cannot escape the repository and unknown source folders fail', t => {
  const { root, load, write } = fixture(t)
  const name = `${versionRoot}/manifest.json`
  const manifest = load(name)
  manifest.generated[0].runtimePath = 'backend/src/main/resources/../../../../outside.json'
  write(name, manifest)
  assert.throws(() => synchronize(root, true), /Invalid generated resource path/)
  assert(!fs.existsSync(path.join(root, 'outside.json')))
  write(`${versionRoot}/game-data/unknown/new.json`, {})
  assert.throws(() => synchronize(root, true), /Unknown game-data directory/)
})

test('removing a source requires explicit archival of its runtime output', t => {
  const { root, write } = fixture(t)
  const source = `${versionRoot}/game-data/crafting/workflow-test.json`
  const runtime = 'backend/src/main/resources/crafting/workflow-test.json'
  write(source, {})
  synchronize(root, true)
  fs.unlinkSync(path.join(root, source))
  assert.throws(() => synchronize(root, true), /review and archive it explicitly/)
  assert(fs.existsSync(path.join(root, runtime)))
  fs.unlinkSync(path.join(root, runtime))
  synchronize(root, true)
  synchronize(root)
})

test('one CLI command updates seals, stable IDs, both mirrors and public policy', t => {
  const { root, load, write, read } = fixture(t)
  for (const file of [
    'README.md', 'backend/src/main/resources/contracts',
    'docs/contracts/workbench-definitions-v1.schema.json', 'docs/contracts/goal-definitions-v1.schema.json',
    'scripts/versioned-game-data.mjs', 'scripts/id-transport.mjs', 'scripts/update-ruleset-manifest.mjs', 'scripts/catalog-data.mjs',
    'scripts/game-data-outputs.mjs', 'scripts/modifier-data.mjs', 'scripts/sync-workbench-data.mjs',
    'scripts/sync-public-contracts.mjs', 'scripts/public-contracts.mjs',
  ]) {
    fs.mkdirSync(path.dirname(path.join(root, file)), { recursive: true })
    fs.cpSync(path.join(repository, file), path.join(root, file), { recursive: true })
  }
  for (const folder of ['frontend/src/shared/contracts', 'infra/docker']) fs.mkdirSync(path.join(root, folder), { recursive: true })
  const namesFile = `${versionRoot}/i18n/catalog/bases.json`
  const names = load(namesFile)
  names[load('backend/src/main/resources/catalog/top-bases.json').soldier.id].name = 'Workflow test base'
  write(namesFile, names)
  write(`${versionRoot}/game-data/crafting/workflow-test.json`, { modifiers: [{ id: 'workflow:test', affixType: 'PREFIX', stats: [] }] })
  const policy = 'backend/src/main/resources/contracts/chaos-repeat.json'
  write(policy, { ...load(policy), defaultUses: 700 })
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
  assert(load('backend/src/main/resources/crafting/ruleset-v1.json').files['catalog/solar-amulet/catalog.json'])
  const newIds = load(`${versionRoot}/mappings/modifiers.json`).entries
  assert.deepEqual(newIds.slice(0, oldIds.length), oldIds)
  assert.equal(newIds.at(-1).sourceKey, 'workflow:test')
  const transport = load('backend/src/main/resources/contracts/game-ids-v1.json')
  assert.equal(transport.domains.modifiers[newIds.at(-1).id], 'workflow:test')
  assert.deepEqual(read('backend/src/main/resources/contracts/game-ids-v1.json'), read('frontend/src/shared/contracts/gameIds.json'))
  assert.match(read('README.md').toString(), /기본 700/)
  assert.match(read('frontend/src/shared/contracts/public.ts').toString(), /"defaultUses":700/)
  const manifest = read(`${versionRoot}/manifest.json`)
  assert.equal(run('--write').status, 0)
  assert.deepEqual(read(`${versionRoot}/manifest.json`), manifest)
})

test('shared reveal definitions drive both consumers and missing references fail', t => {
  const { root, load, write } = fixture(t)
  const source = `${versionRoot}/game-data/modifiers/reveal.json`
  const registry = load(source)
  const id = Object.keys(registry.desecrated.definitions)[0]
  registry.desecrated.definitions[id].stats[0].max = 42
  write(source, registry)
  const outputs = gameDataOutputs(path.join(root, versionRoot))
  assert.equal(outputs.get('backend/src/main/resources/catalog/manual-reveal.json').value.definitions.find(d => d.id === id).stats[0].max, 42)
  assert.equal(outputs.get('frontend/src/features/crafting/manualRevealDefinitions.json').value.definitions[id].stats[0].max, 42)
  delete registry.desecrated.definitions[id]
  write(source, registry)
  assert.throws(() => gameDataOutputs(path.join(root, versionRoot)), /Unknown modifier reference/)
})

test('one base essence patch drives the server and the browser verification row', t => {
  const { root, load, write } = fixture(t)
  const source = `${versionRoot}/game-data/crafting/essences/targets.json`
  const bindings = load(source)
  const base = 'Metadata/Items/Armours/Gloves/FourGlovesStr1'
  const fixed = bindings.groups[bindings.bases[base]].fixed
  const replacement = fixed.ESSENCE_BODY
  assert.notDeepEqual(fixed.LESSER_ESSENCE_BODY, replacement)
  fixed.LESSER_ESSENCE_BODY = replacement
  write(source, bindings)
  const outputs = gameDataOutputs(path.join(root, versionRoot))
  const server = outputs.get('backend/src/main/resources/crafting/workbench-definitions-v1.json').value
  const browser = outputs.get('frontend/src/features/crafting/materialVerification.json').value
  const targets = browser.rows[browser.byBase.stocky].targets
  assert.deepEqual(server.baseTargets[base].fixed.LESSER_ESSENCE_BODY, replacement)
  assert.deepEqual(targets.LESSER_ESSENCE_BODY.ids, replacement)
  assert.equal(targets.LESSER_ESSENCE_BODY.level, targets.ESSENCE_BODY.level)
  assert.notDeepEqual(browser.rows[browser.byBase.solar].targets.LESSER_ESSENCE_BODY.ids, replacement)
})

test('translated tooltip wording cannot change target eligibility; unknown probability stays explicit', t => {
  const { root, load, write } = fixture(t)
  const texts = `${versionRoot}/i18n/materials/en.json`
  const translated = load(texts)
  for (const item of Object.values(translated)) if (item.lines) item.lines = ['번역된 설명']
  const targets = load('frontend/src/features/crafting/materialTargetClasses.json')
  write(texts, translated)
  const outputs = gameDataOutputs(path.join(root, versionRoot))
  assert.deepEqual(outputs.get('frontend/src/features/crafting/materialTargetClasses.json').value, targets)
  const source = `${versionRoot}/game-data/crafting/reveal/options.json`
  const options = load(source)
  options.selectionPolicy.probability = 0
  write(source, options)
  assert.throws(() => gameDataOutputs(path.join(root, versionRoot)), /no reviewed probability/)
})

test('unhandled files fail instead of becoming unused second sources', t => {
  const { root, write } = fixture(t)
  write(`${versionRoot}/game-data/crafting/essences/unhandled.json`, {})
  assert.throws(() => gameDataOutputs(path.join(root, versionRoot)), /Source has no generation rule/)
})

test('every published essence base has the same guaranteed targets in the workbench material picker', () => {
  const load = file => JSON.parse(fs.readFileSync(path.join(repository, file), 'utf8'))
  const crafting = versionRoot + '/game-data/crafting/'
  const { entries } = load(crafting + 'registry-v2.json')
  const { sets } = load(crafting + 'supported-base-sets-v1.json')
  const { rows, byBase } = load('frontend/src/features/crafting/materialVerification.json')
  for (const entry of entries.filter(entry => entry.category === 'ESSENCE' && entry.action)) {
    const published = entry.supportedBases ?? sets[entry.supportedBaseSet]
    const applicable = Object.keys(byBase).filter(base => rows[byBase[base]].targets[entry.action])
    assert.deepEqual(new Set(published), new Set(applicable), entry.id)
  }
})
