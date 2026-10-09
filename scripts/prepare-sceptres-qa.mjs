import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/sceptres-qa-20261005',previous='E:/WORK/Exile-Hephaistos/codex/wands-qa-20261005'
assert(!fs.existsSync(q),'Preserve previous QA output')
fs.mkdirSync(q)
for(const name of ['backend','frontend']) fs.cpSync(name,`${q}/${name}-check`,{recursive:true,filter:p=>!p.split(/[\\/]/).some(s=>['node_modules','build','dist','.gradle','coverage'].includes(s))})
fs.mkdirSync(`${q}/scripts`)
fs.copyFileSync('scripts/check-display-coverage.mjs',`${q}/scripts/check-display-coverage.mjs`)
fs.cpSync(`${previous}/gradle-cache`,`${q}/gradle-cache`,{recursive:true})
fs.copyFileSync(`${previous}/api-attempt-1/api-initials.json`,`${q}/baseline-api-initials.json`)
fs.writeFileSync(`${q}/compose.yaml`,fs.readFileSync(`${previous}/compose.yaml`,'utf8').replaceAll('exile-wands-20261005','exile-sceptres-20261005').replaceAll('20180','20280').replaceAll('20181','20281'))
for(const name of ['backend','frontend']) fs.writeFileSync(`${q}/${name}-check-1.sh`,fs.readFileSync(`${previous}/${name}-check-2.sh`,'utf8').replaceAll(`${name}-check-2.log`,`${name}-check-1.log`))
fs.writeFileSync(`${q}/heavy-qa-owner.json`,JSON.stringify({active:true,project:'exile-sceptres-20261005',ports:[20280,20281],baseHead:'5ae15bfbeaa29a134a422f8a84013928e688064a',authorization:'Sole writer; sequential exclusive QA'},null,2)+'\n')
let api=fs.readFileSync('scripts/qa-wands-api.mjs','utf8').replaceAll('20180','20280').replace("b.family === 'wands'","b.family === 'sceptres' && key !== 'hallowed'").replace("const expected = ['bone','offering','primordial'].includes(key) ? 118 : ['volatile','galvanic'].includes(key) ? 123 : 185","const expected = 150").replaceAll('initials.wand.modifiers','initials.sceptre.modifiers').replaceAll('PERFECT_ESSENCE_SORCERY','PERFECT_ESSENCE_COMMAND').replace("['ESSENCE_COMMAND','PERFECT_ESSENCE_BODY'","['ESSENCE_SORCERY','PERFECT_ESSENCE_BODY'")
// The predicate receives a key through destructuring rather than a free variable.
api=api.replace("filter(([,b]) => b.family === 'sceptres' && key !== 'hallowed')","filter(([key,b]) => b.family === 'sceptres' && key !== 'hallowed')")
fs.writeFileSync(`${q}/qa-api.mjs`,api)
let body=fs.readFileSync('scripts/qa-wands-browser-body.cjs','utf8').replaceAll('20181','20281').replace("filter(([,b]) => b.family === 'wands')","filter(([key,b]) => b.family === 'sceptres' && key !== 'hallowed')").replaceAll("' Wand class'","' Sceptre class'").replaceAll("=== 'Wands'","=== 'Sceptres'").replaceAll('Greater_Essence_of_Sorcery','Greater_Essence_of_Command').replaceAll('initials.dueling','initials.wrath').replaceAll('dueling','wrath')
const helper=fs.readFileSync('E:/WORK/Exile-Hephaistos/codex/hallowed-qa-20261004/browser-helper.cjs','utf8').replaceAll("'62 base selector choices'","'78 base selector choices'").replaceAll('=== 62','=== 78').replace("await page.locator('#base-select').selectOption(base)","if (base.startsWith('shrine-')) check(base + ' selector skill disambiguation', (await page.locator('#base-select option[value=\"' + base + '\"]').innerText()).includes(bases[base].sourceProperties.en[1]))\n  await page.locator('#base-select').selectOption(base)")
fs.writeFileSync(`${q}/qa-browser-1.cjs`,helper+body)
console.log(q)
