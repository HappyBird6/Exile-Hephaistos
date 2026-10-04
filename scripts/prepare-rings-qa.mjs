import fs from 'node:fs'
import assert from 'node:assert/strict'
import {execFileSync} from 'node:child_process'
const root='E:/WORK/Exile-Hephaistos/codex/rings-qa-20261004'
assert(!fs.existsSync(root),'Never overwrite earlier QA output')
fs.mkdirSync(root);fs.mkdirSync(`${root}/scripts`)
fs.copyFileSync('scripts/check-display-coverage.mjs',`${root}/scripts/check-display-coverage.mjs`)
for(const [name,path] of Object.entries({registry:'backend/src/main/resources/crafting/registry-v2.json',gameTerms:'frontend/src/shared/i18n/gameTerms.json',modifierTemplates:'frontend/src/shared/i18n/modifierTemplates.json',topBases:'frontend/src/features/crafting/topBases.json'}))fs.writeFileSync(`${root}/baseline-${name}.json`,execFileSync('git',['show','HEAD:'+path],{maxBuffer:32*1024*1024}))
for(const name of ['backend','frontend'])fs.cpSync(name,`${root}/${name}-check`,{recursive:true,filter:p=>!p.split(/[\\/]/).some(s=>['node_modules','build','dist','.gradle','coverage'].includes(s))})
fs.writeFileSync(`${root}/compose.yaml`,fs.readFileSync('E:/WORK/Exile-Hephaistos/codex/bows-qa-20261004/compose.yaml','utf8').replaceAll('exile-bows-20261004','exile-rings-20261004').replaceAll('19780','19880').replaceAll('19781','19881'))
fs.copyFileSync('E:/WORK/Exile-Hephaistos/codex/bows-qa-20261004/api-initials.json',`${root}/baseline-api-initials.json`)
fs.writeFileSync(`${root}/backend-check-1.sh`,'#!/bin/sh\nset -eu\ncd /qa/backend-check\nsh gradlew --no-daemon spotlessApply check generateJooq bootJar > /qa/backend-check-1.log 2>&1\ncp build/libs/poe2craft.jar /qa/poe2craft.jar\n')
fs.writeFileSync(`${root}/frontend-check-1.sh`,'#!/bin/sh\nset -eu\nexport HEPHAISTOS_TRANSLATION_ROOT=/source\ncd /qa/frontend-check\nnpm ci > /qa/frontend-check-1.log 2>&1\nnpx prettier --write src >> /qa/frontend-check-1.log 2>&1\nnpm run lint >> /qa/frontend-check-1.log 2>&1\nnpm run typecheck >> /qa/frontend-check-1.log 2>&1\nnpm run format:check >> /qa/frontend-check-1.log 2>&1\nnpm run test -- --run >> /qa/frontend-check-1.log 2>&1\nnpm run build >> /qa/frontend-check-1.log 2>&1\n')
fs.cpSync('E:/WORK/Exile-Hephaistos/codex/bows-qa-20261004/gradle-cache',`${root}/gradle-cache`,{recursive:true})
fs.writeFileSync(`${root}/heavy-qa-owner.json`,JSON.stringify({active:true,owner:'workbench/top-bases-20261004',project:'exile-rings-20261004',ports:[19880,19881],baseHead:execFileSync('git',['rev-parse','HEAD'],{encoding:'utf8'}).trim(),authorization:'Continuous authorized work; sole writer; exclusive sequential heavy QA'},null,2)+'\n')
console.log(root)
