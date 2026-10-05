import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/staves-talismans-qa-20261005'
const previous='E:/WORK/Exile-Hephaistos/codex/base-registry-qa-20261005'
assert(!fs.existsSync(q),'Preserve existing outputs')
assert.equal(JSON.parse(fs.readFileSync(`${previous}/heavy-qa-owner.json`)).active,false)
fs.mkdirSync(q)
fs.cpSync(`${previous}/gradle-cache`,`${q}/gradle-cache`,{recursive:true})
fs.writeFileSync(`${q}/compose.yaml`,fs.readFileSync(`${previous}/compose.yaml`,'utf8').replaceAll('exile-base-registry-20261005','exile-staves-talismans-20261005').replaceAll('20980','21080').replaceAll('20981','21081'),{flag:'wx'})
fs.writeFileSync(`${q}/heavy-qa-owner.json`,JSON.stringify({active:true,project:'exile-staves-talismans-20261005',ports:[21080,21081],baseHead:'5287663f383d5199b43ff963fc51c55d70859270',authorization:'Sole writer; isolated sequential QA'},null,2)+'\n',{flag:'wx'})
fs.copyFileSync(`${previous}/baseline125-api-initials.json`,`${q}/baseline125-api-initials.json`,fs.constants.COPYFILE_EXCL)
fs.writeFileSync(`${q}/record-live.mjs`,fs.readFileSync(`${previous}/record-live.mjs`,'utf8').replaceAll('base-registry-qa-20261005','staves-talismans-qa-20261005'),{flag:'wx'})
fs.mkdirSync(`${q}/scripts`)
fs.copyFileSync('scripts/check-display-coverage.mjs',`${q}/scripts/check-display-coverage.mjs`,fs.constants.COPYFILE_EXCL)
for(const [from,to]of [['backend','backend-check'],['frontend','frontend-check']])fs.cpSync(from,`${q}/${to}`,{recursive:true,filter:p=>!p.split(/[\\/]/).some(s=>['node_modules','dist','build','.gradle'].includes(s))})
fs.writeFileSync(`${q}/backend-check-1.sh`,'#!/bin/sh\nset -eu\ncd /qa/backend-check\nsh gradlew --no-daemon spotlessApply check generateJooq bootJar > /qa/backend-check-1.log 2>&1\ncp build/libs/poe2craft.jar /qa/poe2craft.jar\n',{flag:'wx'})
fs.writeFileSync(`${q}/frontend-check-1.sh`,'#!/bin/sh\nset -eu\nexport HEPHAISTOS_TRANSLATION_ROOT=/source\ncd /qa/frontend-check\nnpm ci > /qa/frontend-check-1.log 2>&1\nnpx prettier --write src >> /qa/frontend-check-1.log 2>&1\nnpm run lint >> /qa/frontend-check-1.log 2>&1\nnpm run typecheck >> /qa/frontend-check-1.log 2>&1\nnpm run format:check >> /qa/frontend-check-1.log 2>&1\nnpm run test -- --run >> /qa/frontend-check-1.log 2>&1\nnpm run build >> /qa/frontend-check-1.log 2>&1\n',{flag:'wx'})
console.log(q)
