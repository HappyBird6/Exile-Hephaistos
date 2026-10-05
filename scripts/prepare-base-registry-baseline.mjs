import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/base-registry-qa-20261005'
assert(fs.existsSync(`${q}/baseline-backend`))
const keys=Object.keys(JSON.parse(fs.readFileSync(`${q}/baseline125-api-initials.json`)))
assert.equal(keys.length,125)
const body=`package com.poe2craft.bootstrap;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import java.util.*;
import java.nio.file.*;
import java.security.*;
import org.junit.jupiter.api.Test;
class BaseRegistryBaselineTest {
 static final ObjectMapper M=new ObjectMapper();
 static JsonNode canonical(JsonNode n) {
  if(n.isObject()) {var result=M.createObjectNode();var keys=new TreeSet<String>();n.fieldNames().forEachRemaining(keys::add);for(var k:keys)result.set(k,canonical(n.get(k)));return result;}
  if(n.isArray()) {var values=new ArrayList<JsonNode>();n.forEach(v->values.add(canonical(v)));if(values.stream().allMatch(JsonNode::isTextual))values.sort(Comparator.comparing(JsonNode::asText));var result=M.createArrayNode();values.forEach(result::add);return result;}
  return n;
 }
 static String digest(Object value)throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(M.writeValueAsBytes(canonical(M.valueToTree(value)))));}
 @Test void capture() throws Exception {
 var cfg=new CraftingConfiguration();var catalog=cfg.itemCatalog();var engine=cfg.craftingEngine(catalog);var service=cfg.workbenchService(catalog,cfg.workbenchSimulator(catalog,engine));
 var result=new TreeMap<String,Object>();
 for(var key:List.of(${keys.map(k=>JSON.stringify(k)).join(',')})) {
 var record=new TreeMap<String,Object>();
 for(int level:new int[]{1,20,82}) {
 var initial=service.initial(key,level);record.put("initial-"+level,digest(initial));
 var s=initial.state();var state=new ItemState(s.snapshotId(),s.baseItemId(),s.itemLevel(),s.rarity(),s.implicits(),s.modifierIds().stream().map(id->new ModifierInstance(id,initial.modifiers().get(id).stats().stream().collect(java.util.stream.Collectors.toMap(ModifierDefinition.StatRange::id,stat->stat.max())))).toList(),s.conditions(),initial.augmentSockets());
 record.put("actions-"+level,digest(service.actions(state,Set.of())));
 var trace=new ArrayList<Object>();var random=new Random(760105L);
 for(var action:List.of(WorkbenchCurrency.TRANSMUTATION,WorkbenchCurrency.AUGMENTATION,WorkbenchCurrency.REGAL,WorkbenchCurrency.EXALTED,WorkbenchCurrency.EXALTED,WorkbenchCurrency.DIVINE,WorkbenchCurrency.CHAOS,WorkbenchCurrency.ANNULMENT)) {
 var applied=service.apply(state,action,Set.of(),random);trace.add(applied);state=applied.state();
 }
 record.put("seeded-"+level,digest(trace));
 }
 result.put(key,record);
 }
 Files.writeString(Path.of("/qa/baseline125-runtime-digests.json"),M.writerWithDefaultPrettyPrinter().writeValueAsString(result));
 }
}
`
const path=`${q}/baseline-backend/src/test/java/com/poe2craft/bootstrap/BaseRegistryBaselineTest.java`
fs.mkdirSync(path.slice(0,path.lastIndexOf('/')),{recursive:true})
fs.writeFileSync(path,body,{flag:'wx'})
fs.writeFileSync(`${q}/baseline-check.sh`,'#!/bin/sh\nset -eu\ncd /qa/baseline-backend\nsh gradlew --no-daemon test --tests com.poe2craft.bootstrap.BaseRegistryBaselineTest > /qa/baseline-check.log 2>&1\n',{flag:'wx'})
console.log(keys.length)
