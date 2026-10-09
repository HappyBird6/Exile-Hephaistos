import fs from 'node:fs'
import assert from 'node:assert/strict'
const q = 'E:/WORK/Exile-Hephaistos/codex/belts-qa-20261005'
let probe = fs.readFileSync(`${q}/qa-api.mjs`, 'utf8').replace("'/qa/api-attempt-1'", "'/qa/api-attempt-2'")
const before = "const low = {...pair,itemLevel:Math.min(...ids.map(id=>i.modifiers[id].requiredItemLevel))-1}, rejected = await apply(low,action)\n    check(key+' Perfect source level72 '+action,!rejected.applied)\n    assert.deepEqual(rejected.state,low)"
assert(probe.includes(before))
probe = probe.replace(before, `const minimum = Math.min(...ids.map(id=>i.modifiers[id].requiredItemLevel))
    const low = {...pair,itemLevel:minimum-1}
    if (minimum === 1) {
      const problem = await post('/api/v1/crafting/workbench/apply',{state:low,action,activeOmens:[]},422)
      check(key+' rejects invalid ilvl0 '+action,problem.status === 422)
      check(key+' source minimum1 positive '+action,(await apply({...pair,itemLevel:1},action)).applied)
    } else {
      const rejected = await apply(low,action)
      check(key+' source minimum '+minimum+' '+action,!rejected.applied)
      assert.deepEqual(rejected.state,low)
    }`)
fs.writeFileSync(`${q}/qa-api-2.mjs`, probe, { flag: 'wx' })
