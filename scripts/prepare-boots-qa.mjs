import fs from 'node:fs'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
const root = 'E:/WORK/Exile-Hephaistos/codex/boots-qa-20261004'
const prior = 'E:/WORK/Exile-Hephaistos/codex/body-helmets-qa-20261004'
assert(!fs.existsSync(root + '/backend-check'), 'Preserve earlier QA output')
for (const [name, path] of Object.entries({registry:'backend/src/main/resources/crafting/registry-v2.json', gameTerms:'frontend/src/shared/i18n/gameTerms.json', modifierTemplates:'frontend/src/shared/i18n/modifierTemplates.json', topBases:'frontend/src/features/crafting/topBases.json'})) if(!fs.existsSync(`${root}/baseline-${name}.json`)) fs.writeFileSync(`${root}/baseline-${name}.json`,execFileSync('git',['show','HEAD:'+path],{maxBuffer:32*1024*1024}))
for (const name of ['backend','frontend']) fs.cpSync(name,`${root}/${name}-check`,{recursive:true,filter:p=>!p.split(/[\\/]/).some(s=>['node_modules','build','dist','.gradle','coverage'].includes(s))})
fs.mkdirSync(`${root}/scripts`,{recursive:true})
fs.copyFileSync('scripts/check-display-coverage.mjs',`${root}/scripts/check-display-coverage.mjs`)
fs.writeFileSync(`${root}/compose.yaml`,fs.readFileSync(`${prior}/compose.yaml`,'utf8').replaceAll('exile-body-helmets-20261004','exile-boots-20261004').replaceAll('19580','19680').replaceAll('19581','19681'))
fs.copyFileSync(`${prior}/api-initials.json`,`${root}/baseline-api-initials.json`)
fs.writeFileSync(`${root}/backend-check.sh`,'#!/bin/sh\nset -eu\ncd /qa/backend-check\nsh gradlew --no-daemon spotlessApply check generateJooq bootJar > /qa/backend-check-1.log 2>&1\ncp build/libs/poe2craft.jar /qa/poe2craft.jar\n')
fs.writeFileSync(`${root}/frontend-check.sh`,'#!/bin/sh\nset -eu\nexport HEPHAISTOS_TRANSLATION_ROOT=/source\ncd /qa/frontend-check\nnpm ci > /qa/frontend-check-1.log 2>&1\nnpx prettier --write src >> /qa/frontend-check-1.log 2>&1\nnpm run lint >> /qa/frontend-check-1.log 2>&1\nnpm run typecheck >> /qa/frontend-check-1.log 2>&1\nnpm run format:check >> /qa/frontend-check-1.log 2>&1\nnpm run test -- --run >> /qa/frontend-check-1.log 2>&1\nnpm run build >> /qa/frontend-check-1.log 2>&1\ncd /source\nnode scripts/check-display-coverage.mjs > /qa/display-coverage.json\n')
fs.writeFileSync(`${root}/heavy-qa-owner.json`,JSON.stringify({active:true,owner:'workbench/top-bases-20261004',project:'exile-boots-20261004',ports:[19680,19681],baseHead:'e8034894cce832d0a7397d0265cc1b1d68ee2628',authorization:'Explicit sole writer and exclusive heavy QA'},null,2)+'\n')
const keys="['tasalian', 'drakeskin', 'sekhema', 'blacksteel-boots', 'faithful', 'daggerfoot']"
let s=fs.readFileSync('scripts/qa-gloves-api.mjs','utf8').replaceAll("['massive', 'sirenscale', 'adherent']",keys).replaceAll('19380','19680').replaceAll("'gloves-uniform-candidates-v1'","'boots-uniform-candidates-v1'").replace("['PERFECT_ESSENCE_GROUNDING', 'PERFECT_ESSENCE_OPULENCE', 'ESSENCE_HYSTERIA', 'ESSENCE_ABYSS']","Object.keys(overrides[key].replacements)")
s=s.replace("['PERFECT_ESSENCE_ICE', 'PERFECT_ESSENCE_BODY', 'ARTIFICER', 'ESSENCE_HORROR', 'RUNIC_ALLOY', 'CATALYST_FLESH']","['PERFECT_ESSENCE_ICE', 'PERFECT_ESSENCE_BODY', 'PERFECT_ESSENCE_GROUNDING', 'PERFECT_ESSENCE_OPULENCE', 'PERFECT_ESSENCE_HASTE', 'ARTIFICER', 'ESSENCE_HORROR', 'RUNIC_ALLOY', 'CATALYST_FLESH']")
fs.writeFileSync(`${root}/qa-api.mjs`,s)
s=fs.readFileSync(`${prior}/qa-browser-final.cjs`,'utf8').replaceAll("['slipstrike', 'death-mail', 'sleek', 'vile', 'wolfskin', 'ancestral', 'cryptic']",keys).replaceAll('19581','19681').replaceAll('35 base selector choices','41 base selector choices').replaceAll('count() === 35','count() === 41').replaceAll("'slipstrike'","'tasalian'")
s=s.replace("const result = await use(bases[k].family === 'body' ? 'Perfect_Essence_of_the_Body' : 'Perfect_Essence_of_Thawing')","const result = await use('Essence_of_Hysteria')")
s=s.replace("    const state = { ...concrete(source), rarity: 'RARE', explicits: [{ modifierId: d.id, values: Object.fromEntries(d.stats.map(s => [s.id, s.min])), fractured: true }] }", "    const speed = Object.values(source.modifiers).find(d => d.familyIds.includes('MovementVelocity') && d.requiredItemLevel === 82)\n    const state = { ...concrete(source), rarity: 'RARE', explicits: [{ modifierId: d.id, values: Object.fromEntries(d.stats.map(s => [s.id, s.min])), fractured: true }, { modifierId: speed.id, values: Object.fromEntries(speed.stats.map(s => [s.id, s.min])), fractured: false }] }")
s=s.replace('class-valid Perfect Essence','source-valid Boots Hysteria')
s=s.replace("'gladiatorial', 'grinning'])", "'gladiatorial', 'grinning', 'slipstrike', 'death-mail', 'sleek', 'vile', 'wolfskin', 'ancestral', 'cryptic'])")
s=s.replace("const initials = JSON.parse(fs.readFileSync('/evidence/api-initials.json'))", "const initials = JSON.parse(fs.readFileSync('/evidence/api-initials.json'))\nconst oldInitials = JSON.parse(fs.readFileSync('/evidence/baseline-api-initials.json'))")
s=s.replace('concrete(initials[legacy])','concrete(oldInitials[legacy])')
s=s.replace("const locales = ['en', 'ko', 'zh-CN', 'zh-TW', 'ja', 'es']", "const locales = ['en', 'ko', 'zh-CN', 'zh-TW', 'ja', 'es']\nfs.mkdirSync('/evidence/cards', { recursive: true })")
s=s.replace("await page.screenshot({ path: `/evidence/${k}-${l}-${width}.png`, fullPage: true })", "await page.screenshot({ path: `/evidence/${k}-${l}-${width}.png`, fullPage: true })\n        await page.locator('.bench-item-card').screenshot({ path: `/evidence/cards/${k}-${l}-${width}.png` })")
fs.writeFileSync(`${root}/qa-browser.cjs`,s)
console.log('Prepared isolated Boots QA at '+root)
