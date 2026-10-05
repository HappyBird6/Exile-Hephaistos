import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/sceptres-qa-20261005'
let source=fs.readFileSync(`${q}/qa-browser-2.cjs`,'utf8')
source=source.replace("const terms = require('/source/frontend/src/shared/i18n/gameTerms.json')","const terms = require('/source/frontend/src/shared/i18n/gameTerms.json')\nconst messages = require('/source/frontend/src/shared/i18n/messages.json')")
const anchor="      let card = await page.locator('.bench-item-card').innerText()"
assert(source.includes(anchor))
source=source.replace(anchor,anchor+"\n      const scopes = await page.locator('.workbench-feedback').allTextContents()\n      check(base + ' ' + l + ' exact granted-skill scope', scopes.includes(messages[l]['notice.sceptre_skill_scope']))\n      check(base + ' ' + l + ' no inherited Skeletal Warrior scope', !scopes.includes(messages[l]['notice.sceptre_scope']))")
fs.writeFileSync(`${q}/qa-browser-3.cjs`,source,{flag:'wx'})
const output=`${q}/browser-attempt-2`
assert(!fs.existsSync(output),'Preserve prior captures')
fs.mkdirSync(output)
fs.copyFileSync(`${q}/api-attempt-2/api-initials.json`,`${output}/api-initials.json`)
fs.copyFileSync(`${q}/baseline-api-initials.json`,`${output}/baseline-api-initials.json`)
