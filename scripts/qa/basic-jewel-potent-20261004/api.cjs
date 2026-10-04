const fs=require('fs'),assert=require('assert/strict'),util=require('util');
const api='http://host.docker.internal:18880/api/v1/crafting/workbench',checks=[],captures=[],initials={};
function check(n,v){assert(v,n);checks.push(n)}
async function post(path,body,status=200){const r=await fetch(api+path,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({activeOmens:[],...body})});assert.equal(r.status,status,path+' '+JSON.stringify(await r.clone().json()));return r.json()}
const extra=(s,side)=>s.explicits.some(m=>m.modifierId.endsWith(':crafted:CraftedJewelAdditional'+(side==='PREFIX'?'Prefix':'Suffix')+'Allowed'))?1:0;
const capacity=(s,side)=>s.rarity==='NORMAL'?0:s.rarity==='MAGIC'?1:2+extra(s,side);
function ordinary(defs,p,s){const rows=[],families=new Set();for(const d of Object.values(defs))if(d.weight>0&&!d.familyIds.some(f=>families.has(f))&&(d.affixType==='PREFIX'?p>0:s>0)){rows.push(d.id);d.familyIds.forEach(f=>families.add(f));d.affixType==='PREFIX'?p--:s--}assert.equal(p+s,0);return rows}
(async()=>{
 const registry=await(await fetch(api+'/registry')).json();
 for(const [base,count]of Object.entries({ruby:50,emerald:74,sapphire:58,diamond:160})) {
  const initial=await(await fetch(api+'/initial?base='+base)).json();initials[base]=initial;const defs=initial.modifiers;
  const minimum=id=>({modifierId:id,values:Object.fromEntries(defs[id].stats.map(r=>[r.id,r.min])),fractured:false});
  const state=(rarity='RARE',ids=[])=>{const s={...initial.state,rarity,implicits:[],explicits:ids.map(minimum).sort((a,b)=>a.modifierId.localeCompare(b.modifierId)),augmentSockets:null,catalystQuality:{type:'CARAPACE',amount:20}};delete s.modifierIds;return s};
  function valid(s){let p=0,suf=0,crafted=0,f=new Set();for(const m of s.explicits){const d=defs[m.modifierId];check(base+' definition '+m.modifierId,!!d);check(base+' family '+m.modifierId,d.familyIds.every(id=>!f.has(id)));d.familyIds.forEach(id=>f.add(id));d.affixType==='PREFIX'?p++:suf++;if(d.tags.includes('crafted'))crafted++;for(const r of d.stats)check(base+' original bounds '+m.modifierId,Number.isSafeInteger(m.values[r.id])&&m.values[r.id]>=r.min&&m.values[r.id]<=r.max)}check(base+' existing capacity',s.rarity==='RARE'?p<=3&&suf<=3&&p+suf<=capacity(s,'PREFIX')+capacity(s,'SUFFIX'):p<=capacity(s,'PREFIX')&&suf<=capacity(s,'SUFFIX'));check(base+' crafted cap',crafted<=1);check(base+' quality retained',s.catalystQuality.amount===20)}
  async function apply(before,action,omens=[]){const result=await post('/apply',{state:before,action,activeOmens:omens});captures.push({base,before,action,activeOmens:omens,result});if(result.applied)valid(result.state);return result}
  check(base+' exact normal pool',Object.values(defs).filter(d=>d.weight>0).length===count);
  const full=state('RARE',ordinary(defs,2,2));
  for(const type of ['FLESH','NEURAL','CARAPACE','UUL_NETOL','XOPH','TUL','ESH','CHAYULA','REAVER','SIBILANT','SKITTERING','ADAPTIVE','NECROTIC']) {
   const r=await apply(full,'REFINED_CATALYST_'+type);
   check(base+' refined catalyst '+type,r.applied&&r.state.catalystQuality.type===type&&r.state.catalystQuality.amount===20);
   check(base+' refined canonical rolls '+type,util.isDeepStrictEqual(r.state.explicits,full.explicits));
  }
  for(const tier of ['', 'GREATER_','PERFECT_'])for(const kind of ['TRANSMUTATION','AUGMENTATION','REGAL','EXALTED','CHAOS']) {
   const rarity=kind==='TRANSMUTATION'?'NORMAL':['AUGMENTATION','REGAL'].includes(kind)?'MAGIC':'RARE';const before=state(rarity,rarity==='NORMAL'?[]:ordinary(defs,0,1));const r=await apply(before,tier+kind);check(base+' positive '+tier+kind,r.applied);check(base+' uniform '+tier+kind,r.assumptions.some(a=>a.id==='sapphire-uniform-candidates-v1'&&a.reason.includes('Actual game')));
  }
  for(const a of ['ALCHEMY','ANNULMENT','DIVINE'])check(base+' positive '+a,(await apply(state(a==='ALCHEMY'?'NORMAL':'RARE',a==='ALCHEMY'?[]:ordinary(defs,0,1)),a)).applied);
  const omen=await apply(full,'CHAOS',['Omen_of_Sinistral_Erasure','Omen_of_Whittling']);
  check(base+' side Whittling composition',omen.applied&&omen.consumedOmens.length===2&&defs[omen.events[0].modifierId].affixType==='PREFIX');
  for(const r of registry.entries.filter(r=>r.category==='LIQUID_EMOTION'&&r.action)) {
   const targets=Object.values(defs).filter(d=>d.tags.includes('crafted')&&d.sourceUrl===r.ruleSource);
   for(const before of [state('NORMAL'),state('MAGIC',ordinary(defs,0,1)),state('RARE')]) {
    const refused=await apply(before,r.action);
    check(base+' invalid rarity/empty '+r.id,!refused.applied&&util.isDeepStrictEqual(refused.state,before)&&refused.consumedOmens.length===0);
   }
   if(targets.length===2) {
    const sparse=await apply(state('RARE',ordinary(defs,1,0)),r.action);
    check(base+' two valid outcome model '+r.id,sparse.applied&&sparse.events[1].selectionProbability===0.5&&sparse.assumptions.some(a=>a.id==='uniform-liquid-outcomes-v1'&&a.n===2));
   }
   const seen=new Set();for(let repeat=0;repeat<(r.id.startsWith('Potent_')?24:1);repeat++) {
    const result=await apply(full,r.action,['Omen_of_Sinistral_Crystallisation']);check(base+' applicability '+r.id,result.applied===(targets.length>0));
    if(!targets.length){check(base+' refusal unchanged '+r.id,util.isDeepStrictEqual(result.state,full)&&result.consumedOmens.length===0);break}
    const removed=result.events[0],added=result.events[1];seen.add(added.modifierId);
    const legalRemoval=full.explicits.filter(m=>targets.some(t=>{const rest=full.explicits.filter(o=>o.modifierId!==m.modifierId);return rest.filter(o=>defs[o.modifierId].affixType===t.affixType).length<2&&rest.every(o=>!defs[o.modifierId].familyIds.some(f=>t.familyIds.includes(f)))}));
    const rest=full.explicits.filter(m=>m.modifierId!==removed.modifierId);
    const validTargets=targets.filter(t=>rest.filter(m=>defs[m.modifierId].affixType===t.affixType).length<2&&rest.every(m=>!defs[m.modifierId].familyIds.some(f=>t.familyIds.includes(f))));
    check(base+' Liquid removal 1/N '+r.id,removed.selectionProbability===1/legalRemoval.length);
    check(base+' Liquid valid outcome 1/N '+r.id,added.selectionProbability===1/validTargets.length);
    check(base+' exact legal outcome ledger '+r.id,result.assumptions.some(a=>a.id==='uniform-liquid-outcomes-v1'&&a.n===validTargets.length&&a.candidates.length===validTargets.length&&a.candidates.every(id=>validTargets.some(t=>t.id===id))));
    check(base+' no Omen spend '+r.id,result.consumedOmens.length===0&&result.remainingOmens.length===1);
    const again=await apply(result.state,r.action);check(base+' current Crafted blocks '+r.id,!again.applied&&util.isDeepStrictEqual(again.state,result.state));
    const annul=await apply(state('RARE',[added.modifierId]),'ANNULMENT');check(base+' ordinary removes Crafted '+r.id,annul.applied&&annul.state.explicits.length===0);
   }
   if(targets.length>1)check(base+' both Potent directions '+r.id,seen.size===2);
  }
  for(const side of ['PREFIX','SUFFIX']) {
   const code='CraftedJewelAdditional'+(side==='PREFIX'?'Prefix':'Suffix')+'Allowed';const expanded=state('RARE',[...ordinary(defs,side==='PREFIX'?3:1,side==='SUFFIX'?3:1),base+':crafted:'+code]);
   let lost;
   for(let n=0;n<40&&!lost;n++){const r=await apply(expanded,'ANNULMENT');if(r.events[0].modifierId.endsWith(code))lost=r}
   check(base+' actual cap loss '+side,!!lost&&lost.state.explicits.length===4);
   check(base+' overflow ledger '+side,lost.assumptions.some(a=>a.id==='jewel-cap-loss-preserve-v1'));
   const refused=await apply(lost.state,'EXALTED');check(base+' overflow insertion no spend '+side,!refused.applied&&util.isDeepStrictEqual(refused.state,lost.state));
   await post('/actions',{state:lost.state});
   const divine=await apply(lost.state,'DIVINE');check(base+' overflow survives Divine '+side,divine.applied);
  }
  const d=Object.values(defs).find(d=>d.weight>0&&d.affixType==='PREFIX'&&d.tags.includes('defences'));
  const effect=state('RARE',[d.id,base+':crafted:CraftedJewelPrefixEffect']);effect.explicits.find(m=>m.modifierId.includes(':crafted:')).values.display_source_value=50;
  const display=await post('/quality-display',{state:effect});const projection=display.modifiers.find(m=>m.modifierId===d.id),v=effect.explicits.find(m=>m.modifierId===d.id).values;
  check(base+' effect original preserved',util.isDeepStrictEqual(projection.originalValues,v));check(base+' effect quality one rounding',projection.displayedValues[d.stats[0].id]===Math.round(v[d.stats[0].id]*1.2*1.5));
  for(const a of ['ESSENCE_HYSTERIA','FRACTURING','CATALYST_FLESH'])check(base+' excluded '+a,!(await apply(full,a)).applied);
  const error=await post('/apply',{state:{...full,baseItemId:'Metadata/Items/Jewels/TimeLostJewelInt'},action:'EXALTED'},422);check(base+' Problem Details',error.status===422&&!!error.type);
 }
 fs.writeFileSync('/evidence/api-results.json',JSON.stringify({checks:checks.length,initials,captures},null,2));console.log('PASS '+checks.length);
})().catch(e=>{fs.writeFileSync('/evidence/api-failure-1.json',JSON.stringify({error:e.stack,checks,captures},null,2));console.error(e);process.exit(1)});
