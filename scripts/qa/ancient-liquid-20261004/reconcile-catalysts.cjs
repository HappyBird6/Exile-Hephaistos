const fs=require('fs'),assert=require('assert/strict');
const p='backend/src/main/resources/crafting/registry-v2.json',r=JSON.parse(fs.readFileSync(p));
const types=['FLESH','NEURAL','CARAPACE','UUL_NETOL','XOPH','TUL','ESH','CHAYULA','REAVER','SIBILANT','SKITTERING','ADAPTIVE','NECROTIC'];
const rows=r.entries.filter(e=>e.category==='CATALYST');assert.equal(rows.length,26);
for(const [i,e] of rows.entries()){
 const refined=e.id.startsWith('Refined_'),type=types[i%13];
 if(e.effectStatus!=='IMPLEMENTED')e.registrationHistory={effectStatus:e.effectStatus,solarStatus:e.solarStatus,action:e.action,reason:e.reason,sourceCheckpoint:'2026-09-23 display inventory',supersededBy:'2026-10-04 verified Catalyst application'};
 e.effectStatus='IMPLEMENTED';e.action=(refined?'REFINED_CATALYST_':'CATALYST_')+type;
 e.solarStatus=refined?'UNSUPPORTED_BASE':'VALIDATED';
 e.supportedBases=refined?['ruby','emerald','sapphire','diamond']:['solar','ring'];
 e.reason=refined?'Reviewed Basic Jewels only; Time-Lost source lacks jewel_catalyst. Ordinary Catalyst targets remain excluded.':'Reviewed Solar Amulet and Iron Ring only; other item classes and all Jewels remain excluded.';
 e.ruleSource=e.sourceUrl;e.ruleVerifiedAt='2026-10-04';
 e.verificationEvidence=['docs/evidence/workbench-quality-preserve-validation-2026-10-04.json','docs/evidence/workbench-ancient-liquid-validation-2026-10-04.json'];
 e.qualityPolicy='Preserve canonical rolls; replace Catalyst type; max(existing,current cap) provisional repeat/type-change policy; cap-loss preserves existing quality.';
}
const s=r.serviceScope,active=r.entries.filter(e=>e.serviceScope==='ACTIVE'),implemented=active.filter(e=>e.effectStatus==='IMPLEMENTED');
s.implementedActive=implemented.length;s.pending=active.length-implemented.length;s.pendingIds=active.filter(e=>e.effectStatus!=='IMPLEMENTED').map(e=>e.id);s.defaultImplemented=s.implementedActive-s.optInLegacyImplemented;s.catalystService='VERIFIED_RESTRICTED_BASE_APPLICATION';
r.catalystRegistryCheckpoint={date:'2026-10-04',ordinary:13,refined:13,ordinaryBases:['solar','ring'],refinedBases:['ruby','emerald','sapphire','diamond'],excluded:'Time-Lost Jewels and all other reviewed bases; no blanket item-class support',countsDefinition:'IMPLEMENTED means at least one verified positive execution path on supportedBases, not every base or established game odds.'};
fs.writeFileSync(p,JSON.stringify(r,null,2)+'\n');console.log(s);
