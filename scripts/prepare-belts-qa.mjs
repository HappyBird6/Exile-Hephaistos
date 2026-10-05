import fs from 'node:fs'
import assert from 'node:assert/strict'
const q = 'E:/WORK/Exile-Hephaistos/codex/belts-qa-20261005', previous = 'E:/WORK/Exile-Hephaistos/codex/sceptres-qa-20261005'
assert(!fs.existsSync(q), 'Preserve previous QA output'); fs.mkdirSync(q)
for (const name of ['backend', 'frontend']) fs.cpSync(name, `${q}/${name}-check`, { recursive: true, filter: p => !p.split(/[\\/]/).some(s => ['node_modules', 'build', 'dist', '.gradle', 'coverage'].includes(s)) })
fs.mkdirSync(`${q}/scripts`)
fs.copyFileSync('scripts/check-display-coverage.mjs', `${q}/scripts/check-display-coverage.mjs`)
fs.cpSync(`${previous}/gradle-cache`, `${q}/gradle-cache`, { recursive: true })
fs.copyFileSync(`${previous}/api-attempt-2/api-initials.json`, `${q}/baseline-api-initials.json`)
fs.writeFileSync(`${q}/compose.yaml`, fs.readFileSync(`${previous}/compose.yaml`, 'utf8').replaceAll('exile-sceptres-20261005', 'exile-belts-20261005').replaceAll('20280', '20380').replaceAll('20281', '20381'))
for (const name of ['backend', 'frontend']) fs.copyFileSync(`${previous}/${name}-check-1.sh`, `${q}/${name}-check-1.sh`)
fs.writeFileSync(`${q}/record-live.mjs`, fs.readFileSync('scripts/record-wands-live.mjs', 'utf8').replaceAll('wands-qa-20261005', 'belts-qa-20261005'))
fs.writeFileSync(`${q}/heavy-qa-owner.json`, JSON.stringify({ active: true, project: 'exile-belts-20261005', ports: [20380, 20381], baseHead: 'e5d7c4e663049565de869e0aed826cfa714c8eb5', authorization: 'Sole writer; sequential exclusive QA' }, null, 2) + '\n')
console.log(q)
