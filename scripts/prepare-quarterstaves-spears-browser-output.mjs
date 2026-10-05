import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/quarterstaves-spears-qa-20261005'
const attempt=process.argv[2]??'1'
assert(/^\d+$/.test(attempt))
const target=`${q}/browser-attempt-${attempt}`
assert(!fs.existsSync(target),'Preserve prior browser evidence')
fs.mkdirSync(target)
for(const [from,to]of [['api-attempt-1/api-initials.json','api-initials.json'],['baseline119-api-initials.json','baseline119-api-initials.json']])fs.copyFileSync(`${q}/${from}`,`${target}/${to}`,fs.constants.COPYFILE_EXCL)
console.log(target)
