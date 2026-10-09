import fs from 'node:fs'
import assert from 'node:assert/strict'
const policy='backend/src/main/resources/catalog/base-policies.json'
const data=JSON.parse(fs.readFileSync(policy,'utf8'))
data.baseOverrides.soldier.legacyCatalog='BODY'
data.baseOverrides.imperial={legacyCatalog:'HELMET'}
for(const p of [policy,'frontend/src/features/crafting/basePolicies.json'])fs.writeFileSync(p,JSON.stringify(data,null,2)+'\n')
const edit=(p,f)=>{const before=fs.readFileSync(p,'utf8'),after=f(before);assert.notEqual(before,after,p);fs.writeFileSync(p,after)}
edit('backend/src/main/java/com/poe2craft/item/BaseRegistry.java',s=>{
 s=s.replace('String snapshotDate, String snapshotRetrievedAt) {}','String snapshotDate, String snapshotRetrievedAt, String legacyCatalog) {}')
 const a=s.indexOf('      if (rules.path("schemaVersion")'),z=s.indexOf('\n    } catch (IOException e)',a)
 const logic=s.slice(a,z).replace('      TOP = Collections.unmodifiableMap(top);\n      POLICIES = Collections.unmodifiableMap(byId);','      return new Snapshot(Collections.unmodifiableMap(top),Collections.unmodifiableMap(byId));')
 s=s.slice(0,a)+'      var snapshot = read(source,rules);\n      TOP=snapshot.top();POLICIES=snapshot.policies();'+s.slice(z)
 s=s.replace('  private BaseRegistry() {}',`  record Snapshot(Map<String,Base> top,Map<String,Policy> policies) {}
  static Snapshot read(JsonNode source,JsonNode rules) {
    var mapper=new ObjectMapper();
${logic}
  }
  private BaseRegistry() {}`)
 s=s.replace('        var policy = parse(merged, mapper);','        for(String field:List.of("itemClass","ruleVersion","ledgerVersion","snapshotDate","snapshotRetrievedAt"))required(merged,field);\n        for(String field:List.of("initializeImplicit","sourcePropertyUnscaled"))if(!merged.path(field).isBoolean())throw new IllegalArgumentException("Missing class policy: "+field);\n        var policy = parse(merged, mapper);')
 s=s.replace('try { return mapper.readerFor(Policy.class)', 'if(!source.has("socketExecutionMaximum") || (!source.get("socketExecutionMaximum").isNull() && (!source.get("socketExecutionMaximum").isInt() || source.get("socketExecutionMaximum").asInt()!=1)))throw new IllegalArgumentException("Only the reviewed one-socket execution policy is supported");\n    try { return mapper.readerFor(Policy.class)')
 return s.replace('!Set.of("soldier","imperial").contains(key)','base.policy().legacyCatalog()==null')
})
edit('backend/src/main/java/com/poe2craft/item/infrastructure/ItemCatalogLoader.java',s=>s.replace('if(key.equals("soldier")) pool=loadBody();\n    else if(key.equals("imperial")) pool=loadHelmet();','if("BODY".equals(reviewed.policy().legacyCatalog())) pool=loadBody();\n    else if("HELMET".equals(reviewed.policy().legacyCatalog())) pool=loadHelmet();'))
edit('backend/src/main/java/com/poe2craft/bootstrap/CraftingConfiguration.java',s=>s.replace('if(java.util.Set.of("soldier","imperial").contains(key))continue;','if(com.poe2craft.item.BaseRegistry.require(key).policy().legacyCatalog()!=null)continue;'))
// Clean the already consolidated unions, including their last redundant literal.
for(const p of ['frontend/src/features/crafting/draft.ts','frontend/src/features/crafting/craftingApi.ts','frontend/src/features/crafting/CraftingPage.tsx'])edit(p,s=>s.replace(/\s*\| WorkbenchBaseKey[ \t]*(?:\r?\n[ \t]*\| '[a-z0-9-]+')?/g,' WorkbenchBaseKey'))
