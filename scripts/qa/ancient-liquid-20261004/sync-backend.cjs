const fs=require('fs');
const root='../ancient-liquid-20261004/';
for(const f of ['src/main/java/com/poe2craft/crafting/domain/WorkbenchSimulator.java','src/main/java/com/poe2craft/crafting/application/WorkbenchService.java'])fs.copyFileSync(root+'backend-check/'+f,'backend/'+f);
for(const f of ['src/test/java/com/poe2craft/crafting/AncientLiquidTest.java','src/test/java/com/poe2craft/crafting/SapphireGenerationLiquidTest.java','src/main/resources/crafting/registry-v2.json'])fs.copyFileSync('backend/'+f,root+'backend-check/'+f);
fs.writeFileSync(root+'backend-check-4.sh',fs.readFileSync(root+'backend-check.sh','utf8').replace('backend-check-1.log','backend-check-4.log'));
const s=fs.readFileSync(root+'backend-check/build/test-results/test/TEST-com.poe2craft.crafting.AncientLiquidTest.xml','utf8');
console.log([...s.matchAll(/<failure message="([^"]*)/g)].map(m=>m[1]).join('\n'));
