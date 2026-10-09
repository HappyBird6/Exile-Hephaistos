import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
const root=path.resolve('../i18n-remaining-qa-20261004/coverage-negative')
if(fs.existsSync(root))throw Error('Owned output already exists')
for(const dir of ['backend/src/main/resources/catalog','backend/src/main/resources/crafting','frontend/src/shared/i18n'])fs.cpSync(dir,path.join(root,dir),{recursive:true})
const file=path.join(root,'frontend/src/shared/i18n/gameTerms.json'),original=fs.readFileSync(file,'utf8'),terms=JSON.parse(original)
delete terms.ko.Liquid_Paranoia
fs.writeFileSync(file,JSON.stringify(terms))
const result=spawnSync(process.execPath,['scripts/check-display-coverage.mjs'],{encoding:'utf8',env:{...process.env,HEPHAISTOS_TRANSLATION_ROOT:root}})
if(result.status===0)throw Error('Missing active translation incorrectly passed')
fs.writeFileSync(path.join(root,'expected-failure.log'),result.stderr)
fs.writeFileSync(file,original)
console.log('PASS: missing active Korean translation rejected')
