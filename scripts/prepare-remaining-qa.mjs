import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
const root=path.resolve('../i18n-remaining-qa-20261004')
assert(!fs.existsSync(root+'/frontend-check'),'Preserve existing QA output')
fs.cpSync('frontend',root+'/frontend-check',{recursive:true,filter:p=>!p.split(/[\\/]/).some(s=>['node_modules','dist','coverage'].includes(s))})
fs.mkdirSync(root+'/scripts',{recursive:true})
fs.copyFileSync('scripts/check-display-coverage.mjs',root+'/scripts/check-display-coverage.mjs')
fs.copyFileSync('../ui-feedback-qa-20261004/poe2craft.jar',root+'/poe2craft.jar')
fs.writeFileSync(root+'/compose.yaml',fs.readFileSync('../ui-feedback-qa-20261004/compose.yaml','utf8').replaceAll('exile-ui-feedback-20261004','exile-i18n-remaining-20261004').replaceAll('19080','19180').replaceAll('19081','19181'))
fs.writeFileSync(root+'/check.sh','#!/bin/sh\nset -eu\ncd /evidence/frontend-check\nnpm ci > /evidence/frontend-check-1.log 2>&1\nnpx prettier --write src > /evidence/format-1.log 2>&1\nnpm run lint >> /evidence/frontend-check-1.log 2>&1\nnpm run typecheck >> /evidence/frontend-check-1.log 2>&1\nnpm run format:check >> /evidence/frontend-check-1.log 2>&1\nnpm run test -- --run >> /evidence/frontend-check-1.log 2>&1\nnpm run build >> /evidence/frontend-check-1.log 2>&1\ncd /source\nnode scripts/check-display-coverage.mjs > /evidence/coverage-1.json\n')
fs.writeFileSync(root+'/heavy-qa-owner.json',JSON.stringify({owner:'workbench/i18n-remaining-20261004',active:true,baseHead:'18a2a3d4235e7ebf3e45c81f4fb6fe5f115c93b0',project:'exile-i18n-remaining-20261004',ports:[19180,19181],scope:'Display-only FE, source catalog coverage, browser sequential'},null,2)+'\n')
