import fs from 'node:fs'
import assert from 'node:assert/strict'
const initials=JSON.parse(fs.readFileSync('/qa/api-initials.json','utf8'))
const origin='http://host.docker.internal:19680'
const keys=['tasalian','drakeskin','sekhema','blacksteel-boots','faithful','daggerfoot'], checks=[]
const check=(name,v)=>{assert(v,name);checks.push(name)}
const instance=d=>({modifierId:d.id,values:Object.fromEntries(d.stats.map(s=>[s.id,s.min])),fractured:false})
const post=async(data)=>{const r=await fetch(origin+'/api/v1/crafting/workbench/apply',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(data)});return {status:r.status,body:await r.json()}}
// CraftingErrors treats syntactically valid but semantically invalid states as 422.
const invalidState=r=>r.status===422&&r.body.status===422&&r.body.code==='INVALID_CRAFTING_REQUEST'
const oldInitials=JSON.parse(fs.readFileSync('/qa/baseline-api-initials.json','utf8'))
const canonicalDefinitions=definitions=>Object.fromEntries(Object.entries(definitions).map(([id,d])=>[id,{...d,familyIds:[...d.familyIds].sort(),tags:[...d.tags].sort()}]))
for(const [key,old] of Object.entries(oldInitials)) {
 assert.deepEqual(initials[key].metadata,old.metadata)
 // Backend familyIds/tags are Sets: retain exact members, but not JVM-specific iteration order.
 assert.deepEqual(canonicalDefinitions(initials[key].modifiers),canonicalDefinitions(old.modifiers),key+' historical definitions')
 assert.equal(initials[key].state.baseItemId,old.state.baseItemId)
 assert.equal(initials[key].state.snapshotId,old.state.snapshotId)
 check(key+' previous exact snapshot and modifier definitions retained',true)
}
for(const key of keys) {
 const i=initials[key],defs=Object.values(i.modifiers),normal={...i.state,explicits:[],augmentSockets:null,catalystQuality:null};delete normal.modifierIds
 const movement=defs.filter(d=>d.familyIds.includes('MovementVelocity')).sort((a,b)=>a.requiredItemLevel-b.requiredItemLevel)
 check(key+' movement exact source levels',JSON.stringify(movement.map(d=>d.requiredItemLevel))===JSON.stringify([1,16,33,46,65,82]))
 check(key+' movement exact source values',JSON.stringify(movement.map(d=>d.stats.map(s=>[s.min,s.max])))===JSON.stringify([10,15,20,25,30,35].map(n=>[[n,n]])))
 const life=defs.find(d=>d.familyIds.includes('IncreasedLife')&&d.requiredItemLevel===1),fire=defs.find(d=>d.familyIds.includes('FireResistance')&&d.requiredItemLevel===1)
 const magic={...normal,rarity:'MAGIC',explicits:[instance(life)]},rare={...normal,rarity:'RARE',explicits:[instance(life),instance(fire)]}
 for(const tier of ['','GREATER_','PERFECT_']) for(const action of ['TRANSMUTATION','AUGMENTATION','REGAL','EXALTED','CHAOS']) {
  const state=action==='TRANSMUTATION'?normal:['AUGMENTATION','REGAL'].includes(action)?magic:rare
  const r=await post({state,action:tier+action,activeOmens:[]})
  check(key+' '+tier+action+' positive',r.status===200&&r.body.applied&&r.body.state.baseItemId===normal.baseItemId)
 }
 for(const [index,d] of movement.entries()) {
  const state={...normal,rarity:'RARE',explicits:[instance(life),instance(d)]}
  const r=await post({state,action:'DIVINE',activeOmens:[]})
  check(key+' movement tier '+index+' fixed roll preserved',r.status===200&&r.body.applied&&r.body.state.explicits.find(m=>m.modifierId===d.id).values[d.stats[0].id]===d.stats[0].min)
  const out=await post({state:{...state,explicits:[{...instance(d),values:{[d.stats[0].id]:d.stats[0].max+1}}]},action:'DIVINE',activeOmens:[]})
  check(key+' movement tier '+index+' out of range rejected',invalidState(out))
  if(d.requiredItemLevel>1) {
   const low=await post({state:{...state,itemLevel:d.requiredItemLevel-1},action:'DIVINE',activeOmens:[]})
   check(key+' movement tier '+index+' existing roll preserved below generation level',low.status===200&&low.body.applied&&low.body.state.explicits.find(m=>m.modifierId===d.id).values[d.stats[0].id]===d.stats[0].min)
   for(const level of [d.requiredItemLevel-1,d.requiredItemLevel]) {
    const generated=await post({state:{...normal,itemLevel:level,rarity:'RARE',explicits:[instance(fire)]},action:'EXALTED',activeOmens:['Omen_of_Sinistral_Exaltation']})
    check(key+' movement tier '+index+' generation level '+level+' applies',generated.status===200&&generated.body.applied)
    const candidates=generated.body.assumptions.find(a=>a.id==='boots-uniform-candidates-v1').candidates
    assert.deepEqual(candidates.filter(id=>i.modifiers[id].familyIds.includes('MovementVelocity')).sort(),movement.filter(m=>m.requiredItemLevel<=level).map(m=>m.id).sort())
    check(key+' movement tier '+index+' generation level '+level+' exact sourced speed pool',true)
    check(key+' movement tier '+index+' generation level '+level+' generated affixes obey level',generated.body.state.explicits.every(m=>i.modifiers[m.modifierId].requiredItemLevel<=level))
   }
  }
 }
 const duplicate=await post({state:{...rare,explicits:[instance(movement[0]),instance(movement[1])]},action:'EXALTED',activeOmens:[]})
 check(key+' movement family conflict rejected',invalidState(duplicate))
 const implicit=await post({state:{...normal,implicits:[instance(movement[0])]},action:'TRANSMUTATION',activeOmens:[]})
 check(key+' fabricated implicit rejected',invalidState(implicit))
 const low=await post({state:{...rare,itemLevel:64},action:'ESSENCE_HYSTERIA',activeOmens:['Omen_of_Sinistral_Crystallisation']})
 check(key+' Hysteria exact level65 refusal atomic',low.status===200&&!low.body.applied&&low.body.events.length===0&&low.body.consumedOmens.length===0)
 for(const [omen,action,state] of [
  ['Omen_of_Whittling','CHAOS',rare],
  ['Omen_of_Sinistral_Erasure','CHAOS',rare],['Omen_of_Dextral_Erasure','CHAOS',rare],
  ['Omen_of_Sinistral_Alchemy','ALCHEMY',normal],['Omen_of_Dextral_Alchemy','ALCHEMY',normal],
  ['Omen_of_Sinistral_Coronation','REGAL',magic],['Omen_of_Dextral_Coronation','REGAL',magic],
  ['Omen_of_Greater_Exaltation','EXALTED',rare],
  ['Omen_of_Sinistral_Exaltation','EXALTED',rare],['Omen_of_Dextral_Exaltation','EXALTED',rare],
  ['Omen_of_Greater_Annulment','ANNULMENT',rare],
  ['Omen_of_Sinistral_Annulment','ANNULMENT',rare],['Omen_of_Dextral_Annulment','ANNULMENT',rare],
  ['Omen_of_Sinistral_Crystallisation','ESSENCE_HYSTERIA',rare],['Omen_of_Dextral_Crystallisation','ESSENCE_HYSTERIA',rare],
 ]) {
  const r=await post({state,action,activeOmens:[omen]})
  check(key+' '+omen+' positive',r.status===200&&r.body.applied&&r.body.consumedOmens.includes(omen))
 }
 const blessed=await post({state:rare,action:'DIVINE',activeOmens:['Omen_of_the_Blessed']})
 check(key+' Blessed no implicit refusal atomic',blessed.status===200&&!blessed.body.applied&&blessed.body.events.length===0&&blessed.body.consumedOmens.length===0)
}
fs.writeFileSync('/qa/boots-contract-results.json',JSON.stringify({passed:true,count:checks.length,checks},null,2)+'\n')
console.log(checks.length+' Boots contract checks passed')
