import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/staves-talismans-qa-20261005',output=`${q}/browser-attempt-1`
assert(!fs.existsSync(output),'Preserve previous browser outputs')
fs.mkdirSync(output)
fs.copyFileSync(`${q}/api-attempt-2/api-initials.json`,`${output}/api-initials.json`,fs.constants.COPYFILE_EXCL)
fs.copyFileSync(`${q}/baseline125-api-initials.json`,`${output}/baseline125-api-initials.json`,fs.constants.COPYFILE_EXCL)
for(const name of ['qa-crossclass-filled-2.cjs','qa-negative-jewel-2.cjs'])fs.writeFileSync(`${q}/${name}`,fs.readFileSync(`${q}/${name}`,'utf8').replaceAll('125 base selector choices','134 base selector choices').replaceAll('=== 125','=== 134'))
console.log(output)
