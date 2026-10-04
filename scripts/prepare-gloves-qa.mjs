import fs from 'node:fs'
import assert from 'node:assert/strict'
const root = '/qa'
assert(!fs.existsSync(root + '/backend-check'), 'Preserve existing QA copies')
for (const name of ['backend', 'frontend']) fs.cpSync(`/source/${name}`, `${root}/${name}-check`, { recursive: true, filter: p => !p.split('/').some(s => ['node_modules', 'build', 'dist', '.gradle', 'coverage'].includes(s)) })
fs.mkdirSync(root + '/scripts', { recursive: true })
fs.copyFileSync('/source/scripts/check-display-coverage.mjs', root + '/scripts/check-display-coverage.mjs')
fs.writeFileSync(root + '/backend-check.sh', '#!/bin/sh\nset -eu\ncd /qa/backend-check\nsh gradlew --no-daemon spotlessApply check generateJooq bootJar > /qa/backend-check-1.log 2>&1\ncp build/libs/poe2craft.jar /qa/poe2craft.jar\n')
fs.writeFileSync(root + '/frontend-check.sh', '#!/bin/sh\nset -eu\ncd /qa/frontend-check\nnpm ci > /qa/frontend-check-1.log 2>&1\nnpx prettier --write src >> /qa/frontend-check-1.log 2>&1\nnpm run lint >> /qa/frontend-check-1.log 2>&1\nnpm run typecheck >> /qa/frontend-check-1.log 2>&1\nnpm run format:check >> /qa/frontend-check-1.log 2>&1\nnpm run test -- --run >> /qa/frontend-check-1.log 2>&1\nnpm run build >> /qa/frontend-check-1.log 2>&1\ncd /source\nnode scripts/check-display-coverage.mjs > /qa/display-coverage.json\n')
fs.writeFileSync(root + '/frontend-check.sh', fs.readFileSync(root + '/frontend-check.sh', 'utf8').replace('set -eu\n', 'set -eu\nexport HEPHAISTOS_TRANSLATION_ROOT=/source\n'))
const compose = fs.readFileSync('/prior/compose.yaml', 'utf8').replaceAll('exile-top-bases-20261004', 'exile-gloves-20261004').replaceAll('19280', '19380').replaceAll('19281', '19381')
fs.writeFileSync(root + '/compose.yaml', compose)
fs.writeFileSync(root + '/heavy-qa-owner.json', JSON.stringify({ owner: 'workbench/top-bases-20261004', active: true, project: 'exile-gloves-20261004', ports: [19380, 19381], authorization: 'User delegated actual Gloves implementation and assigned heavy QA slot', baseHead: '97d5a97169e9ee308e6b72cb15dcaf55f175a1f4' }, null, 2) + '\n')
