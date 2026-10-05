import fs from 'node:fs'
import assert from 'node:assert/strict'
const q = 'E:/WORK/Exile-Hephaistos/codex/wands-qa-20261005'
const path = `${q}/qa-browser-${process.argv[2] ?? '1'}.cjs`
assert(!fs.existsSync(path),'Preserve prior output')
const helper = fs.readFileSync('E:/WORK/Exile-Hephaistos/codex/hallowed-qa-20261004/browser-helper.cjs','utf8').replaceAll("'62 base selector choices'","'71 base selector choices'").replaceAll('=== 62','=== 71')
fs.writeFileSync(path,helper+fs.readFileSync('scripts/qa-wands-browser-body.cjs','utf8'))
