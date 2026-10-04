const fs=require('fs'),assert=require('assert/strict'),{isDeepStrictEqual:same}=require('util');
const api='http://host.docker.internal:18980/api/v1/crafting/workbench';
const types=['FLESH','NEURAL','CARAPACE','UUL_NETOL','XOPH','TUL','ESH','CHAYULA','REAVER','SIBILANT','SKITTERING','ADAPTIVE','NECROTIC'];
const checks=[],fixtures={};let last;
const check=(name,ok)=>{assert(ok,name);checks.push(name)};
async function post(path,body){const response=await fetch(api+path,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});const raw=await response.text();last={path,body,status:response.status,raw};check(path+' HTTP200',response.status===200);return JSON.parse(raw)}
function state(initial){const s={...initial.state,explicits:[]};delete s.modifierIds;if(initial.augmentSockets!==undefined)s.augmentSockets=initial.augmentSockets;return s}
(async()=>{
 for(const base of ['solar','ring','stocky','bow','wand','body','helmet','belt','sceptre']){
  const r=await fetch(api+'/initial?itemLevel=82&base='+base);assert(r.ok);const initial=await r.json();fixtures[base]={initial,root:state(initial)};
  for(const type of types){
   const root=fixtures[base].root,omens=['Omen_of_Sinistral_Exaltation'];
   const result=await post('/apply',{state:root,action:'CATALYST_'+type,activeOmens:omens});
   const supported=['solar','ring'].includes(base);
   check(base+' '+type+' genuine applicability',result.applied===supported);
   check(base+' '+type+' preserves original rolls',same(result.state.implicits,root.implicits)&&same(result.state.explicits,root.explicits));
   check(base+' '+type+' leaves Omens untouched',result.consumedOmens.length===0&&same(result.remainingOmens,omens));
   check(base+' '+type+' no random ledgers',result.events.length===0&&result.assumptions.length===0);
   if(supported){
    check(base+' '+type+' max policy',same(result.state.catalystQuality,{type,amount:20})&&result.reason.includes('Simulator policy'));
    const again=await post('/apply',{state:result.state,action:'CATALYST_'+type,activeOmens:[]});
    check(base+' '+type+' repeat idempotent',same(again.state,result.state));
    const switched=await post('/apply',{state:again.state,action:'CATALYST_'+(type==='FLESH'?'NEURAL':'FLESH'),activeOmens:[]});
    check(base+' '+type+' replacement resets only type',switched.state.catalystQuality.amount===20&&same(switched.state.explicits,root.explicits));
   }
   const refined=await post('/apply',{state:result.state,action:'REFINED_CATALYST_'+type,activeOmens:omens});
   check(base+' refined '+type+' never bypasses Jewel restriction',!refined.applied&&same(refined.state,result.state)&&refined.reason.includes('Jewel'));
  }
 }
 const solar=fixtures.solar.root,life={modifierId:'amulet:prefix:healthy',values:{base_maximum_life:29},fractured:false};
 let rare={...solar,rarity:'RARE',explicits:[life]};
 for(const type of ['FLESH','NEURAL','FLESH','CARAPACE','FLESH']){
  const result=await post('/apply',{state:rare,action:'CATALYST_'+type});rare=result.state;
  const display=await post('/quality-display',{state:rare});const projection=display.modifiers.find(m=>m.modifierId===life.modifierId);
  check(type+' canonical29 display once',projection.originalValues.base_maximum_life===29&&projection.displayedValues.base_maximum_life===(type==='FLESH'?35:29));
 }
 const breach={modifierId:'amulet:prefix:essence-maximum-quality',values:{'local_maximum_quality_+':20},fractured:false};
 const cap=await post('/apply',{state:{...rare,explicits:[life,breach].sort((a,b)=>a.modifierId.localeCompare(b.modifierId))},action:'CATALYST_FLESH'});
 check('source-backed custom cap40',cap.state.catalystQuality.amount===40&&cap.qualityLimit.maximumQuality===40);
 const display=await post('/quality-display',{state:cap.state});
 check('cap is never scaled',display.modifiers.find(m=>m.modifierId===breach.modifierId).displayedValues['local_maximum_quality_+']===20);
 check('40percent half-up canonical29 becomes41',display.modifiers.find(m=>m.modifierId===life.modifierId).displayedValues.base_maximum_life===41);
 const removal=await post('/apply',{state:cap.state,action:'ANNULMENT'});check('cap-loss accepted with preserved quality',removal.applied&&removal.state.catalystQuality.amount===40);
 let typed={...solar,catalystQuality:{type:'FLESH',amount:20}};
 for(const action of ['TRANSMUTATION','AUGMENTATION','REGAL','EXALTED','GREATER_EXALTED','PERFECT_EXALTED','DIVINE','CHAOS','ANNULMENT']){
  const result=await post('/apply',{state:typed,action});check('typed '+action+' supported',result.applied);check('typed '+action+' retains quality',same(result.state.catalystQuality,typed.catalystQuality));typed=result.state;
 }
 const alchemy=await post('/apply',{state:{...solar,catalystQuality:{type:'FLESH',amount:20}},action:'ALCHEMY'});
 check('typed Alchemy supported',alchemy.applied&&alchemy.state.explicits.length===4);
 const fractured=await post('/apply',{state:alchemy.state,action:'FRACTURING'});check('typed Fracturing supported',fractured.applied&&fractured.state.explicits.filter(m=>m.fractured).length===1);
 const suffix=Object.values(fixtures.solar.initial.modifiers).find(d=>d.affixType==='SUFFIX'&&d.weight>0&&d.requiredItemLevel<=82&&d.stats.length===1);
 const magic={...solar,rarity:'MAGIC',catalystQuality:{type:'FLESH',amount:20},explicits:[{modifierId:suffix.id,values:{[suffix.stats[0].id]:suffix.stats[0].min},fractured:false}]};
 const essence=await post('/apply',{state:magic,action:'LESSER_ESSENCE_BODY'});check('typed basic Essence supported',essence.applied&&essence.state.catalystQuality.amount===20);
 fs.writeFileSync('/evidence/catalyst-api-results.json',JSON.stringify({checks,fixtures,cap:cap.state,typed:fractured.state},null,2));console.log('PASS '+checks.length+' catalyst API assertions');
})().catch(e=>{fs.writeFileSync('/evidence/catalyst-api-failure.json',JSON.stringify({error:e.message,checks,last},null,2));console.error(e);process.exit(1)});
