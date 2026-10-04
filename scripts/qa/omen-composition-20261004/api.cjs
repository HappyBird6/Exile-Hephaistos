const fs = require('fs');
const assert = require('assert/strict');
const { isDeepStrictEqual: same } = require('util');
const api = 'http://host.docker.internal:18280/api/v1/crafting/workbench';
const checks = [], fixtures = {};
let lastExchange=null,lastCheck=null;
const pair = ['Omen_of_Sinistral_Erasure','Omen_of_Whittling'];
const check = (name, ok) => { lastCheck=name;assert(ok, name); checks.push(name); };
const inst = d => ({modifierId:d.id, values:Object.fromEntries(d.stats.map(s=>[s.id,s.min])), fractured:false});
const overlap = (a,b) => a.familyIds.some(f=>b.familyIds.includes(f));
async function post(path, body) {
  const r=await fetch(api+path,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
  const raw=await r.text();lastExchange={path,request:body,status:r.status,rawResponse:raw};
  assert.equal(r.status,200,path);const response=JSON.parse(raw);lastExchange.response=response;return response;
}
// Initial.state is StateBucket; apply.state is ItemState. Keep every field, roll and flag.
// Only the two optional ItemState fields and existing canonical list order are normalized.
function canonicalInput(state) {
  const byId=(a,b)=>a.modifierId<b.modifierId?-1:a.modifierId>b.modifierId?1:0;
  return {...state,augmentSockets:state.augmentSockets??null,catalystQuality:state.catalystQuality??null,
    implicits:[...state.implicits].sort(byId),explicits:[...state.explicits].sort(byId),conditions:[...state.conditions].sort()};
}
function pool(state,defs,action,side=null,tags=null) {
  let p=Object.values(defs).filter(d=>d.layer==='EXPLICIT'&&d.weight>0&&d.requiredItemLevel<=state.itemLevel
    && (!side||d.affixType===side)
    && !state.explicits.some(m=>overlap(defs[m.modifierId],d))
    && state.explicits.filter(m=>defs[m.modifierId].affixType===d.affixType).length<3
    && (!tags||tags.size===0||d.tags.some(t=>tags.has(t))));
  const minimum=action.startsWith('GREATER_')?35:action.startsWith('PERFECT_')?50:0;
  if(minimum) {
    const max=new Map();for(const d of p){assert.equal(d.familyIds.length,1);const k=d.affixType+':'+d.familyIds[0];max.set(k,Math.max(max.get(k)||0,d.requiredItemLevel));}
    p=p.filter(d=>d.requiredItemLevel>=minimum||d.requiredItemLevel===max.get(d.affixType+':'+d.familyIds[0]));
  }return p;
}
async function apply(state,action,activeOmens){return post('/apply',{state,action,activeOmens});}
function atomic(before,r,ids,label){check(label+' atomic',!r.applied&&same(r.state,canonicalInput(before))&&r.events.length===0&&r.assumptions.length===0&&r.consumedOmens.length===0&&same([...r.remainingOmens].sort(),[...ids].sort()));}
function weighted(before,r,defs,action,side,tags,label){
  const candidates=pool({...before,rarity:'RARE'},defs,action,side,tags), e=r.events.find(e=>e.kind==='ADD'),chosen=candidates.find(d=>d.id===e?.modifierId);
  check(label+' eligible weighted addition',r.applied&&chosen&&e.selectionProbability===chosen.weight/candidates.reduce((n,d)=>n+d.weight,0));
}
(async()=>{
for(const base of ['solar','stocky','bow','wand','body','sceptre','belt','helmet','ring']) {
  const response=await fetch(api+'/initial?base='+base+'&itemLevel=82');assert.equal(response.status,200);
  const initial=await response.json(),defs=initial.modifiers;
  const root=canonicalInput({...initial.state,explicits:[],augmentSockets:initial.augmentSockets??null});delete root.modifierIds;
  const ordinary=Object.values(defs).filter(d=>d.layer==='EXPLICIT'&&d.weight>0&&d.requiredItemLevel<=82);
  const s=ordinary.filter(d=>d.affixType==='SUFFIX').sort((a,b)=>a.requiredItemLevel-b.requiredItemLevel)[0];assert(s);
  const p=ordinary.find(d=>d.affixType==='PREFIX'&&d.requiredItemLevel>s.requiredItemLevel&&!overlap(d,s));assert(p,base+' prefix above suffix');
  const p2=ordinary.find(d=>d.affixType==='PREFIX'&&d.requiredItemLevel===p.requiredItemLevel&&!overlap(d,p)&&!overlap(d,s));
  const rare={...root,rarity:'RARE',explicits:[inst(p),...(p2?[inst(p2)]:[]),inst(s)]};
  const candidates=rare.explicits.filter(m=>defs[m.modifierId].affixType==='PREFIX').map(m=>m.modifierId);
  fixtures[base]={initial,rare,candidates};
  for(const action of ['CHAOS','GREATER_CHAOS','PERFECT_CHAOS']) {
    const r=await apply(rare,action,[...pair,'Omen_of_the_Blessed']);
    const removal=r.events.find(e=>e.kind==='REMOVE');
    check(base+' '+action+' side-before-level/ties',r.applied&&candidates.includes(removal.modifierId)&&removal.selectionProbability===1/candidates.length);
    const ledger=r.assumptions.find(a=>a.id==='uniform-removal-v1');
    check(base+' '+action+' exact candidate ledger',ledger.n===candidates.length&&same([...ledger.candidates].sort(),[...candidates].sort()));
    check(base+' '+action+' both consumed, unrelated retained',same([...r.consumedOmens].sort(),[...pair].sort())&&same(r.remainingOmens,['Omen_of_the_Blessed']));
    check(base+' '+action+' implicit + suffix unchanged',same(r.state.implicits,rare.implicits)&&same(r.state.explicits.find(m=>m.modifierId===s.id),inst(s)));
    const afterRemoval={...rare,explicits:rare.explicits.filter(m=>m.modifierId!==removal.modifierId)};
    weighted(afterRemoval,r,defs,action,null,null,base+' '+action);
  }
  const dexPair=['Omen_of_Dextral_Erasure','Omen_of_Whittling'];
  for(const action of ['CHAOS','GREATER_CHAOS','PERFECT_CHAOS']) {
    const r=await apply(rare,action,dexPair),removal=r.events.find(e=>e.kind==='REMOVE');
    check(base+' '+action+' symmetric Dextral',r.applied&&removal.modifierId===s.id&&removal.selectionProbability===1&&r.consumedOmens.length===2);
  }
  const conflict=[...pair,'Omen_of_Dextral_Erasure'];
  atomic(rare,await apply(rare,'CHAOS',conflict),conflict,base+' opposing sides');
  const fractured={...rare,explicits:rare.explicits.map(m=>({...m,fractured:m.modifierId===s.id}))};
  atomic(fractured,await apply(fractured,'CHAOS',pair),pair,base+' unresolved fracture');
  const noPrefix={...root,rarity:'RARE',explicits:[inst(s)]};
  atomic(noPrefix,await apply(noPrefix,'PERFECT_CHAOS',pair),pair,base+' absent prefix');
  for(const id of ['Omen_of_Sinistral_Alchemy','Omen_of_Dextral_Alchemy']) {
    const r=await apply(root,'ALCHEMY',[id,'Omen_of_the_Blessed']);
    const side=id.includes('Sinistral')?'PREFIX':'SUFFIX';
    check(base+' '+id+' max side4 outcome',r.applied&&r.state.explicits.length===4&&r.state.explicits.filter(m=>defs[m.modifierId].affixType===side).length===3);
    check(base+' '+id+' explicit model ledger',r.assumptions.some(a=>a.id==='legacy-alchemy-order-v1'&&a.reason.includes('UNVERIFIED'))&&same(r.consumedOmens,[id])&&same(r.remainingOmens,['Omen_of_the_Blessed']));
  }
  const magic={...root,rarity:'MAGIC',explicits:[inst(s)]};
  for(const id of ['Omen_of_Sinistral_Coronation','Omen_of_Dextral_Coronation','Omen_of_Homogenising_Coronation']) {
    for(const action of ['REGAL','GREATER_REGAL','PERFECT_REGAL']) {
      const tags=id.includes('Homogenising')?new Set([...magic.implicits,...magic.explicits].flatMap(m=>defs[m.modifierId].tags)):null;
      const side=id.includes('Sinistral')?'PREFIX':id.includes('Dextral')?'SUFFIX':null;
      const r=await apply(magic,action,[id,'Omen_of_the_Blessed']);
      if(pool({...magic,rarity:'RARE'},defs,action,side,tags).length===0) atomic(magic,r,[id,'Omen_of_the_Blessed'],base+' no tag pool');
      else {weighted(magic,r,defs,action,side,tags,base+' '+id+' '+action);check(base+' Regal preserves rolls',same(r.state.explicits.find(m=>m.modifierId===s.id),inst(s))&&same(r.consumedOmens,[id]));}
    }
  }
  for(const id of ['Omen_of_Sinistral_Exaltation','Omen_of_Dextral_Exaltation','Omen_of_Homogenising_Exaltation']) {
    for(const action of ['EXALTED','GREATER_EXALTED','PERFECT_EXALTED']) {
      const before={...root,rarity:'RARE',explicits:[inst(s)]};
      const tags=id.includes('Homogenising')?new Set([...before.implicits,...before.explicits].flatMap(m=>defs[m.modifierId].tags)):null;
      const side=id.includes('Sinistral')?'PREFIX':id.includes('Dextral')?'SUFFIX':null;
      const r=await apply(before,action,[id]);
      if(pool(before,defs,action,side,tags).length===0) atomic(before,r,[id],base+' no tag pool');
      else weighted(before,r,defs,action,side,tags,base+' '+id+' '+action);
    }
  }
  const greater='Omen_of_Greater_Annulment',sinistral='Omen_of_Sinistral_Annulment';
  const r=await apply(rare,'ANNULMENT',[greater,'Omen_of_the_Blessed']);
  check(base+' Greater Annulment two distinct',r.applied&&r.events.length===2&&new Set(r.events.map(e=>e.modifierId)).size===2&&r.events[0].selectionProbability===1/rare.explicits.length&&r.events[1].selectionProbability===1/(rare.explicits.length-1));
  if(p2) {
    const r=await apply(rare,'ANNULMENT',[greater,sinistral,'Omen_of_the_Blessed']);
    check(base+' audited two-prefix combination',r.applied&&r.events.every(e=>candidates.includes(e.modifierId))&&r.state.explicits.length===1&&r.state.explicits[0].modifierId===s.id&&r.consumedOmens.length===2);
  }
  atomic(noPrefix,await apply(noPrefix,'ANNULMENT',[greater,sinistral]),[greater,sinistral],base+' two prefix unavailable');
  for(const action of ['EXALTED','GREATER_EXALTED','PERFECT_EXALTED']) {
    for(const ids of [['Omen_of_Greater_Exaltation'],['Omen_of_Greater_Exaltation','Omen_of_Homogenising_Exaltation']]) {
      const before={...root,rarity:'RARE',explicits:[inst(s)]};
      const tags=ids.length===2?new Set([...before.implicits,...before.explicits].flatMap(m=>defs[m.modifierId].tags)):null;
      const first=pool(before,defs,action,null,tags);
      const possible=first.length>0&&first.every(d=>pool({...before,explicits:[...before.explicits,inst(d)]},defs,action,null,tags).length>0);
      const r=await apply(before,action,[...ids,'Omen_of_the_Blessed']);
      if(!possible) atomic(before,r,[...ids,'Omen_of_the_Blessed'],base+' incomplete double branch');
      else {
        let intermediate=before;
        check(base+' '+action+' double'+ids.length+' count/consumption',r.applied&&r.events.length===2&&r.consumedOmens.length===ids.length&&same(r.remainingOmens,['Omen_of_the_Blessed']));
        for(const e of r.events) {
          weighted(intermediate,{...r,events:[e]},defs,action,null,tags,base+' '+action+' frozen double stage');
          intermediate={...intermediate,explicits:[...intermediate.explicits,{modifierId:e.modifierId,values:e.values,fractured:false}]};
        }
        if(action!=='EXALTED')check(base+' '+action+' unverified composition ledger',r.assumptions.some(a=>a.id==='tiered-omen-composition-v1'&&a.reason.includes('UNVERIFIED')));
      }
    }
  }
  for(const action of ['GREATER_EXALTED','PERFECT_EXALTED']) {
    const before={...root,rarity:'RARE',itemLevel:action.startsWith('GREATER')?34:49,explicits:[]};
    const ids=['Omen_of_Greater_Exaltation','Omen_of_Homogenising_Exaltation'];
    atomic(before,await apply(before,action,ids),ids,base+' upgraded double item-level floor');
  }
}
fs.writeFileSync('/evidence/api-results.json',JSON.stringify({checks,passed:checks.length,fixtures},null,2));
console.log(JSON.stringify({passed:checks.length,bases:Object.keys(fixtures)}));
})().catch(e=>{fs.writeFileSync('/evidence/api-resumption-failure.json',JSON.stringify({checks,lastCheck,lastExchange,error:String(e)},null,2));console.error(e);process.exit(1)});
