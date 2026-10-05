import fs from 'node:fs'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
const q = 'E:/WORK/Exile-Hephaistos/codex/belts-qa-20261005'
let report = '', status = 0
try { report = execFileSync('git', ['diff', '--cached', '--check'], { encoding: 'utf8', maxBuffer: 32 * 1024 * 1024 }) }
catch (error) { report = error.stdout; status = error.status }
const warnings = report.split('\n').filter(line => /: (?:trailing whitespace|new blank line at EOF)\./.test(line))
assert(warnings.length > 0 && warnings.every(line => /^docs\/evidence\/belts-source-bundle-2026-10-05\/.*\.html:\d+:/.test(line)), 'All whitespace findings must belong to preserved raw source HTML')
fs.writeFileSync(`${q}/staged-source-whitespace.log`, report, { flag: 'wx' })
fs.writeFileSync('docs/evidence/belts-runtime-bundle-2026-10-05/staged-review.json', JSON.stringify({ authoredCodeWhitespaceCheckPassed: true, fullDiffCheckStatus: status, rawSourceWhitespaceWarnings: warnings.length, rawSourceBytesPreserved: true, reportPath: `${q}/staged-source-whitespace.log`, gitOwnershipNote: 'Escalated shell used another Windows account and Git refused ownership; normal owning sandbox account staged this authorized branch without global configuration changes' }, null, 2) + '\n', { flag: 'wx' })
console.log(`${warnings.length} raw source HTML whitespace findings preserved; authored paths pass`)
