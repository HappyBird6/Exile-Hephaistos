import fs from 'node:fs'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
const origin = process.env.TOP_BASE_API ?? 'http://127.0.0.1:19280'
const output = process.env.TOP_BASE_EVIDENCE ?? '../top-bases-qa-20261004'
const checks = [], initials = {}
const check = (name, value) => { assert(value, name); checks.push(name) }
const stable = (value) => Array.isArray(value) ? value.map(stable) : value && typeof value === 'object' ? Object.fromEntries(Object.entries(value).sort(([a], [b]) => a.localeCompare(b)).map(([k, v]) => [k, stable(v)])) : value
const same = (a, b) => JSON.stringify(stable(a)) === JSON.stringify(stable(b))
const get = async (path) => { const r = await fetch(origin + path); assert.equal(r.status, 200); return r.json() }
const post = async (path, body) => { const r = await fetch(origin + path, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }); assert.equal(r.status, 200); return r.json() }
const keys = ['solar', 'stocky', 'bow', 'wand', 'body', 'sceptre', 'belt', 'helmet', 'ring', 'ruby', 'emerald', 'sapphire', 'diamond', 'time-lost-ruby', 'time-lost-emerald', 'time-lost-sapphire', 'time-lost-diamond', 'soldier', 'imperial']
const bases = JSON.parse(fs.readFileSync('frontend/src/features/crafting/topBases.json'))
for (const key of keys) {
  initials[key] = await get(`/api/v1/crafting/${key === 'solar' ? '' : 'workbench/'}initial?base=${key}&itemLevel=82`)
  check(`${key}: initial`, initials[key].state.itemLevel === 82)
}
const concrete = (i) => { const state = { ...i.state, explicits: [], augmentSockets: null, catalystQuality: null }; delete state.modifierIds; return state }
const apply = (state, action, activeOmens = []) => post('/api/v1/crafting/workbench/apply', { state, action, activeOmens })
const instances = (defs) => defs.map((d) => ({ modifierId: d.id, values: Object.fromEntries(d.stats.map((s) => [s.id, s.min])) }))
for (const key of ['soldier', 'imperial']) {
  const initial = initials[key], base = bases[key], defs = Object.values(initial.modifiers)
  check(`${key}: source identity`, initial.state.baseItemId === base.id)
  check(`${key}: independent snapshot`, initial.state.snapshotId !== initials[base.family].state.snapshotId && initial.compatibleSnapshotIds.length === 0)
  check(`${key}: correct complete definitions`, defs.length === (key === 'soldier' ? 147 : 140))
  check(`${key}: source quality`, initial.qualityLimit.maximumQuality === 20)
  const root = concrete(initial)
  let state = root
  for (const action of ['TRANSMUTATION', 'AUGMENTATION', 'REGAL', 'EXALTED', 'ANNULMENT', 'CHAOS']) {
    const result = await apply(state, action)
    check(`${key}: ${action} applied`, result.applied && result.state.baseItemId === base.id && result.events.length > 0)
    check(`${key}: ${action} bounded probabilities`, result.events.every((e) => e.selectionProbability > 0 && e.selectionProbability <= 1))
    state = result.state
  }
  const alchemy = await apply(root, 'ALCHEMY')
  check(`${key}: ALCHEMY four affixes`, alchemy.applied && alchemy.state.explicits.length === 4)
  const fractured = await apply(alchemy.state, 'FRACTURING')
  check(`${key}: FRACTURING applied`, fractured.applied && fractured.state.explicits.filter((m) => m.fractured).length === 1)
  const locked = fractured.state.explicits.find((m) => m.fractured)
  const chaos = await apply(fractured.state, 'CHAOS')
  check(`${key}: fracture preserved`, chaos.applied && same(chaos.state.explicits.find((m) => m.modifierId === locked.modifierId), locked))
  const rare = { ...root, rarity: 'RARE', explicits: instances(['PREFIX', 'SUFFIX'].map((side) => defs.find((d) => d.weight > 0 && d.affixType === side))) }
  const targets = key === 'soldier' ? ['PERFECT_ESSENCE_BODY', 'PERFECT_ESSENCE_RUIN', 'PERFECT_ESSENCE_SEEKING'] : ['PERFECT_ESSENCE_THAWING']
  for (const action of targets) {
    const result = await apply(rare, action, ['Omen_of_Sinistral_Crystallisation'])
    check(`${key}: ${action} exact target`, result.applied && result.events.at(-1).selectionProbability === 1 && result.events.at(-1).modifierId.includes(':essence-'))
    check(`${key}: ${action} matching omen`, result.consumedOmens.includes('Omen_of_Sinistral_Crystallisation') && initial.modifiers[result.events[0].modifierId].affixType === 'PREFIX')
    const conflict = await apply(rare, action, ['Omen_of_Sinistral_Crystallisation', 'Omen_of_Dextral_Crystallisation'])
    check(`${key}: ${action} atomic conflict`, !conflict.applied && same(conflict.state, rare))
  }
  for (const action of ['PERFECT_ESSENCE_ICE', 'ARTIFICER', 'RUNIC_ALLOY', 'BLESSED', 'CATALYST_FLESH']) {
    const result = await apply(rare, action)
    check(`${key}: ${action} class refusal`, !result.applied && same(result.state, rare))
  }
  const quality = await post('/api/v1/crafting/workbench/quality-display', { state, activeOmens: [] })
  check(`${key}: quality display endpoint`, quality.qualityLimit.maximumQuality === 20)
}
const registry = await get('/api/v1/crafting/workbench/registry')
const bundled = JSON.parse(fs.readFileSync('backend/src/main/resources/crafting/registry-v2.json'))
assert.deepEqual(registry, bundled); checks.push('runtime registry equals final source')
const old = JSON.parse(execFileSync('git', ['show', 'HEAD:backend/src/main/resources/crafting/registry-v2.json'], { encoding: 'utf8' }))
for (let i = 0; i < old.entries.length; i++) {
  const current = structuredClone(registry.entries[i]);
  if (current.supportedBases) current.supportedBases = current.supportedBases.filter((b) => !['soldier', 'imperial'].includes(b))
  assert.deepEqual(current, old.entries[i])
}
checks.push('all prior registry entries preserved except additive base support')
fs.writeFileSync(`${output}/api-initials.json`, JSON.stringify(initials, null, 2))
fs.writeFileSync(`${output}/api-results.json`, JSON.stringify({ checks, count: checks.length, bases: keys.length, passed: true }, null, 2) + '\n')
console.log(checks.length, 'API checks passed')
