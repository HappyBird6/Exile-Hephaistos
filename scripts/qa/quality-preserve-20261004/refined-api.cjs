const fs=require('fs'),assert=require('assert/strict'),{isDeepStrictEqual:same}=require('util');
const api='http://host.docker.internal:18780/api/v1/crafting/workbench',checks=[];let last;
const check=(n,v)=>{assert(v,n);checks.push(n)};
async function post(path,body,status=200){const r=await fetch(api+path,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({activeOmens:[],...body})});const raw=await r.text();last={path,body,status:r.status,raw};assert.equal(r.status,status);return JSON.parse(raw)}
const root=i=>{const s={...i.state,explicits:[],augmentSockets:null,catalystQuality:null};delete s.modifierIds;return s};
(async()=>{
 const initial=await(await fetch(api+'/initial?base=sapphire&itemLevel=82')).json(),sapphire=root(initial);
 check('source rarity Magic',sapphire.rarity==='MAGIC');check('source metadata Sapphire',sapphire.baseItemId==='Metadata/Items/Jewels/JewelInt');
 check('generation weights absent',Object.values(initial.modifiers).every(d=>d.weight===0));check('initial general crafting blocked',initial.actions.every(a=>!a.available));
 const types=['FLESH','NEURAL','CARAPACE','UUL_NETOL','XOPH','TUL','ESH','CHAYULA','REAVER','SIBILANT','SKITTERING','ADAPTIVE','NECROTIC'];
 const suffix={modifierId:'sapphire:suffix:of-enchanting',values:{display_cast_speed_percent:3},fractured:false};
 for(const rarity of ['MAGIC','RARE'])for(const affixes of [[],[suffix]]){
  let state={...sapphire,rarity,explicits:affixes};
  for(const type of types){
   const action='REFINED_CATALYST_'+type,omen=['Omen_of_Sinistral_Exaltation'];
   const result=await post('/apply',{state,action,activeOmens:omen});
   check(`${rarity}/${affixes.length}/${type} applied`,result.applied);
   check(`${rarity}/${affixes.length}/${type} max20 and replaces`,same(result.state.catalystQuality,{type,amount:20})&&result.qualityLimit.maximumQuality===20);
   check(`${rarity}/${affixes.length}/${type} source preserved`,same(result.state.explicits,affixes)&&result.state.rarity===rarity);
   check(`${rarity}/${affixes.length}/${type} no random or omen consumption`,!result.events.length&&!result.assumptions.length&&!result.consumedOmens.length&&same(result.remainingOmens,omen));
   const display=await post('/quality-display',{state:result.state});
   if(affixes.length){const d=display.modifiers[0],matches=['SIBILANT','SKITTERING'].includes(type);check(`${rarity}/${type} actual match or no match`,d.status===(matches?'SCALED_INTEGER':'NO_MATCH')&&d.originalValues.display_cast_speed_percent===3&&d.displayedValues.display_cast_speed_percent===(matches?4:3));}
   else check(`${rarity}/${type} empty has no fabricated affix`,display.modifiers.length===0);
   const repeat=await post('/apply',{state:result.state,action});check(`${rarity}/${affixes.length}/${type} idempotent`,same(repeat.state,result.state));
   const ordinary=await post('/apply',{state:result.state,action:'CATALYST_'+type});check(`${rarity}/${affixes.length}/${type} ordinary refused`,!ordinary.applied&&same(ordinary.state,result.state));
   state=result.state;
  }
  const actions=await post('/actions',{state});check(`${rarity}/${affixes.length} only refined available`,actions.filter(a=>a.available).length===13&&actions.filter(a=>a.available).every(a=>a.action.startsWith('REFINED_')));
  for(const action of ['ANNULMENT','CHAOS','DIVINE','ALCHEMY','REGAL','EXALTED','ESSENCE_BREACH']){const r=await post('/apply',{state,action});check(`${rarity}/${affixes.length} ${action} blocked`,!r.applied&&same(r.state,state));}
 }
 for(const bad of [{...sapphire,rarity:'NORMAL'},{...sapphire,rarity:'UNIQUE'},{...sapphire,conditions:['CORRUPTED']},{...sapphire,catalystQuality:{type:'FLESH',amount:21}},{...sapphire,explicits:[{...suffix,values:{display_cast_speed_percent:5}}]}]){const r=await post('/apply',{state:bad,action:'REFINED_CATALYST_FLESH'},422);check('invalid Jewel has Problem Details',r.status===422&&!JSON.stringify(r).includes('Exception'));}
 const solarInitial=await(await fetch(api+'/initial?base=solar&itemLevel=82')).json(),solar=root(solarInitial);
 const ring=root(await(await fetch(api+'/initial?base=ring&itemLevel=82')).json());
 for(const type of types)for(const state of [solar,ring]){const r=await post('/apply',{state,action:'REFINED_CATALYST_'+type});check(type+' refined rejects Ring/Amulet '+state.baseItemId,!r.applied&&same(r.state,state));}
 const breach={modifierId:'amulet:prefix:essence-maximum-quality',values:{'local_maximum_quality_+':20},fractured:false},life={modifierId:'amulet:prefix:healthy',values:{base_maximum_life:29},fractured:false},attr={modifierId:'amulet:suffix:of-the-wrestler',values:{additional_strength:9},fractured:false};
 const cap={...solar,rarity:'RARE',explicits:[breach,attr],catalystQuality:{type:'FLESH',amount:40}};
 const suffixOnly=await post('/apply',{state:cap,action:'ANNULMENT',activeOmens:['Omen_of_Dextral_Annulment']});
 check('suffix filter keeps cap40',suffixOnly.applied&&suffixOnly.state.catalystQuality.amount===40&&same(suffixOnly.state.explicits,[breach]));check('suffix filter no clamp ledger',!suffixOnly.assumptions.some(a=>a.id==='unverified-quality-cap-clamp-v1'));
 const prefixOnly=await post('/apply',{state:cap,action:'ANNULMENT',activeOmens:['Omen_of_Sinistral_Annulment']});
 check('prefix filter preserved40 after cap20 type retained',prefixOnly.applied&&same(prefixOnly.state.catalystQuality,{type:'FLESH',amount:40})&&same(prefixOnly.state.explicits,[attr]));
 check('prefix removal original probability',prefixOnly.events[0].selectionProbability===1);check('no obsolete clamp ledger',!prefixOnly.assumptions.some(a=>a.id==='unverified-quality-cap-clamp-v1'));
 for(const action of ['CHAOS','GREATER_CHAOS','PERFECT_CHAOS']){const r=await post('/apply',{state:cap,action,activeOmens:['Omen_of_Sinistral_Erasure','Omen_of_Whittling']});check(action+' side-first cap removal preserves',r.applied&&r.state.catalystQuality.amount===40&&r.events[0].modifierId===breach.modifierId&&r.events[0].selectionProbability===1&&r.consumedOmens.length===2);check(action+' survives original suffix',r.state.explicits.some(m=>same(m,attr)));}
 for(const action of types.map(t=>'CATALYST_'+t)) { const r=await post('/apply',{state:prefixOnly.state,action});check(action+' preserves inherited40',r.applied&&r.state.catalystQuality.amount===40&&r.qualityLimit.maximumQuality===20); }
 for(const amount of [41,100,-1,1.5]) {const r=await post('/apply',{state:{...prefixOnly.state,catalystQuality:{type:'FLESH',amount}},action:'CATALYST_FLESH'},422);check('unreachable or malformed Solar quality '+amount+' rejected',r.status===422&&!JSON.stringify(r).includes('Exception'));}
 const inheritedLife={...solar,rarity:'RARE',explicits:[life],catalystQuality:{type:'FLESH',amount:40}};const projected=await post('/quality-display',{state:inheritedLife}),lifeProjection=projected.modifiers.find(m=>m.modifierId===life.modifierId);check('inherited40 projection preserves original29 and displays41',lifeProjection.originalValues.base_maximum_life===29&&lifeProjection.displayedValues.base_maximum_life===41);
 const both=await post('/apply',{state:cap,action:'ANNULMENT'});check('cap remains unfiltered removal candidate',both.applied&&both.events[0].selectionProbability===0.5&&both.assumptions.some(a=>a.id==='uniform-removal-v1'&&a.n===2&&a.candidates.includes(breach.modifierId)));
 check('before state remains quality40',cap.catalystQuality.amount===40&&cap.explicits.length===2);
 const growth=await post('/apply',{state:{...solar,rarity:'RARE',explicits:[life],catalystQuality:{type:'FLESH',amount:20}},action:'ESSENCE_BREACH'});
 check('larger cap does not refill',growth.applied&&growth.qualityLimit.maximumQuality===40&&growth.state.catalystQuality.amount===20);
 const refill=await post('/apply',{state:growth.state,action:'CATALYST_FLESH'});check('next catalyst sets new max40',refill.applied&&refill.state.catalystQuality.amount===40);
 fs.writeFileSync('/evidence/refined-api-results.json',JSON.stringify({checks,initial,sapphire,rootWithSuffix:{...sapphire,explicits:[suffix]},cap,prefixOnly,suffixOnly,growth},null,2));console.log('PASS '+checks.length+' refined/cap API assertions');
})().catch(e=>{fs.writeFileSync('/evidence/refined-api-failure.json',JSON.stringify({error:e.message,checks,last},null,2));console.error(e);process.exit(1)});
