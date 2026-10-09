import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/sceptres-qa-20261005'
const output=`${q}/qa-browser-2.cjs`
assert(!fs.existsSync(output),'Preserve previous QA script')
const prior=fs.readFileSync(`${q}/qa-browser-1.cjs`,'utf8')
const next=prior.replace("await page.locator('#base-select').selectOption(base)","if (base.startsWith('shrine-')) check(base + ' selector skill disambiguation', (await page.locator('#base-select option[value=\"' + base + '\"]').innerText()).includes(bases[base].sourceProperties.en[1]))\n  await page.locator('#base-select').selectOption(base)")
assert.notEqual(next,prior)
fs.writeFileSync(output,next,{flag:'wx'})
const helper=fs.readFileSync('E:/WORK/Exile-Hephaistos/codex/hallowed-qa-20261004/browser-helper.cjs','utf8')
const legacy=helper+fs.readFileSync('scripts/qa-wands-old-filled.cjs','utf8').replaceAll('20181','20281')
fs.writeFileSync(`${q}/qa-old-filled.cjs`,legacy,{flag:'wx'})
