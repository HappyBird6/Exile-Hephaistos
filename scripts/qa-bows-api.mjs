import fs from 'node:fs'
import assert from 'node:assert/strict'
const root = '/qa', origin = 'http://host.docker.internal:19780'
const read = p => JSON.parse(fs.readFileSync(p, 'utf8'))
const bases = read('/source/frontend/src/features/crafting/topBases.json')
const overrides = read('/source/backend/src/main/resources/catalog/top-base-essences.json')
const checks = [], initials = {}
const check = (n,v) => { assert(v,n); checks.push(n) }
const get = async p => { const r = await fetch(origin+p); assert.equal(r.status,200); return r.json() }
const post = async (p,b,status=200) => { const r = await fetch(origin+p,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(b)}); assert.equal(r.status,status); return r.json() }
const initial = (k,l=82) => get(`/api/v1/crafting/${k==='solar'?'':'workbench/'}initial?base=${k}&itemLevel=${l}`)
const concrete = i => { const s={...i.state,explicits:[],augmentSockets:null,catalystQuality:null}; delete s.modifierIds; return s }
const apply = (s,a,o=[]) => post('/api/v1/crafting/workbench/apply',{state:s,action:a,activeOmens:o})
const instance = d => ({modifierId:d.id,values:Object.fromEntries(d.stats.map(s=>[s.id,s.min])),fractured:false})
const newKeys=['warmonger','guardian','gemini','fanatic','obliterator']
const baseline = read(`${root}/baseline-api-initials.json`)
const canonical = d => ({...d,tags:[...d.tags].sort(),familyIds:[...d.familyIds].sort()})
for(const k of [...Object.keys(baseline),...newKeys]) {
  initials[k]=await initial(k)
  if(baseline[k]) {
    const stable = i => ({ ...i, modifiers: Object.fromEntries(Object.entries(i.modifiers).map(([id,d])=>[id,canonical(d)])) })
    assert.deepEqual(stable(initials[k]),stable(baseline[k])); check(k+' historical initial unchanged (unordered sets canonicalized)',true)
  }
}
for(const k of newKeys) {
  const i=initials[k], b=bases[k], ds=Object.values(i.modifiers), start=concrete(i)
  const catalog=read(`/source/backend/src/main/resources/catalog/${b.pool}/catalog.json`)
  const gateAllows = (id, minimum) => {
    const d=i.modifiers[id]
    return d.requiredItemLevel>=minimum || !ds.some(other=>other.weight>0 && other.requiredItemLevel<=82 && other.requiredItemLevel>d.requiredItemLevel && other.affixType===d.affixType && other.familyIds[0]===d.familyIds[0])
  }
  assert.deepEqual(ds.map(canonical).sort((a,b)=>a.id.localeCompare(b.id)),catalog.modifiers.map(canonical).sort((a,b)=>a.id.localeCompare(b.id)))
  check(k+' all sourced definitions preserved',true)
  check(k+' exact base identity and distinct snapshot',i.state.baseItemId===b.id && i.state.snapshotId!==initials.bow.state.snapshotId)
  check(k+' cap20 unknown sockets',i.qualityLimit.maximumQuality===20 && i.augmentSockets===null)
  check(k+' implicit source ID and canonical maximum',b.implicitModifierId?start.implicits.length===1 && start.implicits[0].modifierId===b.implicitModifierId && b.implicitStats.every(s=>start.implicits[0].values[s.id]===s.max):start.implicits.length===0)
  let s=start
  for(const a of ['TRANSMUTATION','AUGMENTATION','REGAL','EXALTED','ANNULMENT','CHAOS']) {
    const r=await apply(s,a);check(k+' '+a+' positive',r.applied)
    assert.deepEqual(r.state.implicits,start.implicits);check(k+' '+a+' preserves implicit',true)
    for(const e of r.events.filter(e=>e.kind==='ADD'))check(k+' '+a+' legal positive selection',e.selectionProbability>0 && ds.find(d=>d.id===e.modifierId).requiredItemLevel<=82)
    s=r.state
  }
  const dex=ds.find(d=>d.weight>0 && d.familyIds.includes('Dexterity') && d.requiredItemLevel===1)
  const phys=ds.find(d=>d.id==='crude-bow:prefix:glinting')
  const rare={...start,rarity:'RARE',explicits:[instance(phys),instance(dex)]}
  for(const [a,ids] of Object.entries(overrides[k].fixed)) {
    const families=new Set(ids.flatMap(id=>i.modifiers[id].familyIds))
    const companion=ds.find(d=>d.weight>0 && d.requiredItemLevel===1 && d.familyIds.every(f=>!families.has(f)))
    const r=await apply({...start,rarity:'MAGIC',explicits:[instance(companion)]},a)
    check(k+' '+a+' guaranteed positive',r.applied && ids.includes(r.events.at(-1).modifierId))
    assert.deepEqual(r.state.implicits,start.implicits)
  }
  for(const [a,ids] of Object.entries(overrides[k].replacements)) {
    const r=await apply(rare,a,['Omen_of_Sinistral_Crystallisation'])
    check(k+' '+a+' Omen positive',r.applied && ids.includes(r.events.at(-1).modifierId) && r.consumedOmens.includes('Omen_of_Sinistral_Crystallisation'))
    assert.deepEqual(r.state.implicits,start.implicits)
    const refused=await apply(rare,a,['Omen_of_Sinistral_Crystallisation','Omen_of_Dextral_Crystallisation'])
    assert.deepEqual(refused.state,rare)
    check(k+' '+a+' conflict atomic',!refused.applied && refused.events.length===0 && refused.consumedOmens.length===0)
    if(a!=='ESSENCE_ABYSS') {
      const low=await apply({...rare,itemLevel:71},a); assert.deepEqual(low.state,{...rare,itemLevel:71})
      check(k+' '+a+' source72 boundary',!low.applied && low.events.length===0)
    }
  }
  for(const a of ['PERFECT_ESSENCE_BODY','PERFECT_ESSENCE_MIND','ESSENCE_HYSTERIA','ARTIFICER','ESSENCE_HORROR','RUNIC_ALLOY','CATALYST_FLESH']) {
    const r=await apply(rare,a);assert.deepEqual(r.state,rare);check(k+' '+a+' unsupported atomic',!r.applied && r.events.length===0 && r.consumedOmens.length===0)
  }
  for(const a of ['ALCHEMY','PERFECT_TRANSMUTATION','GREATER_TRANSMUTATION']) {
    const r=await apply(start,a);check(k+' '+a+' positive',r.applied)
    const min=a==='PERFECT_TRANSMUTATION'?70:a==='GREATER_TRANSMUTATION'?44:1
    check(k+' '+a+' source minlevels and highest-family exception',r.state.explicits.every(m=>gateAllows(m.modifierId,min)))
  }
  const low=concrete(await initial(k,1)), lowRoll=await apply(low,'TRANSMUTATION')
  check(k+' lowilvl pool',lowRoll.applied && lowRoll.state.explicits.every(m=>i.modifiers[m.modifierId].requiredItemLevel===1))
  for(const a of ['GREATER_EXALTED','PERFECT_EXALTED']) {
    const min=a==='GREATER_EXALTED'?35:50
    const r=await apply(rare,a);check(k+' '+a+' positive minlevel and highest-family exception',r.applied && r.events.filter(e=>e.kind==='ADD').every(e=>gateAllows(e.modifierId,min)))
    const refused=await apply({...rare,itemLevel:min-1},a);check(k+' '+a+' lowlevel refusal',!refused.applied && refused.events.length===0)
  }
  const alchemy=await apply(start,'ALCHEMY'), fracture=await apply(alchemy.state,'FRACTURING')
  check(k+' fracture positive',fracture.applied && fracture.state.explicits.filter(m=>m.fractured).length===1)
  const locked=fracture.state.explicits.find(m=>m.fractured), chaos=await apply(fracture.state,'CHAOS')
  assert.deepEqual(chaos.state.explicits.find(m=>m.modifierId===locked.modifierId),locked);check(k+' canonical fractured roll retained',true)
  const honed=ds.find(d=>d.id==='crude-bow:prefix:honed')
  const conflict={...rare,explicits:[instance(phys),instance(honed)]}
  await post('/api/v1/crafting/workbench/apply',{state:conflict,action:'CHAOS',activeOmens:[]},422);check(k+' invalid duplicate family422',true)
  const fullness=await apply(rare,'EXALTED',['Omen_of_Sinistral_Exaltation'])
  check(k+' side Omen actual positive',fullness.applied && fullness.consumedOmens.includes('Omen_of_Sinistral_Exaltation') && i.modifiers[fullness.events.at(-1).modifierId].affixType==='PREFIX')
  const quality=await post('/api/v1/crafting/workbench/quality-display',{state:rare})
  check(k+' quality endpoint exact state',quality.qualityLimit.maximumQuality===20 && quality.state.baseItemId===b.id)
  const blessed=await apply(start,'DIVINE',['Omen_of_the_Blessed'])
  check(k+' Blessed variable implicit only',k==='guardian'?blessed.applied && blessed.consumedOmens.includes('Omen_of_the_Blessed') && blessed.state.implicits[0].values[b.implicitStats[0].id]>=25 && blessed.state.implicits[0].values[b.implicitStats[0].id]<=35:!blessed.applied && blessed.events.length===0)
}
const registry=read('/source/backend/src/main/resources/crafting/registry-v2.json'), old=read(`${root}/baseline-registry.json`)
assert.deepEqual(await get('/api/v1/crafting/workbench/registry'),registry)
for(let n=0;n<old.entries.length;n++) {const e=structuredClone(registry.entries[n]);if(e.supportedBases)e.supportedBases=e.supportedBases.filter(k=>!newKeys.includes(k));assert.deepEqual(e,old.entries[n])}
assert.deepEqual(registry.serviceScope,old.serviceScope);check('default170 deferred50 SupportExplorer Solar unchanged',registry.serviceScope.deferred===50)
for(const [filename,currentPath] of [['topBases','/source/frontend/src/features/crafting/topBases.json'],['gameTerms','/source/frontend/src/shared/i18n/gameTerms.json'],['modifierTemplates','/source/frontend/src/shared/i18n/modifierTemplates.json']]) {
  const prior=read(`${root}/baseline-${filename}.json`), now=read(currentPath)
  if(filename==='topBases')for(const [k,v] of Object.entries(prior))assert.deepEqual(now[k],v)
  else if(filename==='gameTerms')for(const l of Object.keys(prior))for(const [k,v] of Object.entries(prior[l]))assert.deepEqual(now[l][k],v)
  else {for(const [k,v] of Object.entries(prior.definitions))assert.deepEqual(now.definitions[k],v);for(const l of Object.keys(prior.templates))for(const [k,v] of Object.entries(prior.templates[l]))assert.deepEqual(now.templates[l][k],v)}
  check(filename+' all prior records retained',true)
}
fs.writeFileSync(`${root}/api-initials.json`,JSON.stringify(initials,null,2)+'\n')
fs.writeFileSync(`${root}/api-results.json`,JSON.stringify({passed:true,count:checks.length,checks,baseCount:Object.keys(initials).length},null,2)+'\n')
console.log(checks.length+' API assertions passed')
