import fs from 'node:fs'
import assert from 'node:assert/strict'
const root = '/qa/implicit-attempt-2'
assert(!fs.existsSync(root)); fs.mkdirSync(root)
const initials = JSON.parse(fs.readFileSync('/qa/api-attempt-3/api-initials.json'))
const checks = []
const check = (name,value) => { assert(value,name); checks.push(name) }
const apply = async (state,action,activeOmens=[]) => { const r=await fetch('http://app:8080/api/v1/crafting/workbench/apply',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({state,action,activeOmens})}); assert.equal(r.status,200); return r.json() }
for (const [key,i] of Object.entries(initials).filter(([k])=>k.endsWith('-belt'))) {
  const state={...i.state,explicits:[],augmentSockets:null,catalystQuality:null}; delete state.modifierIds
  const implicit=i.modifiers[state.implicits[0].modifierId]
  const variable=implicit.stats.some(s=>s.min!==s.max)
  const result=await apply(state,'DIVINE',['Omen_of_the_Blessed'])
  check(key+' Blessed applicability exactly follows variable implicit',result.applied===variable)
  if (!variable) assert.deepEqual(result.state,state)
  else check(key+' Blessed consumed',result.consumedOmens.includes('Omen_of_the_Blessed'))
  for (const s of implicit.stats) check(key+' source bound '+s.id,result.state.implicits[0].values[s.id]>=s.min && result.state.implicits[0].values[s.id]<=s.max)
  if (['invoking-belt','sinew-belt','forking-belt'].includes(key)) check(key+' fixed slots remain1',result.state.implicits[0].values.local_charm_slots===1)
  check(key+' slot property never invented as rolled state',Object.keys(result.state).every(k=>!['charmSlots','charmSlotRoll'].includes(k)))
  check(key+' quality cap remains unsupported',result.qualityLimit==null)
  const alchemy=await apply(state,'ALCHEMY')
  assert.deepEqual(alchemy.state.implicits,state.implicits)
  check(key+' crafting preserves canonical implicit',true)
}
const rawhide={...initials.belt.state,explicits:[],augmentSockets:null,catalystQuality:null}; delete rawhide.modifierIds
const r=await apply(rawhide,'DIVINE')
check('Historical Rawhide Divine remains refused',!r.applied)
assert.deepEqual(r.state,rawhide)
fs.writeFileSync(`${root}/results.json`,JSON.stringify({passed:true,count:checks.length,checks},null,2)+'\n')
console.log(checks.length,'implicit/slot/source-unit checks passed')
