const fs=require('fs'),assert=require('assert/strict'),util=require('util');
const api='http://host.docker.internal:18980/api/v1/crafting/workbench',checks=[],captures=[],initials={};
function check(n,v){assert(v,n);checks.push(n)}
async function post(path,body,status=200){const r=await fetch(api+path,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({activeOmens:[],...body})});assert.equal(r.status,status,path+' '+JSON.stringify(await r.clone().json()));return r.json()}
const extra=(s,side)=>s.explicits.some(m=>m.modifierId.endsWith(':crafted:CraftedJewelAdditional'+(side==='PREFIX'?'Prefix':'Suffix')+'Allowed'))?1:0;
const capacity=(s,side)=>s.rarity==='NORMAL'?0:s.rarity==='MAGIC'?1:2+extra(s,side);
function ordinary(defs,p,s,exclude=[]){const rows=[],families=new Set(exclude);for(const d of Object.values(defs))if(d.weight>0&&!d.familyIds.some(f=>families.has(f))&&(d.affixType==='PREFIX'?p>0:s>0)){rows.push(d.id);d.familyIds.forEach(f=>families.add(f));d.affixType==='PREFIX'?p--:s--}assert.equal(p+s,0);return rows}
(async()=>{
 const registry=await(await fetch(api+'/registry')).json();
 for(const [base,count]of Object.entries({'time-lost-ruby':53,'time-lost-emerald':77,'time-lost-sapphire':60,'time-lost-diamond':160})) {
  const initial=await(await fetch(api+'/initial?base='+base)).json();initials[base]=initial;const defs=initial.modifiers;
  const minimum=id=>({modifierId:id,values:Object.fromEntries(defs[id].stats.map(r=>[r.id,r.min])),fractured:false});
  const state=(rarity='RARE',ids=[])=>{const s={...initial.state,rarity,explicits:ids.map(minimum).sort((a,b)=>a.modifierId.localeCompare(b.modifierId)),augmentSockets:null,catalystQuality:null};delete s.modifierIds;return s};
  function valid(s){let p=0,suf=0,crafted=0,f=new Set();for(const m of s.explicits){const d=defs[m.modifierId];check(base+' family '+m.modifierId,d.familyIds.every(id=>!f.has(id)));d.familyIds.forEach(id=>f.add(id));d.affixType==='PREFIX'?p++:suf++;if(d.tags.includes('crafted'))crafted++;for(const r of d.stats)check(base+' canonical bounds '+m.modifierId,Number.isSafeInteger(m.values[r.id])&&m.values[r.id]>=r.min&&m.values[r.id]<=r.max)}check(base+' capacity',s.rarity==='RARE'?p<=3&&suf<=3&&p+suf<=capacity(s,'PREFIX')+capacity(s,'SUFFIX'):p<=capacity(s,'PREFIX')&&suf<=capacity(s,'SUFFIX'));check(base+' one Crafted',crafted<=1);check(base+' original implicit preserved',util.isDeepStrictEqual(s.implicits,initial.state.implicits));check(base+' no inferred catalyst quality',s.catalystQuality==null)}
  async function apply(before,action,omens=[]){const result=await post('/apply',{state:before,action,activeOmens:omens});captures.push({base,before,action,activeOmens:omens,result});if(result.applied)valid(result.state);return result}
  const full=state('RARE',ordinary(defs,2,2));check(base+' normal count',Object.values(defs).filter(d=>d.weight>0).length===count);
  check(base+' implicit fixed source value',initial.state.implicits[0].values.local_jewel_effect_base_radius===1000);
  for(const tier of ['', 'GREATER_','PERFECT_'])for(const kind of ['TRANSMUTATION','AUGMENTATION','REGAL','EXALTED','CHAOS']){
   const rarity=kind==='TRANSMUTATION'?'NORMAL':['AUGMENTATION','REGAL'].includes(kind)?'MAGIC':'RARE';const before=state(rarity,rarity==='NORMAL'?[]:ordinary(defs,0,1)),r=await apply(before,tier+kind);check(base+' positive '+tier+kind,r.applied);check(base+' 1/N ordinary '+tier+kind,r.assumptions.some(a=>a.id==='sapphire-uniform-candidates-v1'&&a.reason.includes('Actual game')));
  }
  for(const a of ['ALCHEMY','ANNULMENT','DIVINE'])check(base+' positive '+a,(await apply(state(a==='ALCHEMY'?'NORMAL':'RARE',a==='ALCHEMY'?[]:ordinary(defs,0,1)),a)).applied);
  let supported=new Set();
  for(const row of registry.entries.filter(r=>r.category==='LIQUID_EMOTION'&&r.action)){
   const action=row.action,targets=Object.values(defs).filter(d=>d.tags.includes('crafted')&&d.sourceUrl===row.ruleSource);
   for(const before of [state('NORMAL'),state('MAGIC',ordinary(defs,0,1)),state('RARE')]){const r=await apply(before,action);check(base+' Rare removable required '+action,!r.applied&&r.events.length===0&&r.consumedOmens.length===0&&util.isDeepStrictEqual(r.state,before));}
   if(!targets.length){const r=await apply(full,action,['Omen_of_Whittling']);check(base+' cross-category/source refusal '+action,!r.applied&&r.events.length===0&&r.consumedOmens.length===0&&util.isDeepStrictEqual(r.state,full));continue}
   supported.add(action);const outcomes=new Set();
   for(const before of [full,state('RARE',ordinary(defs,1,0))])for(let n=0;n<15;n++){
    const r=await apply(before,action,['Omen_of_Sinistral_Crystallisation']);check(base+' positive '+action,r.applied&&r.state.explicits.length===before.explicits.length&&r.events.length===2&&r.consumedOmens.length===0);
    const added=r.events[1],removed=r.events[0];outcomes.add(added.modifierId);check(base+' source target '+action,targets.some(t=>t.id===added.modifierId));
    check(base+' untouched canonical rolls '+action,r.state.explicits.filter(m=>m.modifierId!==added.modifierId).every(m=>before.explicits.some(old=>util.isDeepStrictEqual(old,m))));
    const rest=before.explicits.filter(m=>m.modifierId!==removed.modifierId),legal=targets.filter(d=>rest.filter(m=>defs[m.modifierId].affixType===d.affixType).length<capacity({...before,explicits:rest},d.affixType)&&!rest.some(m=>defs[m.modifierId].familyIds.some(f=>d.familyIds.includes(f))));
    check(base+' valid outcome probability '+action,Math.abs(added.selectionProbability-1/legal.length)<1e-12);
    check(base+' weight disclosure '+action,r.assumptions.some(a=>a.id==='uniform-liquid-outcomes-v1'&&a.n===legal.length&&a.reason.includes('Actual outcome weights')));
    const repeat=await apply(r.state,action);check(base+' Crafted blocks repeat '+action,!repeat.applied&&util.isDeepStrictEqual(repeat.state,r.state));
   }
   if(action==='ANCIENT_POTENT_LIQUID_CONTEMPT')check(base+' both Contempt directions',outcomes.size===2);
   for(const target of targets){const conflict=Object.values(defs).find(d=>d.weight>0&&d.familyIds.some(f=>target.familyIds.includes(f)));if(conflict){const side=conflict.affixType==='PREFIX';const before=state('RARE',[conflict.id,...ordinary(defs,side?0:1,side?1:0,conflict.familyIds)]);const r=await apply(before,action);check(base+' family conflict forced removal '+target.id,r.applied&&r.events[0].modifierId===conflict.id&&r.events[0].selectionProbability===1);}}
   if(action==='ANCIENT_POTENT_LIQUID_FEROCITY')for(const id of outcomes)check(base+' Ancient Ferocity exact resistance',defs[id].text.includes('Notable Passive Skills in Radius')&&defs[id].text.includes('Resistance')&&!id.endsWith('Effect'));
   if(action==='ANCIENT_POTENT_LIQUID_MELANCHOLY'){
    const radius=Object.values(defs).find(d=>d.weight>0&&d.familyIds.includes('JewelRadiusLargerRadius'));
    const before=state('RARE',[radius.id,...ordinary(defs,1,2,radius.familyIds)]),r=await apply(before,action);
    check(base+' radius family forces removal',r.applied&&r.events[0].modifierId===radius.id&&r.events[0].selectionProbability===1&&r.events[1].values.local_jewel_effect_base_radius===500);
   }
   if(!action.includes('POTENT')&&!action.endsWith('ISOLATION'))check(base+' passive conditional scope '+action,targets.every(d=>/Passive Skills in Radius/.test(d.text)));
  }
  check(base+' sourced Liquid count',supported.size===(base.endsWith('diamond')?3:13));
  for(const d of Object.values(defs).filter(d=>d.id.includes(':crafted:CraftedJewelAdditional'))){const p=d.id.endsWith('PrefixAllowed');const ids=ordinary(defs,p?3:1,p?1:3,d.familyIds),overflow=state('RARE',ids),expanded=state('RARE',[d.id,...ids]);
   valid(expanded);const divine=await apply(overflow,'DIVINE');check(base+' overflow Divine preserves shape',divine.applied&&divine.state.explicits.length===4);
   const refuse=await apply(overflow,'EXALTED');check(base+' overflow insertion no spend',!refuse.applied&&util.isDeepStrictEqual(refuse.state,overflow));
   const reapply=await apply(overflow,'ANCIENT_POTENT_LIQUID_CONTEMPT');check(base+' overflow Contempt reapply',reapply.applied);
   let removedCrafted=false;for(let n=0;n<40&&!removedCrafted;n++){const r=await apply(expanded,'ANNULMENT');if(r.events[0].modifierId===d.id){removedCrafted=true;check(base+' removed Contempt keeps overflow',r.state.explicits.length===4&&util.isDeepStrictEqual(r.state.explicits,overflow.explicits));}}check(base+' actual crafted removal',removedCrafted);
  }
  for(const a of ['CATALYST_FLESH','REFINED_CATALYST_FLESH','ARTIFICER','FRACTURING','ESSENCE_HYSTERIA']){const r=await apply(full,a);check(base+' excluded no spend '+a,!r.applied&&r.events.length===0&&util.isDeepStrictEqual(r.state,full));}
  const omen=await apply(full,'CHAOS',['Omen_of_Sinistral_Erasure','Omen_of_Whittling']);check(base+' prior Omen composition',omen.applied&&omen.consumedOmens.length===2&&defs[omen.events[0].modifierId].affixType==='PREFIX');
  const crafted=Object.values(defs).filter(d=>d.tags.includes('crafted'));await post('/apply',{state:state('RARE',crafted.slice(0,2).map(d=>d.id)),action:'DIVINE'},422);
 }
 fs.writeFileSync('/evidence/api-results.json',JSON.stringify({checks,captures,initials},null,2));console.log('Ancient API checks',checks.length,'captures',captures.length);
})().catch(e=>{let n=1;while(fs.existsSync('/evidence/ancient-api-failure-'+n+'.json'))n++;fs.writeFileSync('/evidence/ancient-api-failure-'+n+'.json',JSON.stringify({error:e.stack,checks,captures},null,2));console.error(e);process.exit(1)});
