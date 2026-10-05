import fs from 'node:fs'
import assert from 'node:assert/strict'
const origin = 'http://host.docker.internal:20180'
const output = process.env.HEPHAISTOS_QA_OUTPUT ?? '/qa/api-attempt-1'
assert(!fs.existsSync(output), 'Preserve prior API evidence')
fs.mkdirSync(output)
const checks = [], initials = {}
const check = (name, value) => { assert(value, name); checks.push(name) }
const get = async p => { const r = await fetch(origin + p); assert.equal(r.status, 200); return r.json() }
const post = async (p, body, status = 200) => { const r = await fetch(origin + p, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }); assert.equal(r.status, status); return r.json() }
const initial = (base, level = 82) => get(`/api/v1/crafting/${base === 'solar' ? '' : 'workbench/'}initial?base=${base}&itemLevel=${level}`)
const concrete = i => { const s = { ...i.state, explicits: [], augmentSockets: null, catalystQuality: null }; delete s.modifierIds; return s }
const apply = (state, action, activeOmens = []) => post('/api/v1/crafting/workbench/apply', { state, action, activeOmens })
const stable = x => JSON.parse(JSON.stringify(x, (_, v) => Array.isArray(v) && v.every(x => typeof x === 'string') ? [...v].sort() : v))
const baseline = JSON.parse(fs.readFileSync('/qa/baseline-api-initials.json'))
for (const key of Object.keys(baseline)) {
  initials[key] = await initial(key)
  assert.deepEqual(stable(initials[key]), stable(baseline[key]))
  check(key + ' historical source identity', true)
}
const targets = JSON.parse(fs.readFileSync('/source/frontend/src/features/crafting/topBaseEssences.json'))
const bases = JSON.parse(fs.readFileSync('/source/frontend/src/features/crafting/topBases.json'))
for (const [key, base] of Object.entries(bases).filter(([,b]) => b.family === 'wands')) {
  const i = initials[key] = await initial(key), root = concrete(i)
  check(key + ' exact identity', root.baseItemId === base.id)
  check(key + ' no simulated built-in skill', root.implicits.length === 0 && !Object.values(i.modifiers).some(d => /Grants Skill:/.test(d.text)))
  const expected = ['bone','offering','primordial'].includes(key) ? 118 : ['volatile','galvanic'].includes(key) ? 123 : 185
  check(key + ' complete eligible pool', Object.values(i.modifiers).filter(d => d.weight > 0).length === expected)
  for (const d of Object.values(i.modifiers)) assert.deepEqual(d, initials.wand.modifiers[d.id])
  const magic = (await apply(root, 'TRANSMUTATION')).state
  const rare = (await apply(root, 'ALCHEMY')).state
  const instance = d => ({ modifierId: d.id, values: Object.fromEntries(d.stats.map(s => [s.id,s.min])), fractured: false })
  const prefix = Object.values(i.modifiers).find(d => d.weight > 0 && d.affixType === 'PREFIX' && d.requiredItemLevel === 1)
  const suffix = Object.values(i.modifiers).find(d => d.weight > 0 && d.affixType === 'SUFFIX' && d.requiredItemLevel === 1)
  const pair = { ...root, rarity: 'RARE', explicits: [instance(prefix),instance(suffix)] }
  for (const [action,state] of [['TRANSMUTATION',root],['AUGMENTATION',{...magic,explicits:magic.explicits.slice(0,1)}],['REGAL',magic],['EXALTED',pair],['CHAOS',pair],['ANNULMENT',pair],['DIVINE',rare],['ALCHEMY',root]]) check(key + ' positive ' + action,(await apply(state,action)).applied)
  check(key + ' quality cap20',(await apply(root,'TRANSMUTATION')).qualityLimit.maximumQuality === 20)
  for (const [action,ids] of Object.entries(targets[key].fixed)) {
    const r = await apply({...root,rarity:'MAGIC'},action)
    check(key + ' positive ' + action,r.applied && r.state.explicits.some(m => ids.includes(m.modifierId)))
    const level = i.modifiers[ids[0]].requiredItemLevel
    const low = {...root,rarity:'MAGIC',itemLevel:level-1}
    const reject = await apply(low,action)
    check(key + ' source minimum ' + action,!reject.applied)
    assert.deepEqual(reject.state,low)
  }
  for (const [action,ids] of Object.entries(targets[key].replacements)) {
    const r = await apply(pair,action)
    check(key + ' positive ' + action,r.applied && r.state.explicits.some(m => ids.includes(m.modifierId)))
    const low = {...pair,itemLevel:71}, rejected = await apply(low,action)
    check(key+' Perfect source level72 '+action,!rejected.applied)
    assert.deepEqual(rejected.state,low)
  }
  for (const [omen,action,state] of [['Omen_of_Dextral_Coronation','REGAL',magic],['Omen_of_Sinistral_Coronation','REGAL',magic],['Omen_of_Sinistral_Alchemy','ALCHEMY',root],['Omen_of_Dextral_Alchemy','ALCHEMY',root],['Omen_of_Sinistral_Exaltation','EXALTED',pair],['Omen_of_Dextral_Exaltation','EXALTED',pair],['Omen_of_Sinistral_Annulment','ANNULMENT',pair],['Omen_of_Dextral_Annulment','ANNULMENT',pair],['Omen_of_Whittling','CHAOS',pair],['Omen_of_Dextral_Crystallisation','PERFECT_ESSENCE_SORCERY',pair]]) {
    const r = await apply(state,action,[omen])
    check(key + ' positive ' + omen,r.applied && r.consumedOmens.includes(omen))
  }
  for (const action of ['ESSENCE_COMMAND','PERFECT_ESSENCE_BODY','ARTIFICER','CATALYST_FLESH','RUNIC_ALLOY']) {
    const r = await apply(pair,action)
    check(key + ' atomic wrongclass/deferred ' + action,!r.applied)
    assert.deepEqual(r.state,pair)
  }
  check(key + ' low ilvl independent of requirement',concrete(await initial(key,1)).itemLevel === 1)
  for (const [action,itemLevel] of [['GREATER_TRANSMUTATION',34],['PERFECT_TRANSMUTATION',49]]) {
    const s = {...root,itemLevel}, r = await apply(s,action)
    check(key + ' higher currency minimum ' + action,!r.applied)
    assert.deepEqual(r.state,s)
  }
  const bucket = {...i.state,modifierIds:[]}
  await post('/api/v1/crafting/support/assess',{state:bucket,goal:{required:[{family:'IncreasedLife',minimumTier:1}],candidates:[],candidateCount:0}},422)
  await post('/api/v1/crafting/explore',{state:bucket,plan:['TRANSMUTATION'],maxNodes:10,maxEdges:10,maxMillis:100},422)
  check(key+' Support/Explorer stay Solar-only',true)
}
fs.writeFileSync(`${output}/api-initials.json`, JSON.stringify(initials,null,2)+'\n')
fs.writeFileSync(`${output}/api-results.json`, JSON.stringify({passed:true,count:checks.length,checks},null,2)+'\n')
console.log(checks.length,'API checks passed')
