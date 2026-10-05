import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
const q='E:/WORK/Exile-Hephaistos/codex/maces-qa-20261005'
const hash=b=>crypto.createHash('sha256').update(b).digest('hex')
const files=[]
function walk(path,destination=files) {
  for(const entry of fs.readdirSync(path,{withFileTypes:true})) {
    const p=`${path}/${entry.name}`
    if(entry.isDirectory())walk(p,destination)
    else destination.push(p)
  }
}
for(const dir of ['backend/src','frontend/src'])walk(dir)
files.push('backend/build.gradle.kts','backend/settings.gradle.kts','backend/gradlew','backend/gradlew.bat','backend/gradle/wrapper/gradle-wrapper.jar','backend/gradle/wrapper/gradle-wrapper.properties','frontend/package.json','frontend/package-lock.json','frontend/vite.config.ts','frontend/tsconfig.json','frontend/eslint.config.js','frontend/.prettierrc.json','frontend/.prettierignore','frontend/.npmrc','frontend/index.html')
const proofs=[]
for(const path of files) {
  if(!fs.existsSync(path))continue
  const from=path.replace(/^(backend|frontend)/,(_,area)=>`${q}/${area}-check`)
  assert(fs.existsSync(from),from)
  const source=fs.readFileSync(path),tested=fs.readFileSync(from)
  assert.equal(hash(source),hash(tested),path)
  proofs.push({path,sha256:hash(source)})
}
assert.equal(hash(fs.readFileSync(`${q}/poe2craft.jar`)),hash(fs.readFileSync(`${q}/backend-check/build/libs/poe2craft.jar`)))
const output=process.argv[2]??'final-qa-inputs.json'
assert(/^[a-z0-9-]+\.json$/.test(output))
const buildRoot=`${q}/frontend-check/dist`
const buildFiles=[]
walk(buildRoot,buildFiles)
fs.writeFileSync(`docs/evidence/maces-source-bundle-2026-10-05/${output}`,JSON.stringify({passed:true,sourceInputs:proofs.length,runtimeJar:hash(fs.readFileSync(`${q}/poe2craft.jar`)),frontendBuild:buildFiles.map(path=>({path:path.slice(buildRoot.length+1),sha256:hash(fs.readFileSync(path))})),files:proofs},null,2)+'\n',{flag:'wx'})
console.log(proofs.length,'exact tested source inputs match')
