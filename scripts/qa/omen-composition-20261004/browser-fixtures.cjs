const fs=require('fs'),assert=require('assert/strict');
const api='http://host.docker.internal:18280/api/v1/crafting/workbench';
const fixtures={};
const inst=d=>({modifierId:d.id,values:Object.fromEntries(d.stats.map(s=>[s.id,s.min])),fractured:false});
const overlap=(a,b)=>a.familyIds.some(f=>b.familyIds.includes(f));
(async()=>{
 for(const base of ['solar','stocky','bow','wand','body','sceptre','belt','helmet','ring']) {
  const r=await fetch(api+'/initial?base='+base+'&itemLevel=82');assert.equal(r.status,200);
  const initial=await r.json(),defs=initial.modifiers,root={...initial.state,explicits:[],augmentSockets:initial.augmentSockets??null,catalystQuality:null};
  delete root.modifierIds;
  const ordinary=Object.values(defs).filter(d=>d.layer==='EXPLICIT'&&d.weight>0&&d.requiredItemLevel<=82);
  const s=ordinary.filter(d=>d.affixType==='SUFFIX').sort((a,b)=>a.requiredItemLevel-b.requiredItemLevel)[0];assert(s);
  const p=ordinary.find(d=>d.affixType==='PREFIX'&&d.requiredItemLevel>s.requiredItemLevel&&!overlap(d,s));assert(p);
  const p2=ordinary.find(d=>d.affixType==='PREFIX'&&d.requiredItemLevel===p.requiredItemLevel&&!overlap(d,p)&&!overlap(d,s));
  const rare={...root,rarity:'RARE',explicits:[inst(p),...(p2?[inst(p2)]:[]),inst(s)]};
  fixtures[base]={initial,rare,candidates:rare.explicits.filter(m=>defs[m.modifierId].affixType==='PREFIX').map(m=>m.modifierId)};
 }
 fs.writeFileSync('/evidence/browser-fixtures.json',JSON.stringify({source:'Read-only initial catalogs; synthetic browser fixtures, not API action validation results',fixtures},null,2));
 console.log(JSON.stringify({readOnlyCatalogs:Object.keys(fixtures)}));
})().catch(e=>{console.error(e);process.exit(1)});
