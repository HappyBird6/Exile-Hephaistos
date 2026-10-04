const fs = require('fs'), assert = require('assert/strict');
const same=require('util').isDeepStrictEqual;
const api='http://host.docker.internal:18780/api/v1/crafting/workbench';
const checks=[];
function check(name,value){assert(value,name);checks.push(name)}
async function post(path,body,status=200){const r=await fetch(api+path,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({activeOmens:[],...body})});assert.equal(r.status,status);return r.json()}
(async()=>{
const initial=await(await fetch(api+'/initial?base=sapphire')).json();
const suffix={modifierId:'sapphire:suffix:of-enchanting',values:{display_cast_speed_percent:3},fractured:false};
const root={...initial.state,implicits:[],explicits:[suffix],augmentSockets:null,catalystQuality:{type:'SIBILANT',amount:20}};delete root.modifierIds;
for(const rarity of ['MAGIC','RARE']){
 const state={...root,rarity};
 const actions=await post('/actions',{state});
 check(rarity+'15 positive actions',actions.filter(a=>a.available).length===15);
 check(rarity+'positive support bounded',actions.filter(a=>a.available).every(a=>a.action.startsWith('REFINED_')||['DIVINE','ANNULMENT'].includes(a.action)));
 for(const action of ['DIVINE','ANNULMENT']){
  const r=await post('/apply',{state,action});check(rarity+action+'applied',r.applied);
  check(rarity+action+'quality preserved',same(r.state.catalystQuality,state.catalystQuality));
  check(rarity+action+'rarity preserved',r.state.rarity===rarity);
  check(rarity+action+'source stable',r.state.snapshotId===root.snapshotId);
  if(action==='ANNULMENT'){
   check(rarity+'removed source suffix',r.state.explicits.length===0&&r.events[0].modifierId===suffix.modifierId&&r.events[0].selectionProbability===1);
   const repeat=await post('/apply',{state:r.state,action});check(rarity+'repeat refused',!repeat.applied&&same(repeat.state,r.state));
  }else{
   check(rarity+'source range reroll',r.state.explicits.length===1&&r.state.explicits[0].modifierId===suffix.modifierId&&[2,3,4].includes(r.state.explicits[0].values.display_cast_speed_percent));
   check(rarity+'roll model disclosed',r.assumptions.some(a=>a.n===3&&a.min===2&&a.max===4));
  }
 }
 for(const a of actions.filter(a=>!a.available)){
  const r=await post('/apply',{state,action:a.action});check(rarity+a.action+'registered but refused',!r.applied&&same(r.state,state)&&r.events.length===0);
 }
 for(const [action,omen] of [['ANNULMENT','Omen_of_Sinistral_Annulment'],['DIVINE','Omen_of_the_Blessed']]){
  const r=await post('/apply',{state,action,activeOmens:[omen]});check(rarity+omen+'refusal preserves omen',!r.applied&&r.consumedOmens.length===0&&r.remainingOmens.includes(omen));
 }
}
for(const bad of [{...root,rarity:'NORMAL'},{...root,rarity:'UNIQUE'},{...root,explicits:[{...suffix,values:{display_cast_speed_percent:5}}]},{...root,explicits:[suffix,suffix]},{...root,explicits:[{...suffix,crafted:true}]}]){
 const r=await post('/apply',{state:bad,action:'DIVINE'},422);check('invalid state Problem Details',r.status===422);
}
fs.writeFileSync('/evidence/api-results.json',JSON.stringify({passed:checks.length,checks,root},null,2));console.log('PASS '+checks.length);
})().catch(e=>{fs.writeFileSync('/evidence/api-failure.json',JSON.stringify({error:e.stack,checks},null,2));console.error(e);process.exit(1)});
