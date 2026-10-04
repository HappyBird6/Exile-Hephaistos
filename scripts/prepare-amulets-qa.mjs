import fs from 'node:fs'
import assert from 'node:assert/strict'
import {execFileSync} from 'node:child_process'
const root='E:/WORK/Exile-Hephaistos/codex/amulets-qa-20261004'
assert(!fs.existsSync(root),'Never overwrite prior QA output');fs.mkdirSync(root);fs.mkdirSync(`${root}/scripts`)
fs.copyFileSync('scripts/check-display-coverage.mjs',`${root}/scripts/check-display-coverage.mjs`)
for(const [name,path] of Object.entries({registry:'backend/src/main/resources/crafting/registry-v2.json',gameTerms:'frontend/src/shared/i18n/gameTerms.json',modifierTemplates:'frontend/src/shared/i18n/modifierTemplates.json',topBases:'frontend/src/features/crafting/topBases.json'}))fs.writeFileSync(`${root}/baseline-${name}.json`,execFileSync('git',['show','HEAD:'+path],{maxBuffer:32*1024*1024}))
for(const name of ['backend','frontend'])fs.cpSync(name,`${root}/${name}-check`,{recursive:true,filter:p=>!p.split(/[\\/]/).some(s=>['node_modules','build','dist','.gradle','coverage'].includes(s))})
const previous='E:/WORK/Exile-Hephaistos/codex/rings-qa-20261004'
fs.writeFileSync(`${root}/compose.yaml`,fs.readFileSync(`${previous}/compose.yaml`,'utf8').replaceAll('exile-rings-20261004','exile-amulets-20261004').replaceAll('19880','19980').replaceAll('19881','19981'))
fs.copyFileSync(`${previous}/api-initials.json`,`${root}/baseline-api-initials.json`)
for(const name of ['backend','frontend'])fs.writeFileSync(`${root}/${name}-check-1.sh`,fs.readFileSync(`${previous}/${name}-check-1.sh`))
fs.cpSync(`${previous}/gradle-cache`,`${root}/gradle-cache`,{recursive:true})
fs.writeFileSync(`${root}/heavy-qa-owner.json`,JSON.stringify({active:true,owner:'workbench/top-bases-20261004',project:'exile-amulets-20261004',ports:[19980,19981],baseHead:execFileSync('git',['rev-parse','HEAD'],{encoding:'utf8'}).trim(),authorization:'Sole writer; sequential exclusive QA'},null,2)+'\n')
console.log(root)
