const fs=require('fs');
const root='E:/WORK/Exile-Hephaistos/codex/sapphire-generation-20261004/';
const file='backend/src/main/resources/crafting/registry-v2.json',registry=JSON.parse(fs.readFileSync(file));
const catalog=JSON.parse(fs.readFileSync('backend/src/main/resources/catalog/sapphire/catalog.json'));
for(const r of registry.entries){
 const liquid=catalog.modifiers.find(d=>d.tags.includes('crafted') && d.sourceUrl==='https://poe2db.tw/us/'+r.id);
 if(liquid){r.action=r.id.toUpperCase();r.effectStatus='IMPLEMENTED';r.serviceScope='ACTIVE';r.supportedBases=['sapphire'];r.solarStatus='NOT_APPLICABLE';r.ruleSource=liquid.sourceUrl;r.ruleVerifiedAt='2026-10-04';r.reason='Sapphire fixed Crafted result; one Crafted cap. Explicit uniform legal-removal simulator model; repeated Crafted replacement is unsupported.'}
 if(['TRANSMUTATION','AUGMENTATION','REGAL','EXALTED','CHAOS','ANNULMENT','ALCHEMY','DIVINE'].some(a=>r.action===a || r.action==='GREATER_'+a || r.action==='PERFECT_'+a)){
  r.supportedBases=[...new Set([...(r.supportedBases??[]),'sapphire'])];
 }
 if(r.category==='LIQUID_EMOTION'&&!liquid){r.serviceScope=r.id==='Liquid_Verisium'?'DEFERRED':'ACTIVE';r.reason=r.id==='Liquid_Verisium'?'This is unrelated to Basic/Time-Lost Jewel crafting.':'Registered Liquid inventory; Potent and Ancient mechanics remain unsupported. Registration is not implementation.'}
}
fs.writeFileSync(file,JSON.stringify(registry,null,2)+'\n');
const previous=fs.readFileSync('E:/WORK/Exile-Hephaistos/codex/sapphire-existing-20261004/compose.yaml','utf8');
fs.writeFileSync(root+'compose.yaml',previous.replaceAll('exile-sapphire-existing-20261004','exile-sapphire-generation-20261004'));
fs.writeFileSync(root+'backend-check.sh','#!/bin/sh\nset -eu\ncd /evidence/backend-check\nchmod +x gradlew\n./gradlew --no-daemon spotlessApply check generateJooq bootJar > /evidence/backend-check.log 2>&1\n');
fs.writeFileSync(root+'frontend-check.sh','#!/bin/sh\nset -eu\ncd /evidence/frontend-check\nnpm ci > /evidence/frontend-check.log 2>&1\nnpx prettier --write src/features/crafting/sapphireJewel.ts src/features/crafting/sapphireDefinitions.json src/features/crafting/workbenchApi.ts src/features/crafting/workbenchHistory.ts src/features/crafting/CraftingPage.tsx src/shared/test/sapphire-catalog.json src/shared/i18n/modifierTemplates.json src/shared/i18n/messages.json >> /evidence/frontend-check.log 2>&1\nnpm run lint >> /evidence/frontend-check.log 2>&1\nnpm run typecheck >> /evidence/frontend-check.log 2>&1\nnpm run format:check >> /evidence/frontend-check.log 2>&1\nnpm run test -- --run >> /evidence/frontend-check.log 2>&1\nnpm run build >> /evidence/frontend-check.log 2>&1\n');
fs.writeFileSync(root+'heavy-qa-owner.json',JSON.stringify({owner:'workbench/20261002',base:'3208ff40d583d72ab7c77eb1baea595d644fb9a9',started:new Date().toISOString(),scope:'Sapphire generation and Basic Liquid; sequential isolated checks'},null,2));
