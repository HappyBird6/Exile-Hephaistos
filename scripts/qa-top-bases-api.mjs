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
// ModifierInstance's primitive boolean defaults to false when omitted. Make that default explicit;
// keep full structural equality for every state, stat value, flag and side effect below.
const instances = (defs) => defs.map((d) => ({ modifierId: d.id, values: Object.fromEntries(d.stats.map((s) => [s.id, s.min])), fractured: false }))
for (const key of ['soldier', 'imperial']) {
  const initial = initials[key], base = bases[key], defs = Object.values(initial.modifiers)
  check(`${key}: source identity`, initial.state.baseItemId === base.id)
  check(`${key}: independent snapshot`, initial.state.snapshotId !== initials[base.family].state.snapshotId && initial.compatibleSnapshotIds.length === 0)
  const pool = key === 'soldier' ? 'rusted-cuirass' : 'rusted-greathelm'
  const ordinary = JSON.parse(fs.readFileSync(`backend/src/main/resources/catalog/${pool}/catalog.json`)).modifiers
  const special = JSON.parse(fs.readFileSync(`backend/src/main/resources/catalog/${pool}/perfect-essences.catalog.json`)).modifiers
  check(`${key}: correct complete definitions`, defs.length === (key === 'soldier' ? 147 : 138) && ordinary.length === (key === 'soldier' ? 144 : 137) && special.length === (key === 'soldier' ? 3 : 1))
  // ModifierDefinition tags/familyIds are Sets, unlike ordered stat lists and state history.
  // Compare their complete members in a stable order; retain every other field exactly.
  const definition = (d) => ({ ...d, tags: [...d.tags].sort(), familyIds: [...d.familyIds].sort() })
  assert.deepEqual(defs.map(definition).sort((a, b) => a.id.localeCompare(b.id)), [...ordinary, ...special].map(definition).sort((a, b) => a.id.localeCompare(b.id)))
  checks.push(`${key}: every modifier identity, weight, range and property equals reviewed source`)
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
  const omittedDefault = structuredClone(rare)
  omittedDefault.explicits.forEach((m) => { delete m.fractured })
  const omittedResult = await apply(omittedDefault, 'ARTIFICER')
  const explicitResult = await apply(rare, 'ARTIFICER')
  check(`${key}: omitted fractured and explicit false are schema-equivalent`, same(omittedResult, explicitResult) && !explicitResult.applied && same(explicitResult.state, rare) && explicitResult.events.length === 0 && explicitResult.consumedOmens.length === 0)
  const targets = key === 'soldier' ? ['PERFECT_ESSENCE_BODY', 'PERFECT_ESSENCE_RUIN', 'PERFECT_ESSENCE_SEEKING'] : ['PERFECT_ESSENCE_THAWING']
  for (const action of targets) {
    const result = await apply(rare, action, ['Omen_of_Sinistral_Crystallisation'])
    check(`${key}: ${action} exact target`, result.applied && result.events.at(-1).selectionProbability === 1 && result.events.at(-1).modifierId.includes(':essence-'))
    check(`${key}: ${action} matching omen`, result.consumedOmens.includes('Omen_of_Sinistral_Crystallisation') && initial.modifiers[result.events[0].modifierId].affixType === 'PREFIX')
    const conflict = await apply(rare, action, ['Omen_of_Sinistral_Crystallisation', 'Omen_of_Dextral_Crystallisation'])
    check(`${key}: ${action} atomic conflict`, !conflict.applied && same(conflict.state, rare))
  }
  for (const action of ['PERFECT_ESSENCE_ICE', 'ARTIFICER', 'RUNIC_ALLOY', 'CATALYST_FLESH']) {
    const result = await apply(rare, action)
    check(`${key}: ${action} class refusal`, !result.applied && same(result.state, rare))
  }
  // BLESSED is not a WorkbenchCurrency enum value. Preserve its rejection as a malformed-input
  // assertion rather than claiming it is a supported action with a class restriction.
  const invalidAction = await fetch(origin + '/api/v1/crafting/workbench/apply', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ state: rare, action: 'BLESSED', activeOmens: [] }) })
  const problem = await invalidAction.json()
  check(`${key}: nonexistent BLESSED action rejected with Problem Details`, invalidAction.status === 400 && problem.status === 400)
  const quality = await post('/api/v1/crafting/workbench/quality-display', { state, activeOmens: [] })
  check(`${key}: quality display endpoint`, quality.qualityLimit.maximumQuality === 20)
}
const registry = await get('/api/v1/crafting/workbench/registry')
const bundled = JSON.parse(fs.readFileSync('backend/src/main/resources/crafting/registry-v2.json'))
assert.deepEqual(registry, bundled); checks.push('runtime registry equals final source')
// Pin the pre-expansion registry even after this branch has been committed. Scope Git trust to
// this read-only command and owned worktree; do not change global configuration.
const old = JSON.parse(process.env.TOP_BASE_BASELINE_REGISTRY
  ? fs.readFileSync(process.env.TOP_BASE_BASELINE_REGISTRY, 'utf8')
  : execFileSync('git', ['-c', `safe.directory=${process.cwd().split(String.fromCharCode(92)).join('/')}`, 'show', '2aac292d7a393a4c1f9b97086edf62e068c2e3d8:backend/src/main/resources/crafting/registry-v2.json'], { encoding: 'utf8' }))
for (let i = 0; i < old.entries.length; i++) {
  const current = structuredClone(registry.entries[i]);
  if (current.supportedBases) current.supportedBases = current.supportedBases.filter((b) => !['soldier', 'imperial'].includes(b))
  assert.deepEqual(current, old.entries[i])
}
checks.push('all prior registry entries preserved except additive base support')
fs.writeFileSync(`${output}/api-initials.json`, JSON.stringify(initials, null, 2))
fs.writeFileSync(`${output}/api-results.json`, JSON.stringify({ checks, count: checks.length, bases: keys.length, passed: true }, null, 2) + '\n')
console.log(checks.length, 'API checks passed')
