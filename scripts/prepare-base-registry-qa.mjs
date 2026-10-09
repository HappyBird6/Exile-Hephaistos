import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/base-registry-qa-20261005'
const previous='E:/WORK/Exile-Hephaistos/codex/quarterstaves-spears-qa-20261005'
assert(!fs.existsSync(q),'Preserve existing QA outputs')
assert.equal(JSON.parse(fs.readFileSync(`${previous}/heavy-qa-owner.json`)).active,false)
fs.mkdirSync(q)
fs.cpSync(`${previous}/gradle-cache`,`${q}/gradle-cache`,{recursive:true})
fs.cpSync('backend',`${q}/baseline-backend`,{recursive:true,filter:p=>!p.split(/[\\/]/).some(s=>['build','.gradle'].includes(s))})
fs.copyFileSync(`${previous}/poe2craft.jar`,`${q}/baseline.jar`,fs.constants.COPYFILE_EXCL)
fs.copyFileSync(`${previous}/api-attempt-1/api-initials.json`,`${q}/baseline125-api-initials.json`,fs.constants.COPYFILE_EXCL)
fs.writeFileSync(`${q}/compose.yaml`,fs.readFileSync(`${previous}/compose.yaml`,'utf8').replaceAll('exile-quarterstaves-spears-20261005','exile-base-registry-20261005').replaceAll('20880','20980').replaceAll('20881','20981'),{flag:'wx'})
fs.writeFileSync(`${q}/heavy-qa-owner.json`,JSON.stringify({active:true,project:'exile-base-registry-20261005',ports:[20980,20981],baseHead:'e80d63fca33b9702b192cf23970b03fb8891eed5',authorization:'Sole writer; isolated sequential QA'},null,2)+'\n',{flag:'wx'})
fs.writeFileSync(`${q}/record-live.mjs`,fs.readFileSync(`${previous}/record-live.mjs`,'utf8').replaceAll('quarterstaves-spears-qa-20261005','base-registry-qa-20261005'),{flag:'wx'})
for(const name of ['qa-api-1.mjs','qa-browser-1.cjs','qa-old-filled-1.cjs','contact-sheets.cjs'])fs.copyFileSync(`${previous}/${name}`,`${q}/${name}`,fs.constants.COPYFILE_EXCL)
fs.copyFileSync(`${previous}/baseline119-api-initials.json`,`${q}/baseline119-api-initials.json`,fs.constants.COPYFILE_EXCL)
console.log(q)
