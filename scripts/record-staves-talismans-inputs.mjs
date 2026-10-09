import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
const q='E:/WORK/Exile-Hephaistos/codex/staves-talismans-qa-20261005'
const hash=b=>crypto.createHash('sha256').update(b).digest('hex')
const files=[]
function walk(path) {for(const e of fs.readdirSync(path,{withFileTypes:true})){const p=path+'/'+e.name;if(e.isDirectory())walk(p);else files.push(p)}}
for(const path of ['backend/src','frontend/src'])walk(path)
files.push('backend/build.gradle.kts','backend/settings.gradle.kts','backend/gradlew','backend/gradlew.bat','backend/gradle/wrapper/gradle-wrapper.jar','backend/gradle/wrapper/gradle-wrapper.properties','frontend/package.json','frontend/package-lock.json','frontend/vite.config.ts','frontend/tsconfig.json','frontend/eslint.config.js','frontend/.prettierrc.json','frontend/.prettierignore','frontend/.npmrc','frontend/index.html')
const proof=[]
for(const path of files) {
 if(!fs.existsSync(path))continue
 const tested=path.replace(/^(backend|frontend)/,(_,area)=>`${q}/${area}-check`)
 assert(fs.existsSync(tested),tested)
 const actual=fs.readFileSync(path),copy=fs.readFileSync(tested)
 assert.equal(hash(actual),hash(copy),path+' differs from final tested input')
 proof.push({path,sha256:hash(actual)})
}
assert.equal(hash(fs.readFileSync(`${q}/poe2craft.jar`)),hash(fs.readFileSync(`${q}/backend-check/build/libs/poe2craft.jar`)))
fs.writeFileSync('docs/evidence/staves-talismans-source-bundle-2026-10-05/final-qa-inputs.json',JSON.stringify({passed:true,sourceInputs:proof.length,runtimeJar:hash(fs.readFileSync(`${q}/poe2craft.jar`)),files:proof},null,2)+'\n',{flag:'wx'})
console.log(proof.length,'source inputs exactly match checked/built source;runtime jar matches build')
