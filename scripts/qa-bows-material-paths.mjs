import fs from 'node:fs'
import assert from 'node:assert/strict'
const root='/qa', origin='http://host.docker.internal:19780'
const initials=JSON.parse(fs.readFileSync(`${root}/api-initials.json`))
const registry=JSON.parse(fs.readFileSync('/source/backend/src/main/resources/crafting/registry-v2.json'))
const checks=[], paths=[]
const check=(n,v)=>{assert(v,n);checks.push(n)}
const apply=async(state,action,activeOmens=[])=>{const r=await fetch(origin+'/api/v1/crafting/workbench/apply',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({state,action,activeOmens})});assert.equal(r.status,200);return r.json()}
const inst=d=>({modifierId:d.id,values:Object.fromEntries(d.stats.map(s=>[s.id,s.min])),fractured:false})
for(const key of ['warmonger','guardian','gemini','fanatic','obliterator']) {
  const i=initials[key],defs=i.modifiers, base={...i.state,explicits:[],augmentSockets:null,catalystQuality:null};delete base.modifierIds
  const dex=inst(defs['crude-bow:suffix:of-the-mongoose']),phys=inst(defs['crude-bow:prefix:glinting'])
  const magic={...base,rarity:'MAGIC',explicits:[dex]}, rare={...base,rarity:'RARE',explicits:[phys,dex]}
  const alchemy=(await apply(base,'ALCHEMY')).state
  for(const e of registry.entries.filter(e=>e.category==='CURRENCY'&&e.serviceScope==='ACTIVE'&&e.supportedBases?.includes(key))) {
    const a=e.action, kind=a.replace(/^(GREATER|PERFECT)_/,'')
    const start=['TRANSMUTATION','ALCHEMY'].includes(kind)?base:['AUGMENTATION','REGAL'].includes(kind)?magic:kind==='FRACTURING'?alchemy:rare
    const result=await apply(start,a)
    check(`${key}/${e.id}: actual positive`,result.applied)
    if(kind!=='DIVINE') assert.deepEqual(result.state.implicits,base.implicits)
    if(kind==='DIVINE') {
      assert.deepEqual(result.state.implicits.map(m=>m.modifierId),base.implicits.map(m=>m.modifierId))
      assert.deepEqual(result.state.explicits.map(m=>m.modifierId),rare.explicits.map(m=>m.modifierId))
    }
    check(`${key}/${e.id}: identity and canonical stats`,result.state.baseItemId===base.baseItemId && [...result.state.implicits,...result.state.explicits].every(m=>defs[m.modifierId].stats.every(s=>m.values[s.id]>=s.min&&m.values[s.id]<=s.max)))
    paths.push({key,id:e.id,action:a,positive:true})
  }
  const omens=registry.entries.filter(e=>e.category==='OMEN'&&e.serviceScope==='ACTIVE'&&e.supportedBases?.includes(key))
  for(const e of omens) {
    const id=e.id
    const a=id.includes('Crystallisation')?'ESSENCE_ABYSS':id.includes('Coronation')?'REGAL':id.includes('Alchemy')?'ALCHEMY':id.includes('Exaltation')?'EXALTED':id.includes('Annulment')?'ANNULMENT':id==='Omen_of_the_Blessed'?'DIVINE':'CHAOS'
    const start=a==='ALCHEMY'?base:a==='REGAL'?magic:rare
    const result=await apply(start,a,[id])
    check(`${key}/${id}: positive trigger and consumption`,result.applied&&result.consumedOmens.includes(id)&&result.remainingOmens.length===0)
    const side=id.includes('Sinistral')?'PREFIX':id.includes('Dextral')?'SUFFIX':null
    const events=result.events.filter(e=>e.kind==='ADD'||e.kind==='REMOVE')
    if(side && a!=='ALCHEMY') {
      const relevant=['CHAOS','ANNULMENT','ESSENCE_ABYSS'].includes(a)?events.filter(e=>e.kind==='REMOVE'):events.filter(e=>e.kind==='ADD')
      check(`${key}/${id}: exact side`,relevant.length>0&&relevant.every(e=>defs[e.modifierId].affixType===side))
    }
    if(a==='ALCHEMY') check(`${key}/${id}: maximum three on guaranteed side`,result.state.explicits.filter(m=>defs[m.modifierId].affixType===side).length===3 && result.state.explicits.length===4)
    if(id==='Omen_of_Greater_Annulment')check(key+' double removal',events.filter(e=>e.kind==='REMOVE').length===2)
    if(id==='Omen_of_Greater_Exaltation')check(key+' double addition',events.filter(e=>e.kind==='ADD').length===2)
    if(a==='DIVINE')assert.deepEqual(result.state.explicits,start.explicits)
    paths.push({key,id,action:a,positive:true})
  }
}
const result={passed:true,count:checks.length,checks,paths,currencyPaths:paths.filter(p=>!p.id.startsWith('Omen')).length,omenPaths:paths.filter(p=>p.id.startsWith('Omen')).length}
fs.writeFileSync(`${root}/material-paths-results.json`,JSON.stringify(result,null,2)+'\n')
console.log(result.count+' assertions; '+result.currencyPaths+' currency paths; '+result.omenPaths+' Omen paths')
