import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/base-registry-qa-20261005'
const baseline=JSON.parse(fs.readFileSync(`${q}/baseline125-runtime-digests.json`))
assert.equal(Object.keys(baseline).length,125)
fs.mkdirSync('backend/src/test/resources',{recursive:true})
fs.copyFileSync(`${q}/baseline125-runtime-digests.json`,'backend/src/test/resources/base-registry-runtime-baseline.json',fs.constants.COPYFILE_EXCL)
fs.copyFileSync(`${q}/baseline125-runtime-digests.json`,'docs/evidence/base-registry-refactor-2026-10-05/runtime-baseline.json',fs.constants.COPYFILE_EXCL)
let test=fs.readFileSync(`${q}/baseline-backend/src/test/java/com/poe2craft/bootstrap/BaseRegistryBaselineTest.java`,'utf8')
test=test.replace('class BaseRegistryBaselineTest','class BaseRegistryParityTest').replace('void capture()','void allExistingInitialsActionsAndSeededTracesRemainExact()')
test=test.replace(' var result=new TreeMap<String,Object>();',' var result=new TreeMap<String,Object>();\n var expected=M.readTree(getClass().getResourceAsStream("/base-registry-runtime-baseline.json"));\n org.junit.jupiter.api.Assertions.assertEquals(125,expected.size());')
test=test.replace(/for\(var key:List\.of\([^]*?\)\) \{/,'for(var it=expected.fieldNames();it.hasNext();) { var key=it.next();')
test=test.replace(' result.put(key,record);',' result.put(key,record);\n org.junit.jupiter.api.Assertions.assertEquals(expected.get(key),M.valueToTree(record),key+": pre-refactor behavior changed");')
test=test.replace(' Files.writeString(Path.of("/qa/baseline125-runtime-digests.json"),M.writerWithDefaultPrettyPrinter().writeValueAsString(result));',' org.junit.jupiter.api.Assertions.assertEquals(expected,M.valueToTree(result));')
test=test.replace('import java.nio.file.*;\n','')
fs.writeFileSync('backend/src/test/java/com/poe2craft/bootstrap/BaseRegistryParityTest.java',test,{flag:'wx'})
for(const [from,to]of [['backend','backend-check'],['frontend','frontend-check']]) {
 assert(!fs.existsSync(`${q}/${to}`),'Preserve previous QA inputs')
 fs.cpSync(from,`${q}/${to}`,{recursive:true,filter:p=>!p.split(/[\\/]/).some(s=>['node_modules','dist','build','.gradle'].includes(s))})
}
fs.mkdirSync(`${q}/scripts`)
fs.copyFileSync('scripts/check-display-coverage.mjs',`${q}/scripts/check-display-coverage.mjs`,fs.constants.COPYFILE_EXCL)
fs.writeFileSync(`${q}/backend-check-1.sh`,'#!/bin/sh\nset -eu\ncd /qa/backend-check\nsh gradlew --no-daemon spotlessApply check generateJooq bootJar > /qa/backend-check-1.log 2>&1\ncp build/libs/poe2craft.jar /qa/poe2craft.jar\n',{flag:'wx'})
fs.writeFileSync(`${q}/frontend-check-1.sh`,'#!/bin/sh\nset -eu\nexport HEPHAISTOS_TRANSLATION_ROOT=/source\ncd /qa/frontend-check\nnpm ci > /qa/frontend-check-1.log 2>&1\nnpx prettier --write src >> /qa/frontend-check-1.log 2>&1\nnpm run lint >> /qa/frontend-check-1.log 2>&1\nnpm run typecheck >> /qa/frontend-check-1.log 2>&1\nnpm run format:check >> /qa/frontend-check-1.log 2>&1\nnpm run test -- --run >> /qa/frontend-check-1.log 2>&1\nnpm run build >> /qa/frontend-check-1.log 2>&1\n',{flag:'wx'})
console.log('125-base immutable runtime characterization fixture and isolated copies prepared')
