import fs from 'node:fs'
import assert from 'node:assert/strict'
const origin = 'http://host.docker.internal:20080'
const output = process.env.HEPHAISTOS_QA_OUTPUT ?? '/qa'
assert(!fs.existsSync(output + '/api-results.json') && !fs.existsSync(output + '/api-initials.json'), 'Preserve prior API evidence')
fs.mkdirSync(output, { recursive: true })
const checks = [], initials = {}
const check = (name, value) => { assert(value, name); checks.push(name) }
const get = async p => { const r = await fetch(origin + p); assert.equal(r.status, 200); return r.json() }
const post = async (p, body, status = 200) => { const r = await fetch(origin + p, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }); assert.equal(r.status, status); return r.json() }
const initial = (base, level = 82) => get(`/api/v1/crafting/${base === 'solar' ? '' : 'workbench/'}initial?base=${base}&itemLevel=${level}`)
const concrete = i => { const s = { ...i.state, explicits: [], augmentSockets: null, catalystQuality: null }; delete s.modifierIds; return s }
const apply = (state, action, activeOmens = []) => post('/api/v1/crafting/workbench/apply', { state, action, activeOmens })
const stable = x => JSON.parse(JSON.stringify(x, (_, v) => Array.isArray(v) && v.every(x => typeof x === 'string') ? [...v].sort() : v))
const baseline = JSON.parse(fs.readFileSync('/qa/baseline-api-initials.json'))
for (const k of [...Object.keys(baseline), 'hallowed']) {
  initials[k] = await initial(k)
  if (baseline[k]) assert.deepEqual(stable(initials[k]), stable(baseline[k]))
  check(k + ' initial and historical source identity', true)
}
const i = initials.hallowed, root = concrete(i)
check('exact hallowed identity', root.baseItemId.endsWith('/FourSceptre13'))
check('built-in skill stays outside modifiers', !Object.values(i.modifiers).some(d => /Skeletal Warrior/.test(d.text)) && root.implicits.length === 0)
check('complete Sceptre pool retained', Object.values(i.modifiers).filter(d => d.weight > 0).length === 150)
assert.deepEqual(i.modifiers, initials.sceptre.modifiers)
check('legacy class definitions identical', true)
const magic = (await apply(root, 'TRANSMUTATION')).state
check('verified Hallowed source quality cap20 in API response', (await apply(root, 'TRANSMUTATION')).qualityLimit?.maximumQuality === 20)
const rare = (await apply(root, 'ALCHEMY')).state
const instance = d => ({ modifierId: d.id, values: Object.fromEntries(d.stats.map(s => [s.id, s.min])), fractured: false })
const prefix = Object.values(i.modifiers).find(d => d.weight > 0 && d.affixType === 'PREFIX' && d.requiredItemLevel === 1)
const suffix = i.modifiers['rattling-sceptre:suffix:of-the-brute']
const pair = { ...root, rarity: 'RARE', explicits: [instance(prefix), instance(suffix)] }
for (const [action, s] of [['TRANSMUTATION', root], ['AUGMENTATION', { ...magic, explicits: magic.explicits.slice(0, 1) }], ['REGAL', magic], ['EXALTED', { ...rare, explicits: rare.explicits.slice(0, 3) }], ['CHAOS', rare], ['ANNULMENT', rare], ['DIVINE', rare], ['FRACTURING', rare], ['ALCHEMY', root]]) {
  const r = await apply(s, action)
  check('positive ' + action, r.applied)
}
for (const tier of ['GREATER', 'PERFECT']) for (const [action, state] of [['TRANSMUTATION', root], ['AUGMENTATION', { ...root, rarity: 'MAGIC', explicits: [instance(suffix)] }], ['REGAL', magic], ['EXALTED', pair], ['CHAOS', pair]]) {
  check('positive ' + tier + '_' + action, (await apply(state, tier + '_' + action)).applied)
}
for (const a of ['LESSER_ESSENCE_COMMAND', 'ESSENCE_COMMAND', 'GREATER_ESSENCE_COMMAND']) {
  const r = await apply({ ...root, rarity: 'MAGIC' }, a)
  check('positive ' + a, r.applied && r.state.explicits.some(m => ['rattling-sceptre:prefix:agitative', 'rattling-sceptre:prefix:provocative', 'rattling-sceptre:prefix:motivating'].includes(m.modifierId)))
}
const rareSmall = { ...rare, explicits: rare.explicits.slice(0, 1) }
check('positive Perfect Command', (await apply(rareSmall, 'PERFECT_ESSENCE_COMMAND')).applied)
for (const [action, s] of [['PERFECT_ESSENCE_SORCERY', rare], ['PERFECT_ESSENCE_BODY', rare], ['CATALYST_FLESH', rare], ['ARTIFICER', root], ['RUNIC_ALLOY', rare]]) {
  const r = await apply(s, action)
  assert.deepEqual(r.state, s)
  check('class/deferred atomic rejection ' + action, !r.applied)
}
for (const omen of ['Omen_of_Dextral_Coronation', 'Omen_of_Sinistral_Coronation']) {
  const r = await apply(magic, 'REGAL', [omen])
  check('positive ' + omen, r.applied && r.consumedOmens.includes(omen))
}
for (const [omen, action, state] of [
  ['Omen_of_Sinistral_Alchemy', 'ALCHEMY', root], ['Omen_of_Dextral_Alchemy', 'ALCHEMY', root],
  ['Omen_of_Sinistral_Exaltation', 'EXALTED', pair], ['Omen_of_Dextral_Exaltation', 'EXALTED', pair], ['Omen_of_Greater_Exaltation', 'EXALTED', pair],
  ['Omen_of_Sinistral_Annulment', 'ANNULMENT', pair], ['Omen_of_Dextral_Annulment', 'ANNULMENT', pair], ['Omen_of_Greater_Annulment', 'ANNULMENT', pair],
  ['Omen_of_Sinistral_Erasure', 'CHAOS', pair], ['Omen_of_Dextral_Erasure', 'CHAOS', pair], ['Omen_of_Whittling', 'CHAOS', pair],
  ['Omen_of_Sinistral_Crystallisation', 'PERFECT_ESSENCE_COMMAND', pair], ['Omen_of_Dextral_Crystallisation', 'PERFECT_ESSENCE_COMMAND', pair],
]) {
  const result = await apply(state, action, [omen])
  check('positive ' + omen, result.applied && result.consumedOmens.includes(omen))
}
const blessed = await apply(pair, 'DIVINE', ['Omen_of_the_Blessed'])
assert.deepEqual(blessed.state, pair)
check('Blessed refuses absent numeric implicit', !blessed.applied && blessed.consumedOmens.length === 0)
const low = concrete(await initial('hallowed', 1))
check('item level distinct from character requirement', low.itemLevel === 1)
for (const [action, itemLevel] of [['GREATER_TRANSMUTATION', 34], ['PERFECT_TRANSMUTATION', 49]]) {
  const state = { ...root, itemLevel }
  const result = await apply(state, action)
  check(action + ' minimum level atomically enforced', !result.applied)
  assert.deepEqual(result.state, state)
}
const rejected = await apply({ ...rareSmall, itemLevel: 71 }, 'PERFECT_ESSENCE_COMMAND')
check('Perfect Command source level72 minimum', !rejected.applied)
const bucket = { ...i.state, modifierIds: [] }
await post('/api/v1/crafting/support/assess', { state: bucket, goal: { required: [{ family: 'IncreasedLife', minimumTier: 1 }], candidates: [], candidateCount: 0 } }, 422)
await post('/api/v1/crafting/explore', { state: bucket, plan: ['TRANSMUTATION'], maxNodes: 10, maxEdges: 10, maxMillis: 100 }, 422)
check('Support and Explorer remain Solar only', true)
fs.writeFileSync(output + '/api-initials.json', JSON.stringify(initials, null, 2) + '\n')
fs.writeFileSync(output + '/api-results.json', JSON.stringify({ passed: true, count: checks.length, checks }, null, 2) + '\n')
console.log(checks.length, 'API checks passed')
