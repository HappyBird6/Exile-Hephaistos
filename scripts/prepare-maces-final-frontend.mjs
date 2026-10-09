import fs from 'node:fs'
const q='E:/WORK/Exile-Hephaistos/codex/maces-qa-20261005'
fs.copyFileSync('frontend/src/features/crafting/CraftingPage.tsx',`${q}/frontend-check/src/features/crafting/CraftingPage.tsx`)
fs.writeFileSync(`${q}/frontend-check-3.sh`,fs.readFileSync(`${q}/frontend-check-2.sh`,'utf8').replaceAll('frontend-check-2.log','frontend-check-3.log'),{flag:'wx'})
let browser=fs.readFileSync(`${q}/qa-browser-1.cjs`,'utf8')
browser=browser.replace("      for (const line of b.sourceProperties[l]) check(base+' '+l+' source weapon property',card.includes(line))",`      for (const line of b.sourceProperties[l]) {
        check(base+' '+l+' source weapon property',card.includes(line))
        const label=require('/source/frontend/src/shared/i18n/messages.json')[l]['base.weapon_property'].replace('{property}',line)
        check(base+' '+l+' uncomputed source property disclosure',card.includes(label))
      }`)
fs.writeFileSync(`${q}/qa-browser-2.cjs`,browser,{flag:'wx'})
fs.mkdirSync(`${q}/browser-attempt-2`)
for(const [from,to]of [['api-attempt-1/api-initials.json','api-initials.json'],['baseline-api-initials.json','baseline-api-initials.json']])fs.copyFileSync(`${q}/${from}`,`${q}/browser-attempt-2/${to}`)
