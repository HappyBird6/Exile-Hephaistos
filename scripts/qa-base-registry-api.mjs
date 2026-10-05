import fs from 'node:fs'
import assert from 'node:assert/strict'
const baseline=JSON.parse(fs.readFileSync('/qa/baseline125-api-initials.json'))
const policies=JSON.parse(fs.readFileSync('/source/frontend/src/features/crafting/basePolicies.json'))
const bases=JSON.parse(fs.readFileSync('/source/frontend/src/features/crafting/topBases.json'))
const root='http://app:8080',checks=[]
const stable=x=>JSON.parse(JSON.stringify(x,(_,v)=>Array.isArray(v)&&v.every(x=>typeof x==='string')?[...v].sort():v))
const policy=key=>bases[key]?{...policies.families[bases[key].family],...policies.baseOverrides[key]}:policies.legacy[key]
async function request(path,body,status=200) {const r=await fetch(root+path,body?{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)}:{});assert.equal(r.status,status,path);return r.json()}
for(const [key,expected]of Object.entries(baseline)) {
 const i=await request(`/api/v1/crafting/${key==='solar'?'':'workbench/'}initial?base=${key}&itemLevel=82`)
 assert.deepEqual(stable(i),stable(expected),key+' exact historical API response');checks.push(key+' exact initial/schema/full pool/source identity')
 const s={...i.state,explicits:(i.state.modifierIds??[]).map(id=>({modifierId:id,values:Object.fromEntries(i.modifiers[id].stats.map(s=>[s.id,s.max])),fractured:false})),augmentSockets:i.augmentSockets??null,catalystQuality:null};delete s.modifierIds
 const apply=(state,action,status=200)=>request('/api/v1/crafting/workbench/apply',{state,action,activeOmens:[]},status)
 const p=policy(key)
 if(p.socketExecutionMaximum==null) {await apply({...s,augmentSockets:1},'TRANSMUTATION',422);checks.push(key+' socket state remains unsupported')}
 if(!p.catalystQuality) {await apply({...s,catalystQuality:{type:'FLESH',amount:20}},'TRANSMUTATION',422);checks.push(key+' typed quality state remains unsupported')}
 if(!p.ordinaryCatalyst) {const result=await apply(s,'CATALYST_FLESH');assert.equal(result.applied,false);checks.push(key+' ordinary Catalyst blocked')}
 if(!p.refinedCatalyst) {const result=await apply(s,'REFINED_CATALYST_FLESH');assert.equal(result.applied,false);checks.push(key+' refined Catalyst blocked')}
}
fs.writeFileSync('/qa/api-registry-results.json',JSON.stringify({passed:true,bases:125,count:checks.length,checks},null,2)+'\n',{flag:'wx'})
console.log(checks.length,'aggregate registry API parity/restriction checks passed')
