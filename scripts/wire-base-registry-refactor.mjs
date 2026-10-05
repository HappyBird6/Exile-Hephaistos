import fs from 'node:fs'
import assert from 'node:assert/strict'
const edit=(p,f)=>{const before=fs.readFileSync(p,'utf8'),after=f(before);assert.notEqual(after,before,p);fs.writeFileSync(p,after)}
const item='backend/src/main/java/com/poe2craft/item/'
const mappings={ReviewedGloves:['gloves'],ReviewedHelmets:['helmets'],ReviewedBodies:['body'],ReviewedBoots:['boots'],ReviewedBows:['bows'],ReviewedRings:['rings'],ReviewedAmulets:['amulets'],ReviewedSceptres:['sceptres'],ReviewedWands:['wands'],ReviewedBelts:['belts'],ReviewedCrossbows:['crossbows'],ReviewedOffhands:['shields','bucklers','foci'],ReviewedQuivers:['quivers'],ReviewedMaces:['one-hand-maces','two-hand-maces'],ReviewedQuarterstavesSpears:['quarterstaves','spears']}
for(const [name,families]of Object.entries(mappings))edit(`${item}${name}.java`,s=>s.replace(/public static final Map<String, String> BASES =[^]*?;\n/,`public static final Map<String,String> BASES = BaseRegistry.familyBases(${families.map(f=>JSON.stringify(f)).join(',')});\n`))
edit(`${item}infrastructure/ItemCatalogLoader.java`,s=>{
 const a=s.indexOf('    String id;',s.indexOf('public static ItemCatalog loadTopBase')),z=s.indexOf('    String sourceDigest;',a)
 assert(a>0&&z>a)
 s=s.slice(0,a)+`    var reviewed = com.poe2craft.item.BaseRegistry.require(key);
    String id=reviewed.id(),name=reviewed.name();
    ItemCatalog pool;
    if(key.equals("soldier")) pool=loadBody();
    else if(key.equals("imperial")) pool=loadHelmet();
    else {
      String root="/catalog/"+reviewed.pool()+"/";
      try(var data=ItemCatalogLoader.class.getResourceAsStream(root+"catalog.json");
          var raw=ItemCatalogLoader.class.getResourceAsStream(root+"base.raw.json");
          var details=ItemCatalogLoader.class.getResourceAsStream(root+"details.raw.json")) {pool=load(data,raw,details);}
      catch(IOException e){throw new IllegalStateException("Cannot load reviewed base catalog",e);}
      if(!pool.base().id().equals(id)||!pool.base().name().equals(name))throw new IllegalArgumentException("Reviewed catalog identity mismatch");
    }
`+s.slice(z)
 const start=s.indexOf('    String sourceDigest;'),end=s.indexOf('    var old =',start)
 s=s.slice(0,start)+'    String sourceDigest = reviewed.sourceSha256();\n'+s.slice(end)
 s=s.replace(/\(\(com\.poe2craft\.item\.ReviewedOffhands\.supports\(id\)[^]*?\? "-20261005-"\s*: "-20261004-"\)/,'("-"+reviewed.policy().snapshotDate()+"-")')
 s=s.replace(/\(com\.poe2craft\.item\.ReviewedOffhands\.supports\(id\)[^]*?\? old\.retrievedAt\(\)\s*: "2026-10-04"/,'reviewed.policy().snapshotRetrievedAt().equals("SOURCE") ? old.retrievedAt() : reviewed.policy().snapshotRetrievedAt()')
 return s
})
edit('backend/src/main/java/com/poe2craft/bootstrap/CraftingConfiguration.java',s=>{
 const a=s.indexOf('      try (var source = getClass().getResourceAsStream("/catalog/top-base-essences.json"))'),z=s.indexOf('      return service;',a)
 return s.slice(0,a)+`      try(var source=getClass().getResourceAsStream("/catalog/top-base-essences.json")) {
       var manifest=new com.fasterxml.jackson.databind.ObjectMapper().readTree(source);
       for(var key:com.poe2craft.item.BaseRegistry.topBases().keySet()) {
        if(java.util.Set.of("soldier","imperial").contains(key))continue;
        var targets=manifest.get(key);
        if(targets==null)throw new IllegalArgumentException("Missing reviewed essence manifest");
        service.registerReviewedArmour(key,ItemCatalogLoader.loadTopBase(key),reviewedEssences(targets.get("fixed")),reviewedEssences(targets.get("replacements")));
       }
      }
`+s.slice(z)
})
edit('backend/src/main/java/com/poe2craft/crafting/application/WorkbenchService.java',s=>{
 s=s.replace(/\(ReviewedBows\.supports\(catalog\.base\(\)\.id\(\)\)[^]*?ReviewedQuarterstavesSpears\.supports\(catalog\.base\(\)\.id\(\)\)\)/,'(BaseRegistry.supports(catalog.base().id()) && BaseRegistry.policy(catalog.base().id()).initializeImplicit())')
 const a=s.indexOf('    if ((!ReviewedMaces.BASES'),z=s.indexOf('    var simulator =',a)
 assert(a>0&&z>a)
 return s.slice(0,a)+`    if(!BaseRegistry.require(key).id().equals(catalog.base().id()) || topBases.containsKey(key))throw new IllegalArgumentException("Unreviewed or duplicate base registration");
`+s.slice(z)
})
edit('backend/src/main/java/com/poe2craft/crafting/domain/QualityLimitRules.java',s=>{
 const a=s.indexOf('    if (!state.baseItemId().equals(SolarAmulet.BASE_ID)'),z=s.indexOf('    int maximum = 20;',a)
 assert(a>0&&z>a)
 s=s.slice(0,a)+'    if(!BaseRegistry.qualityLimit(state.baseItemId())) return null;\n'+s.slice(z)
 return s.replace(/\(!state\.baseItemId\(\)\.equals\(SolarAmulet\.BASE_ID\)[^]*?&& !ReviewedAmulets\.supports\(state\.baseItemId\(\)\)\)/,'!BaseRegistry.maximumQualityBreach(state.baseItemId())')
})
edit(`${item}CatalystQuality.java`,s=>s.replace(/return base\.equals\(SolarAmulet\.BASE_ID\)[^]*?\|\| BasicJewel\.supported\(base\);/,'return BaseRegistry.catalystQuality(base);'))
edit('backend/src/main/java/com/poe2craft/crafting/domain/AugmentSocketRules.java',s=>{
 s=s.replace('state.baseItemId().equals(STOCKY_BASE_ID)\n            && (state.augmentSockets() == 0 || state.augmentSockets() == 1)','com.poe2craft.item.BaseRegistry.socketExecutionMaximum(state.baseItemId()) != null\n && state.augmentSockets() >= 0 && state.augmentSockets() <= com.poe2craft.item.BaseRegistry.socketExecutionMaximum(state.baseItemId())')
 s=s.replace('!state.baseItemId().equals(STOCKY_BASE_ID)','com.poe2craft.item.BaseRegistry.socketExecutionMaximum(state.baseItemId()) == null')
 return s.replace('state.augmentSockets() >= 1','state.augmentSockets() >= com.poe2craft.item.BaseRegistry.socketExecutionMaximum(state.baseItemId())')
})
edit('backend/src/main/java/com/poe2craft/crafting/domain/WorkbenchSimulator.java',s=>{
 let a=s.indexOf('    if (ReviewedQuarterstavesSpears.supports',s.indexOf('private String baseRuleVersion()')),z=s.indexOf('    if (BasicJewel.supportedCrafting',a)
 assert(a>0&&z>a)
 s=s.slice(0,a)+'    if(BaseRegistry.supports(catalog.base().id())) return BaseRegistry.policy(catalog.base().id()).ruleVersion();\n'+s.slice(z)
 a=s.indexOf('    if (ReviewedQuarterstavesSpears.supports',s.indexOf('public String ledgerVersion()'));z=s.indexOf('    if (BasicJewel.supportedCrafting',a)
 s=s.slice(0,a)+'    if(BaseRegistry.supports(catalog.base().id())) return BaseRegistry.policy(catalog.base().id()).ledgerVersion();\n'+s.slice(z)
 s=s.replace('state.baseItemId().equals(AugmentSocketRules.STOCKY_BASE_ID)','BaseRegistry.socketExecutionMaximum(state.baseItemId()) != null')
 s=s.replace('if (!BasicJewel.supported(state.baseItemId()))','if (!BaseRegistry.refinedCatalyst(state.baseItemId()))')
 s=s.replace(/!state\.baseItemId\(\)\.equals\(SolarAmulet\.BASE_ID\)\s*&& !state\.baseItemId\(\)\.equals\(RingEssenceTargets\.BASE_ID\)\s*&& !ReviewedRings\.supports\(state\.baseItemId\(\)\)\s*&& !ReviewedAmulets\.supports\(state\.baseItemId\(\)\)/,'!BaseRegistry.ordinaryCatalyst(state.baseItemId())')
 return s
})
console.log('Backend registration and capability policy wired')
