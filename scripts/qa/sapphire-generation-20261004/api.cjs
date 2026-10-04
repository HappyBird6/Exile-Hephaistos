const fs=require('fs'),assert=require('assert/strict');
const api='http://host.docker.internal:18780/api/v1/crafting/workbench',checks=[],captures=[];
function check(n,v){assert(v,n);checks.push(n)}
async function post(path,body,status=200){const response=await fetch(api+path,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({activeOmens:[],...body})});assert.equal(response.status,status,path);return response.json()}
(async()=>{
const initial=await(await fetch(api+'/initial?base=sapphire')).json(),definitions=initial.modifiers;
const state=(rarity='NORMAL',ids=[])=>{const s={...initial.state,rarity,implicits:[],explicits:ids.map(id=>({modifierId:id,values:Object.fromEntries(definitions[id].stats.map(s=>[s.id,s.min])),fractured:false})).sort((a,b)=>a.modifierId.localeCompare(b.modifierId)),augmentSockets:null,catalystQuality:{type:'SIBILANT',amount:20}};delete s.modifierIds;return s};
check('ordinary58',Object.values(definitions).filter(d=>d.weight>0).length===58);
check('prefix23',Object.values(definitions).filter(d=>d.weight>0&&d.affixType==='PREFIX').length===23);
check('crafted10zero',Object.values(definitions).filter(d=>d.tags.includes('crafted')).length===10&&Object.values(definitions).filter(d=>d.tags.includes('crafted')).every(d=>d.weight===0));
function valid(s){let p=0,suf=0,c=0,f=new Set();for(const m of s.explicits){let d=definitions[m.modifierId];check('definition '+m.modifierId,!!d);check('family '+m.modifierId,d.familyIds.every(id=>!f.has(id)));d.familyIds.forEach(id=>f.add(id));d.affixType==='PREFIX'?p++:suf++;if(d.tags.includes('crafted'))c++;for(const r of d.stats)check('bounds '+m.modifierId,Number.isInteger(m.values[r.id])&&m.values[r.id]>=r.min&&m.values[r.id]<=r.max)}let cap=s.rarity==='NORMAL'?0:s.rarity==='MAGIC'?1:2;check('capacity',p<=cap&&suf<=cap&&c<=1);check('quality preserved',s.catalystQuality.type==='SIBILANT'&&s.catalystQuality.amount===20)}
async function apply(before,action,activeOmens=[]){let result=await post('/apply',{state:before,action,activeOmens});captures.push({before,action,activeOmens,result});if(result.applied)valid(result.state);return result}
for(const tier of ['', 'GREATER_','PERFECT_'])for(const kind of ['TRANSMUTATION','AUGMENTATION','REGAL','EXALTED','CHAOS']){
 let rarity=kind==='TRANSMUTATION'?'NORMAL':['AUGMENTATION','REGAL'].includes(kind)?'MAGIC':'RARE';let before=state(rarity,rarity==='NORMAL'?[]:['sapphire:suffix:of-enchanting']);let result=await apply(before,tier+kind);check('positive '+tier+kind,result.applied);check('ordinary added '+tier+kind,result.events.filter(e=>e.kind==='ADD').every(e=>definitions[e.modifierId].weight===1));check('disclosed model '+tier+kind,result.assumptions.some(a=>a.id==='sapphire-uniform-candidates-v1'&&a.reason.includes('Actual game')));
}
for(const action of ['ALCHEMY','ANNULMENT','DIVINE']){let before=state(action==='ALCHEMY'?'NORMAL':'RARE',action==='ALCHEMY'?[]:['sapphire:suffix:of-enchanting']);check('positive '+action,(await apply(before,action)).applied)}
let chain=state();for(const action of ['TRANSMUTATION','AUGMENTATION','REGAL','EXALTED']){let result=await apply(chain,action);check('chain '+action,result.applied);chain=result.state}check('rare four',chain.explicits.length===4);let noSlot=await apply(chain,'EXALTED',['Omen_of_Sinistral_Exaltation']);check('full unchanged',!noSlot.applied&&require('util').isDeepStrictEqual(noSlot.state,chain)&&noSlot.consumedOmens.length===0);
const full=state('RARE',['sapphire:prefix:shimmering','sapphire:prefix:chilling','sapphire:suffix:of-enchanting','sapphire:suffix:of-unmaking']);
for(const [action,before,omens] of [
 ['ALCHEMY',state(),['Omen_of_Sinistral_Alchemy']],
 ['ALCHEMY',state(),['Omen_of_Dextral_Alchemy']],
 ['REGAL',state('MAGIC',['sapphire:suffix:of-enchanting']),['Omen_of_Sinistral_Coronation']],
 ['REGAL',state('MAGIC',['sapphire:suffix:of-enchanting']),['Omen_of_Dextral_Coronation']],
 ['EXALTED',state('RARE',['sapphire:suffix:of-enchanting','sapphire:suffix:of-unmaking']),['Omen_of_Greater_Exaltation']],
 ['EXALTED',state('RARE',['sapphire:suffix:of-enchanting']),['Omen_of_Homogenising_Exaltation']],
 ['CHAOS',full,['Omen_of_Sinistral_Erasure','Omen_of_Whittling']],
]){const result=await apply(before,action,omens);check('Sapphire Omen positive '+omens.join('+'),result.applied);check('Sapphire Omen consumed '+omens.join('+'),result.consumedOmens.length===omens.length)}
const registry=await(await fetch(api+'/registry')).json(),liquids=registry.entries.filter(e=>e.category==='LIQUID_EMOTION'&&e.action);
check('ten implemented liquids',liquids.length===10);
for(const r of liquids){let result=await apply(full,r.action,['Omen_of_Sinistral_Crystallisation']);check('Liquid positive '+r.id,result.applied);check('two events '+r.id,result.events.length===2&&result.events[0].kind==='REMOVE'&&result.events[1].kind==='ADD');check('Crafted '+r.id,definitions[result.events[1].modifierId].tags.includes('crafted'));check('unrelated omen preserved '+r.id,result.consumedOmens.length===0&&result.remainingOmens.includes('Omen_of_Sinistral_Crystallisation'));check('legal removal disclosure '+r.id,result.assumptions.some(a=>a.id==='uniform-removal-v1'&&a.reason.includes('Failed branches')));let repeat=await apply(result.state,r.action);check('second Liquid refuses unchanged '+r.id,!repeat.applied&&require('util').isDeepStrictEqual(repeat.state,result.state));let only=state('RARE',[result.events[1].modifierId]);let annul=await apply(only,'ANNULMENT');check('Crafted removable '+r.id,annul.applied&&annul.state.explicits.length===0);if(definitions[result.events[1].modifierId].stats.some(s=>s.min!==s.max)){let divine=await apply(result.state,'DIVINE');check('Crafted survives Divine '+r.id,divine.applied&&divine.state.explicits.some(m=>m.modifierId===result.events[1].modifierId))}let chaos=await apply(only,'CHAOS');check('Crafted Chaos ordinary replacement '+r.id,chaos.applied&&chaos.state.explicits.every(m=>!definitions[m.modifierId].tags.includes('crafted')));let magic=await apply(state('MAGIC',['sapphire:suffix:of-enchanting']),r.action);check('Magic Liquid refused '+r.id,!magic.applied)}
const invalid=state('RARE',['sapphire:prefix:bestial','sapphire:prefix:overgrown']);let error=await post('/apply',{state:invalid,action:'EXALTED'},422);check('Problem Details',!!error.type&&error.status===422);
await post('/apply',{state:{...full,explicits:[...full.explicits,{...full.explicits[0],modifierId:'sapphire:unknown'}]},action:'EXALTED'},422);
await post('/apply',{state:state('RARE',['sapphire:crafted:JewelColdDamage','sapphire:crafted:JewelCastSpeed']),action:'DIVINE'},422);
for(const action of ['ESSENCE_HYSTERIA','FRACTURING','CATALYST_FLESH']){let result=await apply(full,action);check('excluded '+action,!result.applied&&result.consumedOmens.length===0)}
for(const action of ['POTENT_LIQUID_CONTEMPT','ANCIENT_LIQUID_ENVY'])await post('/apply',{state:full,action},400);
fs.writeFileSync('/evidence/api-results.json',JSON.stringify({checks:checks.length,positiveBasic18:true,positiveLiquid10:true,captures},null,2));console.log(JSON.stringify({checks:checks.length,captures:captures.length}));
})().catch(e=>{fs.writeFileSync('/evidence/api-failure.json',JSON.stringify({error:String(e),checks,captures},null,2));console.error(e);process.exit(1)});
