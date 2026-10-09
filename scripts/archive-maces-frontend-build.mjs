import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/maces-qa-20261005'
const destination=`${q}/frontend-build-attempt-2`
assert(!fs.existsSync(destination))
fs.mkdirSync(destination)
fs.cpSync(`${q}/frontend-check/dist`,`${destination}/dist`,{recursive:true})
fs.writeFileSync(`${destination}/scope.json`,JSON.stringify({checks:'frontend-check-2.log',browser:'browser-attempt-1',supersededBy:'Mace base-property not-computed disclosure; final Frontend/browser rerun',preserved:true},null,2)+'\n',{flag:'wx'})
