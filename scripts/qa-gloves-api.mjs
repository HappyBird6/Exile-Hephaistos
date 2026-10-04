import fs from 'node:fs'
import assert from 'node:assert/strict'
const origin = process.env.GLOVES_API ?? 'http://host.docker.internal:19380'
const root = '/qa'
const source = '/source'
const checks = [], initials = {}
const check = (name, value) => { assert(value, name); checks.push(name) }
const read = p => JSON.parse(fs.readFileSync(p, 'utf8').replace(/^\uFEFF/, ''))
const manifest = read(`${source}/frontend/src/features/crafting/topBases.json`)
const overrides = read(`${source}/backend/src/main/resources/catalog/top-base-essences.json`)
const get = async path => { const r = await fetch(origin + path); assert.equal(r.status, 200); return r.json() }
const post = async (path, data) => { const r = await fetch(origin + path, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data) }); assert.equal(r.status, 200); return r.json() }
const concrete = i => { const state = { ...i.state, explicits: [], augmentSockets: null, catalystQuality: null }; delete state.modifierIds; return state }
const apply = (state, action, activeOmens = []) => post('/api/v1/crafting/workbench/apply', { state, action, activeOmens })
const instance = d => ({ modifierId: d.id, values: Object.fromEntries(d.stats.map(s => [s.id, s.min])), fractured: false })
const canonical = d => ({ ...d, tags: [...d.tags].sort(), familyIds: [...d.familyIds].sort() })
const keys = ['solar', 'stocky', 'bow', 'wand', 'body', 'sceptre', 'belt', 'helmet', 'ring', 'ruby', 'emerald', 'sapphire', 'diamond', 'time-lost-ruby', 'time-lost-emerald', 'time-lost-sapphire', 'time-lost-diamond', ...Object.keys(manifest)]
for (const key of keys) {
  initials[key] = await get(`/api/v1/crafting/${key === 'solar' ? '' : 'workbench/'}initial?base=${key}&itemLevel=82`)
  check(`${key}: initial retained`, initials[key].state.itemLevel === 82)
}
for (const key of ['massive', 'sirenscale', 'adherent']) {
  const initial = initials[key], base = manifest[key], defs = Object.values(initial.modifiers)
  const catalog = read(`${source}/backend/src/main/resources/catalog/${base.pool}/catalog.json`)
  assert.deepEqual(defs.map(canonical).sort((a, b) => a.id.localeCompare(b.id)), catalog.modifiers.map(canonical).sort((a, b) => a.id.localeCompare(b.id)))
  check(`${key}: every modifier definition preserved`, true)
  check(`${key}: identity and empty implicit`, initial.state.baseItemId === base.id && initial.state.implicits.length === 0)
  check(`${key}: quality cap and unmodeled sockets`, initial.qualityLimit.maximumQuality === 20 && initial.augmentSockets === null)
  check(`${key}: explicit uniform policy`, initial.metadata.weightPolicy === 'UNVERIFIED_GAME_WEIGHTS_EXPLICIT_UNIFORM_ELIGIBLE_CANDIDATES' && defs.filter(d => d.weight).every(d => d.weight === 1))
  const start = concrete(initial); let state = start
  for (const action of ['TRANSMUTATION', 'AUGMENTATION', 'REGAL', 'EXALTED', 'ANNULMENT', 'CHAOS']) {
    const result = await apply(state, action)
    check(`${key}: ${action} applied`, result.applied && result.state.baseItemId === base.id)
    for (const event of result.events.filter(e => e.kind === 'ADD')) {
      const assumption = result.assumptions.find(a => a.id === 'gloves-uniform-candidates-v1' && a.candidates.includes(event.modifierId))
      check(`${key}: ${action} exact 1/N selection`, !!assumption && Math.abs(event.selectionProbability - 1 / assumption.n) < 1e-12)
    }
    state = result.state
  }
  const alchemy = await apply(start, 'ALCHEMY')
  check(`${key}: Alchemy four affixes`, alchemy.applied && alchemy.state.explicits.length === 4)
  const fractured = await apply(alchemy.state, 'FRACTURING')
  check(`${key}: Fracturing applied`, fractured.applied && fractured.state.explicits.filter(m => m.fractured).length === 1)
  const locked = fractured.state.explicits.find(m => m.fractured), chaos = await apply(fractured.state, 'CHAOS')
  assert.deepEqual(chaos.state.explicits.find(m => m.modifierId === locked.modifierId), locked)
  check(`${key}: locked roll survives Chaos`, chaos.applied)
  const suffix = defs.find(d => d.weight && d.familyIds.includes('FireResistance') && d.requiredItemLevel === 1)
  const life = defs.find(d => d.weight && d.familyIds.includes('IncreasedLife') && d.requiredItemLevel === 1)
  const magic = { ...start, rarity: 'MAGIC', explicits: [instance(suffix)] }
  for (const action of Object.keys(overrides[key].fixed)) {
    const ids = overrides[key].fixed[action]
    const targetFamilies = new Set(ids.flatMap(id => initial.modifiers[id].familyIds))
    const companion = defs.find(d => d.weight && d.requiredItemLevel === 1 && d.familyIds.every(f => !targetFamilies.has(f)))
    assert(companion)
    const result = await apply({ ...start, rarity: 'MAGIC', explicits: [instance(companion)] }, action)
    check(`${key}: ${action} complete guaranteed set`, result.applied && ids.includes(result.events.at(-1).modifierId) && Math.abs(result.events.at(-1).selectionProbability - 1 / ids.length) < 1e-12)
  }
  const low = await apply({ ...magic, itemLevel: 15 }, 'LESSER_ESSENCE_ENHANCEMENT')
  check(`${key}: source-level boundary refusal`, !low.applied && low.events.length === 0)
  const lowStart = concrete(await get(`/api/v1/crafting/workbench/initial?base=${key}&itemLevel=1`))
  const lowRoll = await apply(lowStart, 'TRANSMUTATION')
  check(`${key}: low ilvl uses only level 1 rows`, lowRoll.applied && lowRoll.state.explicits.every(m => initial.modifiers[m.modifierId].requiredItemLevel === 1))
  const rare = { ...start, rarity: 'RARE', explicits: [instance(life), instance(suffix)] }
  for (const action of ['PERFECT_ESSENCE_GROUNDING', 'PERFECT_ESSENCE_OPULENCE', 'ESSENCE_HYSTERIA', 'ESSENCE_ABYSS']) {
    const result = await apply(rare, action, ['Omen_of_Sinistral_Crystallisation'])
    check(`${key}: ${action} source restricted positive`, result.applied && result.consumedOmens.includes('Omen_of_Sinistral_Crystallisation'))
    const refused = await apply(rare, action, ['Omen_of_Sinistral_Crystallisation', 'Omen_of_Dextral_Crystallisation'])
    assert.deepEqual(refused.state, rare)
    check(`${key}: ${action} omen conflict atomic`, !refused.applied && refused.events.length === 0 && refused.consumedOmens.length === 0)
  }
  for (const action of ['PERFECT_ESSENCE_ICE', 'PERFECT_ESSENCE_BODY', 'ARTIFICER', 'ESSENCE_HORROR', 'RUNIC_ALLOY', 'CATALYST_FLESH']) {
    const refused = await apply(rare, action)
    assert.deepEqual(refused.state, rare)
    check(`${key}: ${action} class refusal`, !refused.applied && refused.events.length === 0)
  }
  const q = await post('/api/v1/crafting/workbench/quality-display', { state: rare })
  check(`${key}: quality endpoint`, q.qualityLimit.maximumQuality === 20 && q.state.baseItemId === base.id)
}
const oldRegistry = read(`${root}/baseline-registry.json`), current = read(`${source}/backend/src/main/resources/crafting/registry-v2.json`)
assert.deepEqual(await get('/api/v1/crafting/workbench/registry'), current)
check('runtime registry equals final source', true)
for (let i = 0; i < oldRegistry.entries.length; i++) {
  const next = structuredClone(current.entries[i])
  if (next.supportedBases) next.supportedBases = next.supportedBases.filter(b => !['massive', 'sirenscale', 'adherent'].includes(b))
  assert.deepEqual(next, oldRegistry.entries[i])
}
check('all 220 prior registry entries preserve fields/status/scope except added base support', true)
assert.deepEqual(current.serviceScope, oldRegistry.serviceScope)
check('deferred 50 and Support/Explorer scope preserved', current.serviceScope.deferred === 50)
const oldDisplay = read(`${root}/baseline-modifierTemplates.json`), display = read(`${source}/frontend/src/shared/i18n/modifierTemplates.json`)
for (const [id, binding] of Object.entries(oldDisplay.definitions)) assert.deepEqual(display.definitions[id], binding)
for (const locale of Object.keys(oldDisplay.templates)) for (const [id, template] of Object.entries(oldDisplay.templates[locale])) assert.deepEqual(display.templates[locale][id], template)
check('all historical modifier bindings and six-language templates preserved', true)
const oldTerms = read(`${root}/baseline-gameTerms.json`), terms = read(`${source}/frontend/src/shared/i18n/gameTerms.json`)
for (const locale of Object.keys(oldTerms)) for (const [id, term] of Object.entries(oldTerms[locale])) assert.deepEqual(terms[locale][id], term)
check('all historical six-language game terms preserved', true)
const oldBases = read(`${root}/baseline-topBases.json`)
for (const [key, base] of Object.entries(oldBases)) assert.deepEqual(manifest[key], base)
check('all previously registered top-base source facts preserved', true)
fs.writeFileSync(`${root}/api-initials.json`, JSON.stringify(initials, null, 2) + '\n')
fs.writeFileSync(`${root}/api-results.json`, JSON.stringify({ passed: true, count: checks.length, checks, initialBases: keys.length }, null, 2) + '\n')
console.log(checks.length, 'API assertions passed')
