import fs from 'node:fs'
import assert from 'node:assert/strict'
const root = 'E:/WORK/Exile-Hephaistos/codex/hallowed-qa-20261004'
const previous = 'E:/WORK/Exile-Hephaistos/codex/amulets-qa-20261004'
assert(!fs.existsSync(root), 'Never overwrite prior QA output')
fs.mkdirSync(root)
for (const name of ['backend', 'frontend']) fs.cpSync(name, `${root}/${name}-check`, { recursive: true, filter: p => !p.split(/[\\/]/).some(s => ['node_modules', 'build', 'dist', '.gradle', 'coverage'].includes(s)) })
fs.mkdirSync(`${root}/scripts`)
fs.copyFileSync('scripts/check-display-coverage.mjs', `${root}/scripts/check-display-coverage.mjs`)
fs.cpSync(`${previous}/gradle-cache`, `${root}/gradle-cache`, { recursive: true })
fs.copyFileSync(`${previous}/api-initials.json`, `${root}/baseline-api-initials.json`)
fs.writeFileSync(`${root}/compose.yaml`, fs.readFileSync(`${previous}/compose.yaml`, 'utf8').replaceAll('exile-amulets-20261004', 'exile-hallowed-20261004').replaceAll('19980', '20080').replaceAll('19981', '20081'))
for (const name of ['backend', 'frontend']) fs.copyFileSync(`${previous}/${name}-check-1.sh`, `${root}/${name}-check-1.sh`)
fs.writeFileSync(`${root}/heavy-qa-owner.json`, JSON.stringify({ active: true, owner: 'workbench/top-bases-20261004', project: 'exile-hallowed-20261004', ports: [20080, 20081], baseHead: '927275d3a531223d68471f3e1dbae7fa0dbd9108', authorization: 'Sole writer; sequential exclusive QA' }, null, 2) + '\n')
console.log(root)
