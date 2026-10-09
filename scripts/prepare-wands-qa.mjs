import fs from 'node:fs'
import assert from 'node:assert/strict'
const root = 'E:/WORK/Exile-Hephaistos/codex/wands-qa-20261005'
const previous = 'E:/WORK/Exile-Hephaistos/codex/hallowed-qa-20261004'
assert(!fs.existsSync(root), 'Never overwrite prior QA output')
fs.mkdirSync(root)
for (const name of ['backend', 'frontend']) fs.cpSync(name, `${root}/${name}-check`, { recursive: true, filter: p => !p.split(/[\\/]/).some(s => ['node_modules', 'build', 'dist', '.gradle', 'coverage'].includes(s)) })
fs.mkdirSync(`${root}/scripts`)
fs.copyFileSync('scripts/check-display-coverage.mjs', `${root}/scripts/check-display-coverage.mjs`)
fs.cpSync(`${previous}/gradle-cache`, `${root}/gradle-cache`, { recursive: true })
fs.copyFileSync(`${previous}/api-after-quality-fix/api-initials.json`, `${root}/baseline-api-initials.json`)
fs.writeFileSync(`${root}/compose.yaml`, fs.readFileSync(`${previous}/compose.yaml`, 'utf8').replaceAll('exile-hallowed-20261004', 'exile-wands-20261005').replaceAll('20080', '20180').replaceAll('20081', '20181'))
for (const name of ['backend', 'frontend']) fs.copyFileSync(`${previous}/${name}-check-1.sh`, `${root}/${name}-check-1.sh`)
fs.writeFileSync(`${root}/heavy-qa-owner.json`, JSON.stringify({ active: true, owner: 'workbench/top-bases-20261004', project: 'exile-wands-20261005', ports: [20180, 20181], baseHead: '25131b190fd242f90b51051bc87ae1e65936ae29', authorization: 'Sole writer; sequential exclusive QA' }, null, 2) + '\n')
console.log(root)
