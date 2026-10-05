import fs from 'node:fs'
import assert from 'node:assert/strict'
const q = 'E:/WORK/Exile-Hephaistos/codex/belts-qa-20261005'
const out = `${q}/browser-attempt-1`
assert(!fs.existsSync(out))
fs.mkdirSync(out)
fs.copyFileSync(`${q}/api-attempt-3/api-initials.json`, `${out}/api-initials.json`)
fs.copyFileSync(`${q}/baseline-api-initials.json`, `${out}/baseline-api-initials.json`)
