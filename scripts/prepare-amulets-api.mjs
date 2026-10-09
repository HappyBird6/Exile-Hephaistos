import fs from 'node:fs'
import assert from 'node:assert/strict'
const root='E:/WORK/Exile-Hephaistos/codex/amulets-qa-20261004'
let s=fs.readFileSync(`${root}/qa-api-2.mjs`,'utf8')
s=s.replace("  const invalidQuality={...rare",`  const bucket={...i.state,modifierIds:[]}
  const goal={required:[{family:'IncreasedLife',minimumTier:1}],candidates:[],candidateCount:0}
  await post('/api/v1/crafting/support/assess',{state:bucket,goal},422);check(k+' Support stays Solar-only',true)
  await post('/api/v1/crafting/explore',{state:bucket,plan:['TRANSMUTATION'],maxNodes:10,maxEdges:10,maxMillis:100},422);check(k+' Explorer stays Solar-only',true)
  await post('/api/v1/crafting/actions',bucket,422);check(k+' default engine stays Solar-only',true)
  const defaultInitial=await get('/api/v1/crafting/initial?base='+k);check(k+' default initial stays Solar',defaultInitial.state.baseItemId===initials.solar.state.baseItemId)
  const invalidQuality={...rare`)
assert(!fs.existsSync(`${root}/qa-api-3.mjs`),'Preserve prior probe')
fs.writeFileSync(`${root}/qa-api-3.mjs`,s)
