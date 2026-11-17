import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { catalogResolver, catalogOutputs, catalogRuntimePath } from '../catalog-data.mjs'

function fixture(t) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'catalog-source-'))
  t.after(() => fs.rmSync(root, { recursive: true, force: true }))
  const write = (file, data) => {
    fs.mkdirSync(path.dirname(path.join(root, file)), { recursive: true })
    fs.writeFileSync(path.join(root, file), JSON.stringify(data))
  }
  const registry = { definitions: {
    life: { id: 'life', layer: 'EXPLICIT', affixType: 'PREFIX', weight: 100, tier: 2, stats: [{ id: 'life', min: 1, max: 5 }] },
    spirit: { id: 'spirit', layer: 'IMPLICIT', affixType: 'NONE', weight: 0, tier: 0, stats: [] },
  }, variants: { 'life@1': { modifierId: 'life', weight: 50, tier: 1 } } }
  const source = { metadata: { snapshotId: 'historical' }, base: { id: 'solar' }, modifierPool: 'default', implicitModifiers: [{ index: 1, ref: 'spirit' }] }
  write('modifiers.json', registry)
  write('amulets/modifier-pools.json', { default: ['life'], earlier: ['life@1'] })
  write('amulets/solar/catalog.json', source)
  write('amulets/stellar/catalog.json', { ...source, base: { id: 'stellar' } })
  return { root, write, registry, source }
}

test('one shared edit updates every referring base, totals and exact implicit placement', t => {
  const { root, write, registry } = fixture(t)
  registry.definitions.life.weight = 300
  registry.definitions.life.stats[0].max = 9
  write('modifiers.json', registry)
  const output = catalogOutputs(root).filter(row => row.runtimePath?.endsWith('/catalog.json'))
  assert.equal(output.length, 2)
  for (const row of output) {
    const document = JSON.parse(row.runtimeBytes)
    assert.deepEqual(document.modifiers.map(m => m.id), ['life', 'spirit'])
    assert.equal(document.modifiers[0].stats[0].max, 9)
    assert.equal(document.metadata.prefixWeight, 300)
    assert.equal(document.metadata.prefixCount, 1)
    assert.equal(document.metadata.snapshotId, 'historical')
  }
  assert.equal(catalogRuntimePath('amulets/modifier-pools.json'), null)
  assert.equal(catalogRuntimePath('modifiers.json'), null)
  assert.throws(() => catalogRuntimePath('solar/base.raw.json'), /Raw evidence/)
})

test('context differences override only reviewed fields and preserve shared stat changes', t => {
  const { root, registry, write, source } = fixture(t)
  registry.definitions.life.stats[0].max = 12
  write('modifiers.json', registry)
  const result = catalogResolver(root)('amulets/solar/catalog.json', { ...source, modifierPool: 'earlier' })
  assert.equal(result.modifiers[0].weight, 50)
  assert.equal(result.modifiers[0].tier, 1)
  assert.equal(result.modifiers[0].stats[0].max, 12)
  assert.equal(result.metadata.prefixWeight, 50)
  registry.variants['life@1'].id = 'silently-renamed'
  write('modifiers.json', registry)
  assert.throws(() => catalogResolver(root), /Unsupported variant field/)
})

test('broken references, duplicate identities and invalid implicit positions fail generation', t => {
  const { root, source, write } = fixture(t)
  const expand = catalogResolver(root)
  const name = 'amulets/solar/catalog.json'
  assert.throws(() => expand(name, { ...source, modifierPool: 'missing' }), /Unknown modifier pool/)
  assert.throws(() => expand(name, { ...source, implicitModifiers: [{ index: 3, ref: 'spirit' }] }), /Invalid implicit position/)
  assert.throws(() => expand(name, { ...source, implicitModifiers: [{ index: 0, ref: 'life' }] }), /EXPLICIT/)
  assert.throws(() => expand(name, { modifierRefs: ['missing'] }), /Unknown modifier reference/)
  assert.throws(() => expand(name, { modifierRefs: ['life', 'life@1'] }), /Duplicate modifier identity/)
  write('amulets/modifier-pools.json', { default: ['spirit'] })
  assert.throws(() => catalogResolver(root)(name, source), /Implicit definition in shared pool/)
})
