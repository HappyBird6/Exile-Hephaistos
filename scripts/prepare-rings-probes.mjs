import fs from 'node:fs'
const root='E:/WORK/Exile-Hephaistos/codex/rings-qa-20261004'
const keys="['kinetic','vitalic','mnemonic','pearl','amethyst','prismatic','ruby-ring','two-stone-fire-cold']"
const write=(p,s)=>{if(fs.existsSync(p))throw Error('Preserve prior QA probe '+p);fs.writeFileSync(p,s)}
let s=fs.readFileSync('scripts/qa-bows-api.mjs','utf8')
s=s.replaceAll('19780','19880').replace("['warmonger','guardian','gemini','fanatic','obliterator']",keys).replaceAll("initials.bow.state.snapshotId","initials.ring.state.snapshotId").replaceAll("crude-bow:prefix:glinting","iron-ring:prefix:hale").replaceAll("crude-bow:prefix:honed","iron-ring:prefix:healthy")
s=s.replace("if(a!=='ESSENCE_ABYSS') {", "if(a!=='ESSENCE_ABYSS' && a!=='ESSENCE_BREACH') {").replaceAll("{...rare,itemLevel:71}","{...rare,itemLevel:i.modifiers[ids[0]].requiredItemLevel-1}").replace("source72 boundary","exact sourced itemlevel boundary")
s=s.replace("['PERFECT_ESSENCE_BODY','PERFECT_ESSENCE_MIND','ESSENCE_HYSTERIA','ARTIFICER','ESSENCE_HORROR','RUNIC_ALLOY','CATALYST_FLESH']","['PERFECT_ESSENCE_BODY','PERFECT_ESSENCE_SORCERY','ARTIFICER','ESSENCE_HORROR','RUNIC_ALLOY','REFINED_CATALYST_FLESH']")
const begin=s.indexOf("  check(k+' Blessed variable implicit only'")
const end=s.indexOf('\n}',begin)
s=s.slice(0,begin)+`  check(k+' Blessed variable implicit',blessed.applied && blessed.consumedOmens.includes('Omen_of_the_Blessed') && b.implicitStats.every(stat=>blessed.state.implicits[0].values[stat.id]>=stat.min && blessed.state.implicits[0].values[stat.id]<=stat.max))
  const catalysts=registryForCatalysts.entries.filter(e=>e.category==='CATALYST' && e.serviceScope==='ACTIVE' && e.supportedBases.includes(k))
  check(k+' thirteen ordinary Catalysts',catalysts.length===13)
  for(const e of catalysts){
    const r=await apply(rare,e.action); check(k+' '+e.action+' cap20 and canonical state',r.applied && r.state.catalystQuality.amount===20)
    assert.deepEqual(r.state.implicits,rare.implicits);assert.deepEqual(r.state.explicits,rare.explicits)
    const q=await post('/api/v1/crafting/workbench/quality-display',{state:r.state})
    check(k+' '+e.action+' projection reachable',q.qualityLimit.maximumQuality===20)
    const tags={FLESH:['life'],NEURAL:['mana'],CARAPACE:['defences','armour','evasion','energyshield'],UUL_NETOL:['physical'],XOPH:['fire'],TUL:['cold'],ESH:['lightning'],CHAYULA:['chaos'],REAVER:['attack'],SIBILANT:['caster'],SKITTERING:['speed'],ADAPTIVE:['attribute'],NECROTIC:['minion']}[r.state.catalystQuality.type]
    const implicit=q.modifiers.find(d=>d.modifierId===b.implicitModifierId),matches=tags.some(t=>i.modifiers[b.implicitModifierId].tags.includes(t))
    assert.deepEqual(implicit.originalValues,start.implicits[0].values)
    check(k+' '+e.action+' exact source tag status',implicit.status===(matches?'SCALED_INTEGER':'NO_MATCH'))
    for(const stat of b.implicitStats)check(k+' '+e.action+' single scaling '+stat.id,implicit.displayedValues[stat.id]===(matches?Math.round(stat.max*1.2):stat.max))
    const switched=await apply(r.state,e.action==='CATALYST_FLESH'?'CATALYST_NEURAL':'CATALYST_FLESH')
    check(k+' Catalyst switch does not accumulate',switched.applied && switched.state.catalystQuality.amount===20)
  }
  const breach=await apply(rare,'ESSENCE_BREACH')
  check(k+' Breach source cap40',breach.applied && breach.qualityLimit.maximumQuality===40)
  const typed=await apply(breach.state,'CATALYST_REAVER')
  check(k+' Breach Catalyst40',typed.applied && typed.state.catalystQuality.amount===40)
  const stripped=await apply({...typed.state,explicits:typed.state.explicits.filter(m=>m.modifierId==='amulet:prefix:essence-maximum-quality')},'ANNULMENT')
  check(k+' cap loss keeps quality40',stripped.applied && stripped.qualityLimit.maximumQuality===20 && stripped.state.catalystQuality.amount===40)
  const switched=await apply(stripped.state,'CATALYST_FLESH')
  check(k+' overcap switch keeps quality40',switched.applied && switched.state.catalystQuality.amount===40 && switched.qualityLimit.maximumQuality===20)
  const foreign=structuredClone(start);foreign.implicits=concrete(initials.ring).implicits
  await post('/api/v1/crafting/workbench/apply',{state:foreign,action:'TRANSMUTATION',activeOmens:[]},422);check(k+' foreign Iron implicit422',true)
  const invalidQuality={...rare,catalystQuality:{type:'FLESH',amount:41}}
  await post('/api/v1/crafting/workbench/apply',{state:invalidQuality,action:'CATALYST_FLESH',activeOmens:[]},422);check(k+' invalid quality41 rejected',true)
`+s.slice(end)
s=s.replace("const newKeys=", "const registryForCatalysts=read('/source/backend/src/main/resources/crafting/registry-v2.json')\nconst newKeys=")
write(`${root}/qa-api.mjs`,s)
s=fs.readFileSync('scripts/qa-bows-material-paths.mjs','utf8').replaceAll('19780','19880').replace("['warmonger','guardian','gemini','fanatic','obliterator']",keys).replaceAll('crude-bow:suffix:of-the-mongoose','iron-ring:suffix:of-the-mongoose').replaceAll('crude-bow:prefix:glinting','iron-ring:prefix:hale')
s=s.replace("e.supportedBases?.includes(key))\n  for(const e of omens)","(e.supportedBases?.includes(key) || ['Omen_of_Homogenising_Exaltation','Omen_of_Homogenising_Coronation'].includes(e.id)))\n  for(const e of omens)")
write(`${root}/qa-material-paths.mjs`,s)
console.log('Prepared Ring API contracts with source boundaries, crossclass rejections and all thirteen Catalyst switches')
