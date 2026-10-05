import fs from 'node:fs'
import assert from 'node:assert/strict'
const q = 'E:/WORK/Exile-Hephaistos/codex/belts-qa-20261005'
fs.copyFileSync(`${q}/reviewed-belts-contract.json`, 'frontend/src/shared/test/reviewed-belts-contract.json')
for (const file of ['src/shared/test/reviewed-belts-contract.json', 'src/features/crafting/ReviewedBeltsContract.test.ts', 'src/features/crafting/workbenchApi.ts']) fs.copyFileSync(`frontend/${file}`, `${q}/frontend-check/${file}`)
let script = fs.readFileSync(`${q}/frontend-check-2.sh`, 'utf8').replaceAll('\r', '').replaceAll('frontend-check-2.log', 'frontend-check-4.log')
fs.writeFileSync(`${q}/frontend-check-4.sh`, script.trimEnd() + '\n', { flag: 'wx' })
const out = `${q}/browser-attempt-2`
assert(!fs.existsSync(out)); fs.mkdirSync(out)
for (const [from, to] of [['api-attempt-3/api-initials.json', 'api-initials.json'], ['baseline-api-initials.json', 'baseline-api-initials.json']]) fs.copyFileSync(`${q}/${from}`, `${out}/${to}`)
